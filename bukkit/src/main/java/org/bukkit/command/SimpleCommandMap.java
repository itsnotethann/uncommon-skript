package org.bukkit.command;

import org.bukkit.Server;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SimpleCommandMap implements CommandMap {
	protected final Map<String, Command> knownCommands = new HashMap<>();
	private final Server server;

	public SimpleCommandMap(@NotNull final Server server) {
		this.server = server;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerAll(@NotNull String fallbackPrefix, @NotNull List<Command> commands) {
		if (commands != null) {
			for (Command c : commands) {
				register(fallbackPrefix, c);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean register(@NotNull String fallbackPrefix, @NotNull Command command) {
		return register(command.getName(), fallbackPrefix, command);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean register(@NotNull String label, @NotNull String fallbackPrefix, @NotNull Command command) {
		label = label.toLowerCase(Locale.ROOT).trim();
		fallbackPrefix = fallbackPrefix.toLowerCase(Locale.ROOT).trim();
		boolean registered = register(label, command, false, fallbackPrefix);

		Iterator<String> iterator = command.getAliases().iterator();
		while (iterator.hasNext()) {
			if (!register(iterator.next(), command, true, fallbackPrefix)) {
				iterator.remove();
			}
		}

		// If we failed to register under the real name, we need to set the command label to the direct address
		if (!registered) {
			command.setLabel(fallbackPrefix + ":" + label);
		}

		// Register to us so further updates of the commands label and aliases are postponed until its reregistered
		command.register(this);

		return registered;
	}

	/**
	 * Registers a command with the given name is possible. Also uses
	 * fallbackPrefix to create a unique name.
	 *
	 * @param label the name of the command, without the '/'-prefix.
	 * @param command the command to register
	 * @param isAlias whether the command is an alias
	 * @param fallbackPrefix a prefix which is prepended to the command for a
	 *     unique address
	 * @return true if command was registered, false otherwise.
	 */
	private synchronized boolean register(@NotNull String label, @NotNull Command command, boolean isAlias, @NotNull String fallbackPrefix) {
		knownCommands.put(fallbackPrefix + ":" + label, command);
		if ((command instanceof BukkitCommand || isAlias) && knownCommands.containsKey(label)) {
			// Request is for an alias/fallback command and it conflicts with
			// a existing command or previous alias ignore it
			// Note: This will mean it gets removed from the commands list of active aliases
			return false;
		}

		boolean registered = true;

		// If the command exists but is an alias we overwrite it, otherwise we return
		Command conflict = knownCommands.get(label);
		if (conflict != null && conflict.getLabel().equals(label)) {
			return false;
		}

		if (!isAlias) {
			command.setLabel(label);
		}
		knownCommands.put(label, command);

		return registered;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean dispatch(@NotNull CommandSender sender, @NotNull String commandLine) throws CommandException {
		String[] args = commandLine.split(" ");

		if (args.length == 0) {
			return false;
		}

		String sentCommandLabel = args[0].toLowerCase(Locale.ROOT);
		Command target = getCommand(sentCommandLabel);

		if (target == null) {
			return false;
		}

		try {
			// Note: we don't return the result of target.execute as thats success / failure, we return handled (true) or not handled (false)
			target.execute(sender, sentCommandLabel, Arrays.copyOfRange(args, 1, args.length));
		} catch (CommandException ex) {
			throw ex;
		} catch (Throwable ex) {
			throw new CommandException("Unhandled exception executing '" + commandLine + "' in " + target, ex);
		}

		// return true as command was handled
		return true;
	}

	@Override
	public synchronized void clearCommands() {
		for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
			entry.getValue().unregister(this);
		}
		knownCommands.clear();
	}

	@Override
	@Nullable
	public Command getCommand(@NotNull String name) {
		Command target = knownCommands.get(name.toLowerCase(Locale.ROOT));
		return target;
	}

	@Override
	@Nullable
	public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String cmdLine) {
		return Collections.emptyList();
	}

	@NotNull
	public Collection<Command> getCommands() {
		return Collections.unmodifiableCollection(knownCommands.values());
	}
}