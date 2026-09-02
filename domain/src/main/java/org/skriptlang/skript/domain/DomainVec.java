package org.skriptlang.skript.domain;

public interface DomainVec {
	double x();

	double y();

	double z();

	DomainVec add(DomainVec other);

	DomainVec sub(DomainVec other);

	DomainVec mul(double multiplier);

	double dot(DomainVec other);

	DomainVec cross(DomainVec other);

	double length();

	double lengthSquared();

	DomainVec normalize();

	DomainPoint asPoint();
}
