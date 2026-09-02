package org.skriptlang.skript.domain;

import java.util.Collection;
import java.util.UUID;

import net.kyori.adventure.sound.Sound;

public interface DomainInstance {
	DomainBlock getBlock(DomainPoint point);

	void setBlock(DomainPoint point, DomainBlock block, boolean update);

	Collection<DomainPlayer> getPlayers();

	Collection<DomainEntity> getEntities();

	boolean isChunkLoaded(DomainPoint point);

	UUID getUuid();

	boolean isRegistered();

	long getTime();

	void setTime(long time);

	void playSound(Sound sound, DomainPoint point);

	void playSound(Sound sound, DomainEntity emitter);
}
