package com.platformnexus.retry.config;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Validated
@Configuration
@ConfigurationProperties(prefix = "platformnexus.app.retry")
public class RetryConfig implements InitializingBean {

  /*
   * Initialized to empty Map to ensure types is never null.
   * {@link $afterPropertiesSet} validates map is not empty on context load.
   */
  private Map<String, RetryType> types = new HashMap<>();

  public RetryType getTypeByName(String name) {
    Preconditions.checkArgument(types.containsKey(name), String.format("Could not locate retry type=%s", name));
    return types.get(name);
  }

  public void setTypes(@NotNull Map<String, RetryType> types) {
    this.types = types;
  }

  @Override
  public void afterPropertiesSet() {
    Preconditions.checkArgument(!types.isEmpty(),
        "app.retry.types cannot be empty. Please define one inside your application.yml or suitable context.");
  }

  @Validated
  public static class RetryType {

    @NotNull
    private String topic;

    @NotNull
    private Interval interval;

    public void setTopic(String topic) {
      this.topic = topic;
    }

    public void setInterval(Interval interval) {
      this.interval = interval;
    }

    public String getTopic() {
      return topic;
    }

    public Interval getInterval() {
      return interval;
    }
  }

  @Validated
  public static class Interval {

    @NotNull
    private ChronoUnit unit;

    @NotNull
    private Long value;

    public ChronoUnit getUnit() {
      return unit;
    }

    public Long getValue() {
      return value;
    }

    public void setUnit(String unit) {
      this.unit = ChronoUnit.valueOf(unit);
    }

    public void setValue(String value) {
      this.value = Long.valueOf(value);
    }

    public long getMillis() {
      return Duration.of(this.value, this.unit).toMillis();
    }
  }
}
