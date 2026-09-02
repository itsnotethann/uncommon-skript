package org.skriptlang.skript.domain;

import org.jetbrains.annotations.Nullable;

public interface MetadataHolder {
	<T> void setMetadata(MetadataKey<T> key, @Nullable T value);

	<T> @Nullable T getMetadata(MetadataKey<T> key);

	<T> boolean hasMetadata(MetadataKey<T> key);

	<T> void removeMetadata(MetadataKey<T> key);
}
