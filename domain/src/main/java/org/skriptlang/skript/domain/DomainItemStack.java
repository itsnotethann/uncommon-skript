package org.skriptlang.skript.domain;

import java.util.List;
import java.util.Set;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public interface DomainItemStack {
	DomainMaterial material();

	int amount();

	DomainItemStack withAmount(int amount);

	DomainItemStack withDisplayName(@Nullable Component name);

	DomainItemStack withLore(List<Component> lore);

	DomainItemStack withEnchantment(DomainEnchantment enchantment, int level);

	DomainItemStack withNBT(CompoundBinaryTag nbt);

	DomainItemStack withFlags(Set<DomainItemFlag> flags);

	CompoundBinaryTag toNBT();
}
