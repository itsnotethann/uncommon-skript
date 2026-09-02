package org.skriptlang.skript.domain;

import org.jetbrains.annotations.Nullable;

public interface PermissionProvider {
	@Nullable Integer groupWeight(String groupName);
}
