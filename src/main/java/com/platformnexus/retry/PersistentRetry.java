package com.platformnexus.retry;


import com.platformnexus.retry.exception.FatalRetryException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;

public abstract class PersistentRetry<T extends Serializable> {
  private Serializer serializer = new SerializerImpl();

  protected final RetryBroker retryBroker;

  public PersistentRetry(RetryBroker retryBroker) {
    this.retryBroker = retryBroker;
    this.retryBroker.register(this);
  }

  public void schedule(T o) throws IOException {
    retryBroker.schedule(o);
  }

  public Serializer getSerializer() {
    return serializer;
  }

  public void setSerializer(Serializer serializer) {
    this.serializer = serializer;
  }

  public abstract void handleRetry(T o) throws FatalRetryException;

  public abstract  void onFatalRetry(T o);

  /**
   * You must override this class if you rename your class that extends {@link PersistentRetry} or
   * you decide to override {@link #getDataClass()}
   */
  public String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Used by the deserializer as type must be specified in order to inflate desired T object.
   * Please note that this only works for classes where T does not contain generics. If your T
   * contains fields with nested generics (parameterized types), you will need to override this
   * method and supply your own expected data class.
   *
   * @return Class T
   */
  public Class<T> getDataClass() {
    return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
        .getActualTypeArguments()[0];
  }

}
