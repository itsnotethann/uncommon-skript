package org.skriptlang.skript.domain;

import net.kyori.adventure.text.Component;

public interface DomainScoreboard {
	Component getLineContent(int line);

	void setLine(int line, Component content, Component numberFormat);

	void removeLine(int line);

	void addViewer(DomainPlayer player);

	void removeViewer(DomainPlayer player);
}
