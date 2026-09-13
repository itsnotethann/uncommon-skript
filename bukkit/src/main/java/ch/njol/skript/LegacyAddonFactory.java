package ch.njol.skript;

import java.io.File;

import org.jetbrains.annotations.Nullable;

public final class LegacyAddonFactory {

	private LegacyAddonFactory() {}

	public static SkriptAddon create(
		String name,
		String version,
		Class<?> sourceClass,
		File dataFolder,
		@Nullable File jarFile,
		org.skriptlang.skript.addon.SkriptAddon addon
	) {
		return new SkriptAddon(name, version, sourceClass, dataFolder, jarFile, addon);
	}
}
