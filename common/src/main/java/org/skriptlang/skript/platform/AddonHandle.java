package org.skriptlang.skript.platform;

import java.io.File;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

public interface AddonHandle {
	String name();

	String version();

	File dataFolder();

	File jarFile();

	@Nullable InputStream resource(String path);

	Class<?> source();

	org.slf4j.Logger logger();

	boolean isEnabled();

	void setEnabled(boolean enabled);

	void onEnable();

	void onDisable();
}
