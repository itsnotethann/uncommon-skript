package org.skriptlang.skript.domain;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public interface SuggestionEntryView {
	String literal();

	@Nullable Component tooltip();
}
