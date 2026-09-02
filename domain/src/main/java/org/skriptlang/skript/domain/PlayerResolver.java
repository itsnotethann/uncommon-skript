package org.skriptlang.skript.domain;

import org.jetbrains.annotations.Nullable;

public interface PlayerResolver {
	@Nullable DomainPlayer byUuid(java.util.UUID uuid);

	@Nullable DomainPlayer byName(String name, boolean strict);
}
