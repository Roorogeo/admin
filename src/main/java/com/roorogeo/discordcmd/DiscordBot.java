package com.roorogeo.discordcmd;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.server.MinecraftServer;

import java.util.EnumSet;

/**
 * Owns the JDA connection and forwards eligible channel messages to the server as commands.
 */
public class DiscordBot extends ListenerAdapter {
	private final MinecraftServer server;
	private final ModConfig config;
	private final CommandRunner runner;
	private JDA jda;

	public DiscordBot(MinecraftServer server, ModConfig config) {
		this.server = server;
		this.config = config;
		this.runner = new CommandRunner(server);
	}

	public void start() throws InterruptedException {
		// MESSAGE_CONTENT is a privileged intent; it must also be enabled for the bot in the
		// Discord Developer Portal (Bot → Privileged Gateway Intents → Message Content Intent).
		this.jda = JDABuilder.createLight(config.botToken,
						EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
				.setMemberCachePolicy(MemberCachePolicy.NONE)
				.setChunkingFilter(ChunkingFilter.NONE)
				.disableCache(EnumSet.allOf(CacheFlag.class))
				.addEventListeners(this)
				.build();
		this.jda.awaitReady();
	}

	public void shutdown() {
		if (jda != null) {
			jda.shutdownNow();
			jda = null;
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		// Only the configured channel.
		if (!event.getChannel().getId().equals(config.channelId)) {
			return;
		}
		// Never react to our own bot's messages (e.g. the output we post back) to avoid loops.
		if (jda != null && event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
			return;
		}

		// Webhook messages are intentionally allowed through: a webhook is a valid way to
		// "chat in the channel", so it may drive commands just like a human can.
		String authorId = event.getAuthor().getId();
		if (!config.isAllowed(authorId)) {
			return;
		}

		String raw = event.getMessage().getContentRaw();
		if (raw == null || raw.isBlank()) {
			return;
		}

		String command = extractCommand(raw);
		if (command == null) {
			return;
		}

		final MessageChannel channel = event.getChannel();
		final String label = describeAuthor(event);

		// Commands must run on the main server thread.
		server.execute(() -> {
			DiscordCommandBridge.LOGGER.info("Running command from Discord ({}): {}", label, command);
			String output = runner.run(command);
			if (config.postCommandOutput) {
				postOutput(channel, output);
			}
		});
	}

	private String extractCommand(String raw) {
		String trimmed = raw.strip();
		String prefix = config.commandPrefix;
		if (prefix != null && !prefix.isEmpty()) {
			if (!trimmed.startsWith(prefix)) {
				return null;
			}
			trimmed = trimmed.substring(prefix.length()).strip();
		}
		// Minecraft commands are run without a leading slash; strip one if present.
		if (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1).strip();
		}
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String describeAuthor(MessageReceivedEvent event) {
		String name = event.getAuthor().getName();
		if (event.isWebhookMessage()) {
			return name + " [webhook]";
		}
		return name;
	}

	private static void postOutput(MessageChannel channel, String output) {
		String body = (output == null || output.isBlank()) ? "(no output)" : output;
		// Discord hard-limits messages to 2000 characters.
		if (body.length() > 1900) {
			body = body.substring(0, 1900) + "\n… (truncated)";
		}
		try {
			channel.sendMessage("```\n" + body + "\n```").queue();
		} catch (RuntimeException e) {
			DiscordCommandBridge.LOGGER.warn("Could not post command output to Discord.", e);
		}
	}
}
