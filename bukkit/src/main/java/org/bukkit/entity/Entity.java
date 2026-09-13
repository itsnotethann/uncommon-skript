package org.bukkit.entity;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public interface Entity extends CommandSender {

	Location getLocation();

	World getWorld();

	boolean teleport(Location location);

	UUID getUniqueId();

	int getEntityId();

	boolean isDead();

	boolean isValid();

	void remove();
}
