package com.platformnexus.retry.kafka;

import com.platformnexus.retry.PersistentRetry;
import com.platformnexus.retry.Serializer;
import com.platformnexus.retry.config.RetryConfig;
import com.platformnexus.retry.exception.FatalRetryException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

class KafkaRetryMessageListener<T extends Serializable>
    implements AcknowledgingMessageListener<String, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRetryMessageListener.class);
  private final PersistentRetry<T> persistentRetry;
  private final RetryConfig retryConfig;
  private final Serializer serializer;
  private final KafkaPublisher kafkaPublisher;

  KafkaRetryMessageListener(PersistentRetry<T> persistentRetry, RetryConfig retryConfig,
                            KafkaPublisher kafkaPublisher) {
    this.persistentRetry = persistentRetry;
    this.retryConfig = retryConfig;
    this.kafkaPublisher = kafkaPublisher;
    serializer = persistentRetry.getSerializer();
  }

  @Override
  public void onMessage(@NotNull ConsumerRecord<String, String> consumerRecord, Acknowledgment ack) throws Error {
    try {
      processMessage(consumerRecord, ack);
    } catch (Exception e) {
      /*
        We must catch any Exception thrown from {@link #processMessage(ConsumerRecord, Acknowledgment)} to prevent our
        Kafka queues from blocking. Any error thrown from processMessage is an unrecoverable error.
       */
      LOGGER.error("Consumer Record [key={} , topic={} ] failed!", consumerRecord.key(), consumerRecord.topic(), e);
      sendToDeadLetterQueue(consumerRecord, ack);
    }
  }

  private void processMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment ack) throws IOException {
    RecordMetadata metaData = RecordMetadata.fromConsumerRecord(consumerRecord);
    T dataObject = getData(consumerRecord);
    sleepUntilThresholdMet(consumerRecord, metaData);

    try {
      persistentRetry.handleRetry(dataObject);
      ack.acknowledge();
    } catch (FatalRetryException e) {
      LOGGER.warn("Consumer Record [key={} , topic={} ] failed!", consumerRecord.key(), consumerRecord.topic());
      sendToDeadLetterQueue(consumerRecord, dataObject, ack);
    } catch (Exception e) {
      // todo: Throttle the message listener and enforce retry count
      republishMessage(consumerRecord, ack, metaData);
    }
  }

  private void republishMessage(@NotNull ConsumerRecord<String, String> consumerRecord, Acknowledgment ack, RecordMetadata metaData) {
    LOGGER.debug("Republishing message {key={} topic={} }", consumerRecord.key(), consumerRecord.topic());
    kafkaPublisher.publish(consumerRecord.value(), consumerRecord.topic(), metaData.incrementRetryCount());
    ack.acknowledge();
  }

  private void sendToDeadLetterQueue(ConsumerRecord<String, String> consumerRecord, Acknowledgment ack) {
    sendToDeadLetterQueue(consumerRecord, null, ack);
  }

  private void sendToDeadLetterQueue(ConsumerRecord<String, String> consumerRecord, T data, Acknowledgment ack) {
    if (data != null) {
      persistentRetry.onFatalRetry(data);
    }
    // todo: ack if and only if dead letter queue publishing sends success
    ack.acknowledge();
  }

  private void sleepUntilThresholdMet(ConsumerRecord<String, String> consumerRecord, RecordMetadata metaData) {
    RetryConfig.Interval interval = retryConfig.getTypeByName(metaData.getRetryType()).getInterval();
    long messageAge = Instant.now().toEpochMilli() - metaData.getPublishTime();
    if (messageAge < interval.getMillis()) {
      // Message retry can wait until desired interval condition is met
      try {
        Thread.sleep(Math.max(messageAge - interval.getMillis(), 0));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.warn("Thread.sleep was interrupted for record with Topic={} Partition={} Offset={} ThreadId={} Metadata={}",
            consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset(), Thread.currentThread().getId(),
            metaData);
      }
    }
  }

  private T getData(ConsumerRecord<String, String> consumerRecord) throws IOException {
    try {
      return (T) serializer.deserialize(consumerRecord.value(), persistentRetry.getDataClass());

    } catch (IOException e) {
      LOGGER.error(
          "Failed to deserialize value for record with Topic={} Partition={} Offset={} ThreadId={}", consumerRecord.topic(),
          consumerRecord.partition(), consumerRecord.offset(),
          Thread.currentThread().getId());
      throw e;
    }
  }
}
