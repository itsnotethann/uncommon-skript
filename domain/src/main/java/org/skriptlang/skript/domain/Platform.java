package org.skriptlang.skript.domain;

import java.util.Collection;
import java.util.Optional;

public interface Platform {
	Collection<DomainPlayer> onlinePlayers();

	DomainSender console();

	Collection<DomainInstance> instances();

	void unregisterInstance(DomainInstance instance);

	PlayerResolver playerResolver();

	EntityFactory entityFactory();

	DomainBlockFactory blockFactory();

	DomainRegistries registries();

	DomainScheduler scheduler();

	CommandRegistry commands();

	Optional<PermissionProvider> permissions();
}
