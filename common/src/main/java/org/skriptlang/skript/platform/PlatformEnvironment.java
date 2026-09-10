package org.skriptlang.skript.platform;

import java.io.File;

import org.slf4j.Logger;

public interface PlatformEnvironment {
	String platformVersion();

	boolean isPrimaryThread();

	File serverDirectory();

	Logger logger();
}
