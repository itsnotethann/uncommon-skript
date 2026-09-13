package org.bukkit.permissions;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an object that may be assigned permissions
 */
public interface Permissible extends ServerOperator {

	/**
	 * Checks if this object contains an override for the specified
	 * permission, by fully qualified name
	 *
	 * @param name Name of the permission
	 * @return true if the permission is set, otherwise false
	 */
	public boolean isPermissionSet(@NotNull String name);

	/**
	 * Checks if this object contains an override for the specified {@link
	 * Permission}
	 *
	 * @param perm Permission to check
	 * @return true if the permission is set, otherwise false
	 */
	public boolean isPermissionSet(@NotNull Permission perm);

	/**
	 * Gets the value of the specified permission, if set.
	 * <p>
	 * If a permission override is not set on this object, the default value
	 * of the permission will be returned.
	 *
	 * @param name Name of the permission
	 * @return Value of the permission
	 */
	public boolean hasPermission(@NotNull String name);
}