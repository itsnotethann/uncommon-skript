package org.skriptlang.skript.domain;

public interface EntityFactory {
	DomainEntity create(DomainEntityType type);

	DomainLivingEntity createLiving(DomainEntityType type);

	DomainEntity createNavigable(DomainEntityType type);
}
