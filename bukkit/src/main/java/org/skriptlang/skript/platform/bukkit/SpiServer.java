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
import org.skriptlang.skript.platform.PlatformProvider;

public class SpiServer implements Server {

	private final PlatformEnvironment environment;
	private final ConsoleCommandSender console;
	private File serverDirectory;

	public SpiServer(PlatformProvider provider) {
		this.environment = provider.environment();
		this.console = new SpiConsoleSender(provider.logSink());
		this.serverDirectory = environment.serverDirectory();
	}

	@Override
	public ConsoleCommandSender getConsoleSender() {
		return console;
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
