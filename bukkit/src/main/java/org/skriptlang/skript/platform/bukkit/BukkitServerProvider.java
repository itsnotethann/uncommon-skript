package org.skriptlang.skript.platform.bukkit;

import org.bukkit.Server;
import org.skriptlang.skript.platform.PlatformProvider;

public interface BukkitServerProvider {

	Server create(PlatformProvider provider);
}
