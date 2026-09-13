package org.bukkit.entity;

import org.bukkit.OfflinePlayer;

public interface Player extends HumanEntity, OfflinePlayer {

	String getDisplayName();

	void kickPlayer(String message);
}
