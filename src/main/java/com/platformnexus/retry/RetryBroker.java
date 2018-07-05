package com.platformnexus.retry;

import java.io.IOException;

public interface RetryBroker {

  void start();

  void schedule(Object o) throws IOException;

  void shutdown() throws InterruptedException;

  void register(PersistentRetry tPersistentRetry);
}
