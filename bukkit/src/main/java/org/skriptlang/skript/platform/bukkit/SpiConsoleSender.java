package org.skriptlang.skript.platform.bukkit;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.platform.LogSink;

public final class SpiConsoleSender implements ConsoleCommandSender {

	private final LogSink logSink;

	public SpiConsoleSender(LogSink logSink) {
		this.logSink = logSink;
	}

	@Override
	public void sendMessage(String message) {
		logSink.log(Level.INFO, message, null);
	}

	@Override
	public void sendMessage(String... messages) {
		for (String message : messages)
			sendMessage(message);
	}

	@Override
	public void sendMessage(@Nullable UUID sender, String message) {
		sendMessage(message);
	}

	@Override
	public void sendMessage(@Nullable UUID sender, String... messages) {
		sendMessage(messages);
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public String getName() {
		return "console";
	}

	@Override
	public boolean isPermissionSet(String name) {
		return true;
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return true;
	}

	@Override
	public boolean hasPermission(String name) {
		return true;
	}

	@Override
	public boolean isOp() {
		return true;
	}

	@Override
	public void setOp(boolean value) {
	}
}
