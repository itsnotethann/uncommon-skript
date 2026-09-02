package org.skriptlang.skript.domain;

import net.kyori.adventure.text.Component;

public interface DomainPlayer extends DomainEntity, DomainSender {
	void kick(Component message);

	DomainGameMode getGameMode();

	void setGameMode(DomainGameMode gameMode);

	int getFood();

	void setFood(int food);

	int getLevel();

	void setLevel(int level);

	float getExp();

	void setExp(float exp);

	int getLatency();

	void refreshLatency(int milliseconds);

	DomainPlayerInventory getInventory();

	void openInventory(DomainInventory inventory);

	void closeInventory();

	String getUsername();

	DomainPlayerSkin getSkin();

	void setSkin(DomainPlayerSkin skin);

	void setAllowFlying(boolean allowFlying);

	void setInstantBreak(boolean instantBreak);

	int getHeldItemSlot();

	void setHeldItemSlot(int slot);

	DomainScoreboard getScoreboard();

	void setScoreboard(DomainScoreboard scoreboard);

	void refreshCommands();
}
