package com.platformnexus.retry.kafka.config;

import com.gs.cft.retry.RetryBroker;
import com.gs.cft.retry.config.RetryConfig;
import com.gs.cft.retry.kafka.KafkaPersistentRetry;
import com.gs.cft.retry.kafka.KafkaPublisher;
import com.gs.cft.retry.kafka.KafkaPublisherImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@Import({RetryConfig.class, KafkaConfig.class, KafkaConsumerConfig.class, KafkaProducerConfig.class})
public class BrokerConfig {

  @Bean
  @Scope("prototype")
  public RetryBroker retryBroker() {
    return new KafkaPersistentRetry();
  }

  @Bean
  public KafkaPublisher kafkaPublisher(KafkaConfig kafkaConfig,  KafkaTemplate<String, String> kafkaTemplate) {
    return new KafkaPublisherImpl(kafkaConfig, kafkaTemplate);
  }
}
