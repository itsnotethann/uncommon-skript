package org.skriptlang.skript.domain;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public interface DomainEntity {
	int getEntityId();

	DomainEntityType getEntityType();

	UUID getUuid();

	DomainPoint getPosition();

	DomainVec getVelocity();

	void setVelocity(DomainVec velocity);

	@Nullable DomainInstance getInstance();

	CompletableFuture<Void> teleport(DomainPos pos);

	CompletableFuture<Void> setInstance(DomainInstance instance, DomainPoint point);

	boolean isRemoved();

	void remove();

	boolean isOnGround();

	boolean hasGravity();

	void setGravity(boolean gravity);

	boolean isGlowing();

	void setGlowing(boolean glowing);

	void setInvisible(boolean invisible);

	@Nullable Component getCustomName();

	void setCustomName(@Nullable Component name);

	boolean isCustomNameVisible();

	void setCustomNameVisible(boolean visible);

	boolean isAutoViewable();

	void setAutoViewable(boolean autoViewable);

	List<DomainPlayer> getViewers();

	List<DomainEntity> getPassengers();

	void addPassenger(DomainEntity passenger);

	boolean hasPhysics();

	void setPhysics(boolean physics);

	@Nullable DomainPoint getTargetBlockPosition(int range);

	@Nullable DomainEntity getLineOfSightEntity(double range, Predicate<DomainEntity> predicate, boolean ignoreBlocks);

	int getAirTicks();

	void setAirTicks(int ticks);

	int getFireTicks();

	void setFireTicks(int ticks);

	int getTickFrozen();

	void setTickFrozen(int ticks);

	boolean isSwimming();

	void setSwimming(boolean swimming);

	boolean isSneaking();

	void setSneaking(boolean sneaking);

	boolean isSprinting();

	void setSprinting(boolean sprinting);

	boolean isSilent();

	void setSilent(boolean silent);
}
