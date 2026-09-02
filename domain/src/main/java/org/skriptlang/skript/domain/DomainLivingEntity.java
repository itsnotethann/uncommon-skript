package org.skriptlang.skript.domain;

public interface DomainLivingEntity extends DomainEntity {
	float getHealth();

	void setHealth(float health);

	float getMaxHealth();

	double getAttributeValue(DomainAttribute attribute);

	void damage(float amount);

	void kill();
}
