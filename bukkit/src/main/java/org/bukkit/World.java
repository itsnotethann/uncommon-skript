package org.bukkit;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

public interface World {

	String getName();

	UUID getUID();

	List<Player> getPlayers();
}
