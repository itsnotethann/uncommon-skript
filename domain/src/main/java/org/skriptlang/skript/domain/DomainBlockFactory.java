package org.skriptlang.skript.domain;

import net.kyori.adventure.key.Key;

public interface DomainBlockFactory {
	DomainBlock fromState(String state);

	DomainBlock fromKey(Key key);
}
