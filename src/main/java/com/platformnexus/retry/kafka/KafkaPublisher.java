package com.platformnexus.retry.kafka;

public interface KafkaPublisher {

  void publish(String value, String topicName, RecordMetadata metadata);
}
