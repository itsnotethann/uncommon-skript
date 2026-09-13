package org.skriptlang.skript.platform.bukkit;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.platform.PlatformEnvironment;

public class SpiServer implements Server {

	private final PlatformEnvironment environment;
	private File serverDirectory;

	public SpiServer(PlatformEnvironment environment) {
		this.environment = environment;
		this.serverDirectory = environment.serverDirectory();
	}

	@Override
	public @Nullable ConsoleCommandSender getConsoleSender() {
		return null;
	}

	@Override
	public Collection<Player> getOnlinePlayers() {
		return List.of();
	}

	@Override
	public boolean getOnlineMode() {
		return false;
	}

	@Override
	public String getVersion() {
		return environment.platformVersion();
	}

	@Override
	public String getName() {
		return "uncommon-skript";
	}

	@Override
	public @Nullable Player getPlayer(UUID uuid) {
		return null;
	}

	@Override
	public File getServerDirectory() {
		return serverDirectory;
	}

	@Override
	public void setServerDirectory(File serverDirectory) {
		this.serverDirectory = serverDirectory;
	}
}
