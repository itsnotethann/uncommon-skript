package org.bukkit;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.Nullable;

public interface OfflinePlayer extends ServerOperator {

	@Nullable String getName();

	UUID getUniqueId();

	boolean isOnline();

	@Nullable Player getPlayer();
}
