package org.skriptlang.skript.platform.bukkit;

import org.jetbrains.annotations.Nullable;

public interface BukkitTypeBridge {

	@Nullable Object toBukkit(Object value, Class<?> bukkitType);

	void registerSkriptTypes();
}
