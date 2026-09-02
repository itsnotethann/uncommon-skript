package org.skriptlang.skript.domain;

import java.util.List;

public interface SuggestionSink {
	List<SuggestionEntryView> entries();

	void add(String literal);

	void add(SuggestionEntryView entry);
}
