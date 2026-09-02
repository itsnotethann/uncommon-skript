package org.skriptlang.skript.domain;

public interface DomainInventory {
	DomainItemStack[] getItemStacks();

	void addItemStack(DomainItemStack item);

	void copyContents(DomainItemStack[] stacks);

	void clear();

	DomainInventoryType getType();
}
