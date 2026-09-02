package org.skriptlang.skript.domain;

public interface DomainRegistries {
	DomainRegistry<DomainEnchantment> enchantments();

	DomainRegistry<DomainBiome> biomes();

	DomainRegistry<DomainDimensionType> dimensions();
}
