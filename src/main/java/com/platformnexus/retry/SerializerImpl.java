package com.platformnexus.retry;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class SerializerImpl implements Serializer {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String serialize(Object o) throws IOException {
    return MAPPER.writeValueAsString(o);
  }

  @Override
  public Object deserialize(String data, Class clazz) throws IOException {
    return MAPPER.readValue(data, clazz);
  }
}
