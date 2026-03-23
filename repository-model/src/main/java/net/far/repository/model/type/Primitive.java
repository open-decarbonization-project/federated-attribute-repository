package net.far.repository.model.type;

public sealed interface Primitive extends Type
    permits StringType, NumericType, BooleanType, DateTimeType {}
