package com.platformnexus.retry.exception;

/**
 * This class when thrown indicates that a conditions was met where a process will never succeed.
 */
public class FatalRetryException extends Exception {
  public FatalRetryException(String message) {
    super(message);
  }

  public FatalRetryException(String message, Throwable cause) {
    super(message, cause);
  }
}
