package com.platformnexus.retry.kafka;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.nio.charset.StandardCharsets;

public class RecordMetadata {

  static final String RETRY_TYPE_KEY = "retryType";
  static final String RETRY_COUNT_KEY = "retryCount";
  static final String PUBLISH_TIME_KEY = "publishTime";

  private final String retryType;
  private final long publishTime;
  private int retryCount;

  RecordMetadata(String retryType, int retryCount, long publishTime) {
    this.retryType = retryType;
    this.retryCount = retryCount;
    this.publishTime = publishTime;
  }

  public String getRetryType() {
    return retryType;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public RecordMetadata incrementRetryCount(){
    this.retryCount = retryCount + 1;
    return this;
  }

  public long getPublishTime() {
    return publishTime;
  }

  static RecordMetadata fromConsumerRecord(ConsumerRecord<String, String> consumerRecord) {
    Headers headers = consumerRecord.headers();

    byte[] retryTypeByteArray = headers.lastHeader(RETRY_TYPE_KEY).value();
    String retryType = new String(retryTypeByteArray, StandardCharsets.UTF_8);

    byte[] retryCountByteArray = headers.lastHeader(RETRY_COUNT_KEY).value();
    int retryCount = Ints.fromByteArray(retryCountByteArray);

    byte[] publishTimeByteArray = headers.lastHeader(PUBLISH_TIME_KEY).value();
    long publishTime = Longs.fromByteArray(publishTimeByteArray);

    return new RecordMetadata(retryType, retryCount, publishTime);
  }

  public RecordHeaders toRecordHeaders() {
    RecordHeaders headers = new RecordHeaders();
    headers.add(new RecordHeader(RETRY_TYPE_KEY, getRetryTypeAsByteArray()));
    headers.add(new RecordHeader(PUBLISH_TIME_KEY, getPublishTimeAsByteArray()));
    headers.add(new RecordHeader(RETRY_COUNT_KEY, getRetryCountAsByteArray()));
    return headers;
  }

  private byte[] getRetryCountAsByteArray() {
    return Ints.toByteArray(retryCount);
  }

  private byte[] getPublishTimeAsByteArray() {
    return Longs.toByteArray(getPublishTime());
  }

  private byte[] getRetryTypeAsByteArray() {
    String retryType = getRetryType();
    Preconditions.checkArgument(retryType != null, "Retry Type cannot be null when converting to record headers!");
    return retryType.getBytes(StandardCharsets.UTF_8);
  }
}
