package com.roorogeo.discordcmd;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Discord Command Bridge.
 *
 * <p>This is a server-side only mod: it connects to Discord as a bot, watches a single
 * channel, and runs whatever is posted there as a server console command with full
 * (level 4) permissions. The only gate on who may run commands is who can post in the
 * watched Discord channel — exactly as requested.
 *
 * <p>Messages posted by Discord webhooks are treated the same as messages typed by a
 * person, so a webhook pointed at the watched channel is enough to drive the server.
 */
public class DiscordCommandBridge implements DedicatedServerModInitializer {
	public static final String MOD_ID = "discordcmd";
	public static final Logger LOGGER = LoggerFactory.getLogger("DiscordCommandBridge");

	private DiscordBot bot;

	@Override
	public void onInitializeServer() {
		ModConfig config = ModConfig.loadOrCreate();

		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

		// Keep a reference so the lifecycle callbacks can reach the loaded config.
		this.pendingConfig = config;
		LOGGER.info("Discord Command Bridge initialized.");
	}

	private ModConfig pendingConfig;

	private void onServerStarted(MinecraftServer server) {
		ModConfig config = this.pendingConfig;
		if (config == null) {
			return;
		}

		if (config.botToken == null || config.botToken.isBlank()
				|| config.botToken.equals(ModConfig.TOKEN_PLACEHOLDER)) {
			LOGGER.warn("No Discord bot token configured. Edit config/{}.json and set \"botToken\" "
					+ "and \"channelId\", then restart the server.", MOD_ID);
			return;
		}
		if (config.channelId == null || config.channelId.isBlank()) {
			LOGGER.warn("No Discord channelId configured. Edit config/{}.json and set \"channelId\", "
					+ "then restart the server.", MOD_ID);
			return;
		}

		try {
			this.bot = new DiscordBot(server, config);
			this.bot.start();
			LOGGER.info("Connected to Discord. Watching channel {} for commands.", config.channelId);
		} catch (Exception e) {
			LOGGER.error("Failed to start the Discord bot.", e);
		}
	}

	private void onServerStopping(MinecraftServer server) {
		if (this.bot != null) {
			this.bot.shutdown();
			this.bot = null;
		}
	}
}
