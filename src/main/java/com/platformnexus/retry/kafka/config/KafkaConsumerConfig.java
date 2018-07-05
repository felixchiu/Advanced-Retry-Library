package com.platformnexus.retry.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Import(KafkaConfig.class)
public class KafkaConsumerConfig {

  @Autowired
  private KafkaConfig kafkaConfig;

  public ConcurrentMessageListenerContainer<String, String> getMessageListenerContainer(
      String topic) {

    ContainerProperties containerProperties = new ContainerProperties(topic);
    containerProperties.setAckMode(AbstractMessageListenerContainer.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setAckOnError(false);
    // TODO: set error handler
    //containerProperties.setErrorHandler();

    ConcurrentMessageListenerContainer<String, String> messageListenerContainer =
        new ConcurrentMessageListenerContainer<>(consumerFactory(), containerProperties);
    messageListenerContainer.setConcurrency(kafkaConfig.getConsumer().getConcurrencyLimit());

    return messageListenerContainer;
  }

  @Bean
  public ConsumerFactory<String, String> consumerFactory() {
    return new DefaultKafkaConsumerFactory<>(consumerConfigs());
  }

  @Bean
  public Map<String, Object> consumerConfigs() {
    // See https://kafka.apache.org/documentation/#consumerconfigs for more properties
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootStrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getConsumer().getGroupId());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConfig.getConsumer().isEnableAutoCommit());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConfig.getConsumer().getAutoOffsetReset());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return props;
  }

}
