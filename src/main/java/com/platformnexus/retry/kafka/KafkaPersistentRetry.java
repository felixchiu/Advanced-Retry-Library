package com.platformnexus.retry.kafka;

import com.platformnexus.retry.PersistentRetry;
import com.platformnexus.retry.RetryBroker;
import com.platformnexus.retry.config.RetryConfig;
import com.platformnexus.retry.kafka.config.KafkaConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class KafkaPersistentRetry implements RetryBroker {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPersistentRetry.class);
  private static final int INITIAL_ATTEMPTS_COUNT = 0;
  private ConcurrentMessageListenerContainer<String, String> messageListenerContainer;
  private PersistentRetry retryService;

  @Autowired
  private KafkaPublisher publisher;

  @Autowired
  private KafkaConsumerConfig kafkaConsumerConfig;

  @Autowired
  private RetryConfig retryConfig;

  @Override
  public void start() {
    KafkaRetryMessageListener listener =
        new KafkaRetryMessageListener<>(retryService, retryConfig, publisher);

    String retryServiceName = retryService.getName();
    messageListenerContainer = kafkaConsumerConfig.getMessageListenerContainer(
        retryConfig.getTypeByName(retryServiceName).getTopic());

    messageListenerContainer.setupMessageListener(listener);

    messageListenerContainer.start();
  }

  @Override
  public void register(PersistentRetry tPersistentRetry) {
    this.retryService = tPersistentRetry;
  }

  @Override
  public void schedule(Object o) throws IOException {
    String data = serialize(o);
    String type = retryService.getName();
    RecordMetadata metadata = new RecordMetadata(type, INITIAL_ATTEMPTS_COUNT, Instant.now().toEpochMilli());
    RetryConfig.RetryType retryType = retryConfig.getTypeByName(type);
    // todo: Add Precondition
    String topic = retryType.getTopic();
    publisher.publish(data, topic, metadata);
  }

  private String serialize(Object o) throws IOException {
    return retryService.getSerializer().serialize(o);
  }

  @Override
  public void shutdown() throws InterruptedException {
    messageListenerContainer.stop();
  }
}
