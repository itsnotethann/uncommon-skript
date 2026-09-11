package org.skriptlang.skript.platform;

import java.io.File;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

public interface AddonHandle {
	String name();

	String version();

	@Nullable String website();

	File dataFolder();

	File jarFile();

	@Nullable InputStream resource(String path);

	Class<?> source();

	default @Nullable Object nativeHandle() {
		return null;
	}

	org.slf4j.Logger logger();

	PlatformScheduler scheduler();

	boolean isEnabled();

	void setEnabled(boolean enabled);

	void onEnable();

	void onDisable();
}
