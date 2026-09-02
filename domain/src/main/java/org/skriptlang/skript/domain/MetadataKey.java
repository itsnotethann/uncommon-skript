package org.skriptlang.skript.domain;

public record MetadataKey<T>(String namespace, String name, Class<T> type) {
}
