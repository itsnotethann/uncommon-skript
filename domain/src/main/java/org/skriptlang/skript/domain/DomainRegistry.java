package org.skriptlang.skript.domain;

import java.util.Iterator;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

public interface DomainRegistry<T extends DomainKeyed> extends Iterable<T> {
	@Nullable T byKey(Key key);

	@Override
	Iterator<T> iterator();
}
