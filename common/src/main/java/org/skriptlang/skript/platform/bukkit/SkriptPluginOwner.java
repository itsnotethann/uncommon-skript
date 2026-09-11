package org.skriptlang.skript.platform.bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkriptPluginOwner extends JavaPlugin {

	private static SkriptPluginOwner instance;

	private PluginDescriptionFile description;

	public static synchronized SkriptPluginOwner get() {
		SkriptPluginOwner current = instance;
		if (current == null)
			instance = current = new SkriptPluginOwner();
		return current;
	}

	@Override
	public PluginDescriptionFile getDescription() {
		PluginDescriptionFile current = description;
		if (current == null)
			description = current = new PluginDescriptionFile(
				"Skript", readVersion(), "ch.njol.skript.Skript", null, List.of(), List.of());
		return current;
	}

	private static String readVersion() {
		try (InputStream in = SkriptPluginOwner.class.getResourceAsStream("/version")) {
			if (in == null)
				return "1.0.0";
			String version = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
			return version.isEmpty() ? "1.0.0" : version;
		} catch (IOException e) {
			return "1.0.0";
		}
	}
}
