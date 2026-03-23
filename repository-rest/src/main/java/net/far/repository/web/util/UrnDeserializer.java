package net.far.repository.web.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import net.far.resolver.model.Urn;

public final class UrnDeserializer extends JsonDeserializer<Urn> {

  @Override
  public Urn deserialize(final JsonParser parser, final DeserializationContext context)
      throws IOException {
    return Urn.parse(parser.getText());
  }
}
