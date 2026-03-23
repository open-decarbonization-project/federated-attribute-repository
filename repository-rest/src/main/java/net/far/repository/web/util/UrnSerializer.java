package net.far.repository.web.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import net.far.resolver.model.Urn;

public final class UrnSerializer extends JsonSerializer<Urn> {

  @Override
  public void serialize(
      final Urn value, final JsonGenerator generator, final SerializerProvider provider)
      throws IOException {
    generator.writeString(value.toString());
  }
}
