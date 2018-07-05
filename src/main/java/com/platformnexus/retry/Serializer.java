package com.platformnexus.retry;

import java.io.IOException;

public interface Serializer{
  String serialize(Object o) throws IOException;

  Object deserialize(String data, Class clazz) throws IOException;
}
