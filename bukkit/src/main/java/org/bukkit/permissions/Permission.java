package org.bukkit.permissions;

import org.jetbrains.annotations.Nullable;

public class Permission {

	private final String name;
	private final String description;

	public Permission(String name) {
		this(name, null);
	}

	public Permission(String name, @Nullable String description) {
		this.name = name;
		this.description = description == null ? "" : description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
}
