package com.roorogeo.discordcmd;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Runs a command string against the server with full (level 4) permissions and captures
 * the feedback so it can be sent back to Discord.
 *
 * <p>The base source comes from {@link MinecraftServer#createCommandSourceStack()}, which is
 * the server console source: permission level 4, able to run anything. We swap in our own
 * {@link CommandSource} sink so success and failure messages are collected rather than only
 * printed to the console log.
 */
public class CommandRunner {
	private final MinecraftServer server;

	public CommandRunner(MinecraftServer server) {
		this.server = server;
	}

	/**
	 * Executes the given command (no leading slash) on the calling thread, which must be the
	 * main server thread. Returns the captured feedback text.
	 */
	public String run(String command) {
		StringBuilder captured = new StringBuilder();

		CommandSource sink = new CommandSource() {
			@Override
			public void sendSystemMessage(Component message) {
				if (captured.length() > 0) {
					captured.append('\n');
				}
				captured.append(message.getString());
			}

			@Override
			public boolean acceptsSuccess() {
				return true;
			}

			@Override
			public boolean acceptsFailure() {
				return true;
			}

			@Override
			public boolean shouldInformAdmins() {
				return false;
			}
		};

		// createCommandSourceStack() is already the level-4 console source; we only swap the
		// output sink so command feedback is captured instead of merely logged.
		CommandSourceStack source = server.createCommandSourceStack().withSource(sink);

		try {
			server.getCommands().performPrefixedCommand(source, command);
		} catch (Exception e) {
			DiscordCommandBridge.LOGGER.error("Error while executing command: {}", command, e);
			if (captured.length() > 0) {
				captured.append('\n');
			}
			captured.append("Error: ").append(e.getMessage());
		}

		return captured.toString();
	}
}
