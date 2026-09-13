package org.bukkit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.Nullable;

public class Location implements Cloneable, ConfigurationSerializable {

	private @Nullable World world;
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;

	public Location(@Nullable World world, double x, double y, double z) {
		this(world, x, y, z, 0, 0);
	}

	public Location(@Nullable World world, double x, double y, double z, float yaw, float pitch) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public @Nullable World getWorld() {
		return world;
	}

	public void setWorld(@Nullable World world) {
		this.world = world;
	}

	public boolean isWorldLoaded() {
		return world != null;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public int getBlockX() {
		return locToBlock(x);
	}

	public int getBlockY() {
		return locToBlock(y);
	}

	public int getBlockZ() {
		return locToBlock(z);
	}

	public Location add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	public Location add(Location other) {
		return add(other.x, other.y, other.z);
	}

	public Location subtract(double x, double y, double z) {
		return add(-x, -y, -z);
	}

	public Location subtract(Location other) {
		return add(-other.x, -other.y, -other.z);
	}

	public double distanceSquared(Location other) {
		if (other.world != world)
			throw new IllegalArgumentException("Cannot measure distance between locations in different worlds");
		double dx = x - other.x;
		double dy = y - other.y;
		double dz = z - other.z;
		return dx * dx + dy * dy + dz * dz;
	}

	public double distance(Location other) {
		return Math.sqrt(distanceSquared(other));
	}

	public static int locToBlock(double coordinate) {
		return (int) Math.floor(coordinate);
	}

	@Override
	public Location clone() {
		try {
			return (Location) super.clone();
		} catch (CloneNotSupportedException exception) {
			throw new AssertionError(exception);
		}
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> data = new LinkedHashMap<>();
		if (world != null)
			data.put("world", world.getName());
		data.put("x", x);
		data.put("y", y);
		data.put("z", z);
		data.put("yaw", yaw);
		data.put("pitch", pitch);
		return data;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Location other))
			return false;
		return Objects.equals(world, other.world)
			&& Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0 && Double.compare(z, other.z) == 0
			&& Float.compare(yaw, other.yaw) == 0 && Float.compare(pitch, other.pitch) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(world, x, y, z, yaw, pitch);
	}

	@Override
	public String toString() {
		return "Location{world=" + (world == null ? null : world.getName()) + ",x=" + x + ",y=" + y + ",z=" + z
			+ ",pitch=" + pitch + ",yaw=" + yaw + "}";
	}
}
