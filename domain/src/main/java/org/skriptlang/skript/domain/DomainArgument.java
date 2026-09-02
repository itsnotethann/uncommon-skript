package org.skriptlang.skript.domain;

import org.jetbrains.annotations.Nullable;

public interface DomainArgument<T> {
	String id();

	void setDefaultValue(@Nullable Object value);

	void setSuggestionCallback(SuggestionCallback callback);

	boolean setFormat(CaseFormat format);
}
