package com.platformnexus.retry.exception;

public class ScheduleRetryException extends RuntimeException {

  public ScheduleRetryException(String message, Throwable cause) {
    super(message, cause);
  }
}
