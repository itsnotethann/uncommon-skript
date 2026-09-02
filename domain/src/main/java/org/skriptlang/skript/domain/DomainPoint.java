package org.skriptlang.skript.domain;

public interface DomainPoint {
	double x();

	double y();

	double z();

	int blockX();

	int blockY();

	int blockZ();

	DomainPoint withX(double x);

	DomainPoint withY(double y);

	DomainPoint withZ(double z);

	DomainVec sub(DomainPoint other);

	DomainVec asVec();

	double distance(DomainPoint other);

	double distanceSquared(DomainPoint other);
}
