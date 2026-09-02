package org.skriptlang.skript.domain;

public interface DomainPos extends DomainPoint {
	float yaw();

	float pitch();

	DomainVec direction();

	DomainPos withYaw(float yaw);

	DomainPos withPitch(float pitch);
}
