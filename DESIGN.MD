<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents** 

- [Current Design Proposal](#current-design-proposal)
- [Motivations](#motivations)
- [Design Considerations](#design-considerations)
- [Discussion](#discussion)
- [References](#references)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


### Current Design Proposal
Implement a retry queue with Kafka using a single topic for all retry-able event types and partition on failure event type. 

Consider the following client call:

```
// retryService publishes the correct eventType to the correct partition and is async

try {
    dependency.call(request);
} catch (Exception e) {
    // as a developer, you will have more context on what should or shouldn't be re-tried.
    retryService.schedule(request);
}


/*
    Perhaps create an annotation that works like hystrix-javanica's @HystrixCommand. This would make the annotated method the callback itself.
    Eric has actually written something before that lets you inject values from application context into an annotation's configuration.
    See https://github.com/Netflix/Hystrix/issues/1768
*/
@PersistentRetryable(topic = "${dependency.topic.name}")
public void callDependency(Request request) {
    dependency.call(request);
}
```

Consumers listen to the relevant partitions of the retry topic based on a partitioning expression. 
The retry policy can be configured per event type.

For example, within the topic "card.transaction.failures", we might have the following partitions:

Partition #: `getPartitionIndex("API1-external-fault")`

Events in the partition:

    { "eventType": "externalFault", "reason": "api1-429" } (too many requests)
    { "eventType": "externalFault", "reason": "api1-5xx" } (server errors)
    { "eventType": "externalFault", "reason": "api1-serverTimeout"} (connect or read)


Partition #: `getPartitionIndex("API1-internal-fault")`

Events in the partition:

    { "eventType": "internalFault", "reason": : "api1-clientTimeout"} (connect or read)

Note: Neal is working on a partition expression that will let us map event types to specific Kafka partitions.

## Motivations
If an event in the queue is failing, we attempt to retry on the current record according to current back-off policy:

- On success: Acknowledge the message and decrease the back-off if any, then proceed to next message 
adhering to back-off policy.

- On failure, Acknowledge the message and increase back-off according to policy
  - If dead letter queue criteria is met (for example, max retries met), send to dead letter queue (DLQ).
  - Else re-publish the attempt to the topic with an incremented retry count to schedule it for later.
    
    Re-publishing prevents one record from blocking all others in case the records have different failure
    reasons. The library has no way of knowing that the records in a partition will fail for the same reasons,
    even if it is likely. This depends on how careful the user has been about defining the types of events
    that go into the partition, so it's best not to assume.
    
    ** Question for Neal: When does the actual re-publish of a record occur? Is the actual publish delayed?
    Or is the message published immediately and on re-entry the consumer says skip or no skip based on the
    record's backoff policy?

## Design Considerations
1) Why use Kafka for the retry queue?

    Refer back to the purpose: primarily because we need a persistent retry:
    
    Use cases such as legal requirements that require a commit log of required actions.
    - Fault tolerance. Design for resilience in case processes fail and there needs to be a way to recover from an attempted action where rollback is not an option.
    - Kafka provides persistence, fault tolerance, and ordered partitions. The library also wants to avoid reinventing the wheel. The spring-kafka integration consumer uses a blocking queue under the hood.

    Suggestion:
    ZooKeeper and Kafka add DEVOPS maintenance overhead and additional system dependencies which require 
    more extensive component and integration testing. A Kafka persistence implementation will be provided
    out of the box but the user should be allowed to implement their own persistence strategy that plugs
    into the retry framework.

2) What is the motivation for using one topic with a partition mapped to a specific type?

    Primarily to manage overhead of configuring a lot of topics. There's DEVOPS overhead in creating the 
    topics and managing the number of partitions and replication factors for each topic.
    
    With the approach of one topic and multiple partitions, the getPartitionIndex() for producers and consumers will need to handle the concept of partition buckets. For example, if we wanted to assign more partitions to process events of "failureA", we need to figure out how to load balance between the original partition for failureA events and the newly added partitions.
    
    `TODO: Neal may rewrite or revise.`

3) How long do we set our retention window to? Where do we back up old retry attempts?

    Currently undecided. DEVOPS says we don't currently have a strategy that backs up Kafka topics (1).
    
    There are a few options:
    - Use log compaction or set the retention to forever. [Kafka Docs - Log Compaction](https://kafka.apache.org/documentation/#compaction), [Confluent - Log Compaction](https://www.confluent.io/blog/okay-store-data-apache-kafka/) 
    
      > Treating Kafka as a primary store does mean raising the bar for how you run it, though. Storage systems rightly have a huge burden in terms of correctness, uptime, and data integrity...if you're running Kafka for this kind of use case, you need to make sure you know how to operate it well, and you need to know the limitation the system has.
        
    - Have another dedicated consumer that writes all the retry attempt events to relational database or document store for auditing or reporting purposes
    Physically backup the topics from the Kafka brokers
    
## Discussion
**Can we be smarter about retrying? How can we use something like Hystrix?**

Only if we can be sure that the next records will have the same failure reasons as the current one that 
is failing.

When a dependency emits a large number of failures, we can flip a circuit breaker (e.g. Hystrix).
The circuit breaker prevents us from retrying subsequent attempts that will likely fail. 
Instead of re-publishing these records back to the retry topic, we can send circuit-open failures somewhere 
else that schedules retries only when the circuit closes. This reduces noise in the topic, and we can log the 
number of retries of the current record somewhere else (logger, another kafka topic, etc).

We only acknowledge a record until it succeeds or exhausts a back-off policy.

So perhaps before doing a persistent retry, what we might want to do is:

1) Auto-wrap all client call methods with @HystrixCommand. Perhaps @PersistentRetryable will actually 
also apply @HystrixCommand to the annotated method.

2) @PersistentRetryable uses a composite backoff policy: a) if circuit closed, send to original retry 
topic, b) if circuit open, send to circuit open queue that blocks on records until the circuit closes again.

Then when circuit breaker flips due to large number of failures:

Records that fail due to OPEN_CIRCUIT are sent to a separate retry topic: circuit.open.retry, instead 
of the main retry queue for infrequent failures.

Consumers of this topic DO block on the current record using a RetryTemplate with exponential back-off 
policy until the circuit becomes closed. I think this reflects traditional usages of Kafka consumers 
more closely. If the circuit never closes within a specified time, then it is escalated to the DLQ.

## References
"Do we backup our kafka topics?" Chiu, Felix. Lync. 17 May 2018 2:40 PM PST.
