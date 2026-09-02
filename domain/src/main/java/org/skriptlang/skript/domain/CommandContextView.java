package org.skriptlang.skript.domain;

import org.jetbrains.annotations.Nullable;

public interface CommandContextView {
	@Nullable Object get(DomainArgument<?> argument);

	String input();
}
