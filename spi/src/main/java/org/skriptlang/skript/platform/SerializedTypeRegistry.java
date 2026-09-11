package org.skriptlang.skript.platform;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.serialization.MapSerializable;

public interface SerializedTypeRegistry {
	void register(Class<? extends MapSerializable> type, String alias);

	@Nullable Class<? extends MapSerializable> byAlias(String alias);

	@Nullable String aliasOf(Class<? extends MapSerializable> type);
}
