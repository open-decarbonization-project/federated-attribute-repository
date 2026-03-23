package net.far.repository.model.type;

public sealed interface Structured extends Type permits QuantityType, ListType, RecordType {}
