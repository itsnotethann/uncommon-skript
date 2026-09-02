package org.skriptlang.skript.domain;

@FunctionalInterface
public interface CommandExecutor {
	void run(DomainSender sender, CommandContextView context);
}
