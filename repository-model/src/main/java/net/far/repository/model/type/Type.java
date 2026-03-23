package net.far.repository.model.type;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.far.resolver.model.Value;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "name")
@JsonSubTypes({
  @JsonSubTypes.Type(value = StringType.class, name = "string"),
  @JsonSubTypes.Type(value = NumericType.class, name = "numeric"),
  @JsonSubTypes.Type(value = BooleanType.class, name = "boolean"),
  @JsonSubTypes.Type(value = DateTimeType.class, name = "datetime"),
  @JsonSubTypes.Type(value = QuantityType.class, name = "quantity"),
  @JsonSubTypes.Type(value = ListType.class, name = "list"),
  @JsonSubTypes.Type(value = RecordType.class, name = "record"),
})
public sealed interface Type permits Primitive, Structured {
  boolean matches(Value value);
}
