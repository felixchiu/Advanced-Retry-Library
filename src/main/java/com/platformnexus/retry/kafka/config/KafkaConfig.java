package com.platformnexus.retry.kafka.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Validated
@Configuration
@ConfigurationProperties("platformnexus.retry.kafka")
public class KafkaConfig {
  @Value("${platformnexus.retry.kafka.bootstrap-servers}")
  private String bootStrapServers;
  @NotNull
  private Consumer consumer;
  @NotNull
  private Producer producer;

  public String getBootStrapServers() {
    return bootStrapServers;
  }

  public void setBootStrapServers(String bootStrapServers) {
    this.bootStrapServers = bootStrapServers;
  }

  public Consumer getConsumer() {
    return consumer;
  }

  public void setConsumer(Consumer consumer) {
    this.consumer = consumer;
  }

  public Producer getProducer() {
    return producer;
  }

  public void setProducer(Producer producer) {
    this.producer = producer;
  }

  public static class Consumer {

    @NotNull
    @Value("${platformnexus.retry.kafka.consumer.concurrency-limit}")
    private Integer concurrencyLimit;

    @Value("${platformnexus.retry.kafka.consumer.retry-interval}")
    private long retryInterval;

    @Value("${platformnexus.retry.kafka.consumer.retry-count-limit}")
    private int retryCountLimit;

    @Value("${platformnexus.retry.kafka.consumer.polling-interval}")
    private long pollingInterval;

    @Value("${platformnexus.retry.kafka.consumer.enable-auto-commit}")
    private boolean enableAutoCommit;

    @Value("${platformnexus.retry.kafka.consumer.group-id}")
    private String groupId;

    @Value("${platformnexus.retry.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    public Integer getConcurrencyLimit() {
      return concurrencyLimit;
    }

    public void setConcurrencyLimit(Integer concurrencyLimit) {
      this.concurrencyLimit = concurrencyLimit;
    }

    public long getRetryInterval() {
      return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
      this.retryInterval = retryInterval;
    }

    public int getRetryCountLimit() {
      return retryCountLimit;
    }

    public void setRetryCountLimit(int retryCountLimit) {
      this.retryCountLimit = retryCountLimit;
    }

    public long getPollingInterval() {
      return pollingInterval;
    }

    public void setPollingInterval(long pollingInterval) {
      this.pollingInterval = pollingInterval;
    }

    public boolean isEnableAutoCommit() {
      return enableAutoCommit;
    }

    public void setEnableAutoCommit(boolean enableAutoCommit) {
      this.enableAutoCommit = enableAutoCommit;
    }

    public String getGroupId() {
      return groupId;
    }

    public void setGroupId(String groupId) {
      this.groupId = groupId;
    }

    public String getAutoOffsetReset() {
      return autoOffsetReset;
    }

    public void setAutoOffsetReset(String autoOffsetReset) {
      this.autoOffsetReset = autoOffsetReset;
    }
  }

  public static class Producer {

    @Value("${platformnexus.retry.kafka.producer.time-out-milli-seconds}")
    private Integer timeOutMilliSeconds;

    @Value("${platformnexus.retry.kafka.producer.retries}")
    private Integer retries;

    public Integer getTimeOutMilliSeconds() {
      return timeOutMilliSeconds;
    }

    public void setTimeOutMilliSeconds(Integer timeOutMilliSeconds) {
      this.timeOutMilliSeconds = timeOutMilliSeconds;
    }

    public Integer getRetries() {
      return retries;
    }

    public void setRetries(Integer retries) {
      this.retries = retries;
    }
  }
}
