package org.skriptlang.skript.platform.bukkit;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.util.LoggerUtils;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.platform.LogSink;
import org.slf4j.Logger;

public final class LoggerLogSink implements LogSink {

	private final Logger logger;

	public LoggerLogSink(Logger logger) {
		this.logger = logger;
	}

	public static LoggerLogSink platformDefault() {
		return new LoggerLogSink(Bukkit.getBetterLogger());
	}

	@Override
	public void log(Level level, String message, @Nullable Throwable error) {
		if (error == null) {
			LoggerUtils.log(logger, level, message);
		} else if (error instanceof Exception exception) {
			LoggerUtils.log(logger, level, message, exception);
		} else {
			LoggerUtils.log(logger, level, message, new RuntimeException(error));
		}
	}
}
