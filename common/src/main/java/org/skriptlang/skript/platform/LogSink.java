package org.skriptlang.skript.platform;

import java.util.logging.Level;

import org.jetbrains.annotations.Nullable;

public interface LogSink {
	void log(Level level, String message, @Nullable Throwable error);
}
