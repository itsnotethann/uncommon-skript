package org.skriptlang.skript.domain;

import java.util.function.BiPredicate;

public interface DomainCommand {
	void addSubcommand(DomainCommand child);

	void setCondition(BiPredicate<DomainSender, String> condition);

	void addSyntax(CommandExecutor executor, DomainArgument<?>... arguments);

	void setDefaultExecutor(CommandExecutor executor);
}
