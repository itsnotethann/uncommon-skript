package org.skriptlang.skript.domain;

import org.jetbrains.annotations.Nullable;

public interface CommandRegistry {
	DomainCommand create(String name, String... aliases);

	void register(DomainCommand command);

	void unregister(DomainCommand command);

	@Nullable DomainArgument<?> argument(String id, String typeInput);

	Object toScriptValue(Object parsed, DomainSender sender, DomainArgument<?> argument);
}
