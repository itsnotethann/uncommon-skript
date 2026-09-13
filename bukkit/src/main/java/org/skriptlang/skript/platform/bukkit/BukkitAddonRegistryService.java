package org.skriptlang.skript.platform.bukkit;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.platform.AddonHandle;
import org.skriptlang.skript.platform.AddonRegistry;

public final class BukkitAddonRegistryService implements AddonRegistry {

	private volatile @Nullable AddonRegistry delegate;

	private AddonRegistry delegate() {
		AddonRegistry current = delegate;
		if (current == null) {
			synchronized (this) {
				current = delegate;
				if (current == null)
					delegate = current = BukkitAddonSupport.install();
			}
		}
		return current;
	}

	@Override
	public @Nullable AddonHandle load(File jar) {
		return delegate().load(jar);
	}

	@Override
	public List<AddonHandle> addons() {
		return delegate().addons();
	}

	@Override
	public @Nullable AddonHandle providingAddon(Class<?> from) {
		return delegate().providingAddon(from);
	}
}
