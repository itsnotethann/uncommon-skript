package org.bukkit.command;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@link Command} belonging to a plugin
 */
public final class PluginCommand extends Command implements PluginIdentifiableCommand {
	private final Plugin owningPlugin;
	private CommandExecutor executor;
	private TabCompleter completer;

	public PluginCommand(@NotNull String name, @NotNull Plugin owner) {
		super(name);
		this.executor = owner;
		this.owningPlugin = owner;
		this.usageMessage = "";
	}

	/**
	 * Sets the {@link CommandExecutor} to run when parsing this command
	 *
	 * @param executor New executor to run
	 */
	public void setExecutor(@Nullable CommandExecutor executor) {
		this.executor = executor == null ? owningPlugin : executor;
	}

	/**
	 * Gets the {@link CommandExecutor} associated with this command
	 *
	 * @return CommandExecutor object linked to this command
	 */
	@NotNull
	public CommandExecutor getExecutor() {
		return executor;
	}

	/**
	 * Sets the {@link TabCompleter} to run when tab-completing this command.
	 * <p>
	 * If no TabCompleter is specified, and the command's executor implements
	 * TabCompleter, then the executor will be used for tab completion.
	 *
	 * @param completer New tab completer
	 */
	public void setTabCompleter(@Nullable TabCompleter completer) {
		this.completer = completer;
	}

	/**
	 * Gets the {@link TabCompleter} associated with this command.
	 *
	 * @return TabCompleter object linked to this command
	 */
	@Nullable
	public TabCompleter getTabCompleter() {
		return completer;
	}

	/**
	 * Gets the owner of this PluginCommand
	 *
	 * @return Plugin that owns this command
	 */
	@Override
	@NotNull
	public Plugin getPlugin() {
		return owningPlugin;
	}

	@Override
	public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
		return true;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder(super.toString());
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		stringBuilder.append(", ").append(owningPlugin.getDescription().getFullName()).append(')');
		return stringBuilder.toString();
	}
}