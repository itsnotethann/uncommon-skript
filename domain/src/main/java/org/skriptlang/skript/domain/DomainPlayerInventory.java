package org.skriptlang.skript.domain;

public interface DomainPlayerInventory extends DomainInventory {
	DomainItemStack getCursorItem();

	void setCursorItem(DomainItemStack item);

	DomainItemStack getEquipment(DomainEquipmentSlot slot);
}
