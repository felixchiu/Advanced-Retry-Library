package com.platformnexus.retry.kafka;

import com.platformnexus.retry.exception.ScheduleRetryException;
import com.platformnexus.retry.kafka.config.KafkaConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaPublisherImpl implements KafkaPublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPublisherImpl.class);

  private final KafkaConfig kafkaConfig;
  private final KafkaTemplate<String, String> kafkaTemplate;


  public KafkaPublisherImpl(KafkaConfig kafkaConfig, KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaConfig = kafkaConfig;
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public void publish(String value, String topicName, RecordMetadata metadata) {
    doPublish(topicName, value, metadata);
  }

  private void doPublish(String topic, String messageBody, RecordMetadata metadata) {
    String id = UUID.randomUUID().toString();
    final ProducerRecord<String, String> record = new ProducerRecord<>(topic, id, messageBody);

    RecordHeaders headers = metadata.toRecordHeaders();
    headers.forEach(header -> record.headers().add(header));

    try {
      LOGGER.debug("Sending message [id={} ] on topic={}", id, topic);
      kafkaTemplate.send(record).get(kafkaConfig.getProducer().getTimeOutMilliSeconds(), TimeUnit.SECONDS);
    } catch (Exception e) {
      String message = String.format("Could not publish a message id=%s on topic=%s", id, topic);
      throw new ScheduleRetryException(message, e);
    }
  }
}
