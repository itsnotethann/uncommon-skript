package org.skriptlang.skript.platform;

public interface PlatformEnvironment {
	String platformVersion();

	boolean isPrimaryThread();
}
