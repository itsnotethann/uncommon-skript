package org.skriptlang.skript.platform;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public interface AddonRegistry {
	@Nullable AddonHandle load(File jar);

	List<AddonHandle> addons();

	@Nullable AddonHandle providingAddon(Class<?> from);
}
