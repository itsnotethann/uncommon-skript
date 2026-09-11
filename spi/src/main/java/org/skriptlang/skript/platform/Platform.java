package org.skriptlang.skript.platform;

import java.util.Iterator;
import java.util.ServiceLoader;

public final class Platform {

	private Platform() {}

	private static volatile PlatformProvider provider;

	public static void install(PlatformProvider provider) {
		Platform.provider = provider;
	}

	public static PlatformProvider provider() {
		PlatformProvider current = provider;
		if (current != null)
			return current;
		synchronized (Platform.class) {
			current = provider;
			if (current == null)
				provider = current = discover();
			return current;
		}
	}

	private static PlatformProvider discover() {
		Iterator<PlatformProvider> candidates =
			ServiceLoader.load(PlatformProvider.class, Platform.class.getClassLoader()).iterator();
		if (candidates.hasNext())
			return candidates.next();
		throw new IllegalStateException(
			"No PlatformProvider found. The host must call Platform.install(...) before Skript loads,"
				+ " or supply one through META-INF/services.");
	}
}
