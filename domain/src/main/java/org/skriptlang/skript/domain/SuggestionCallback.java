package org.skriptlang.skript.domain;

@FunctionalInterface
public interface SuggestionCallback {
	void suggest(DomainSender sender, CommandContextView context, SuggestionSink sink);
}
