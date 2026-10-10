package com.roorogeo.discordcmd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON-backed configuration, stored at {@code config/discordcmd.json}.
 */
public class ModConfig {
	public static final String TOKEN_PLACEHOLDER = "PUT-YOUR-BOT-TOKEN-HERE";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Discord bot token (from the Discord Developer Portal → your app → Bot → Reset Token). */
	public String botToken = TOKEN_PLACEHOLDER;

	/** The numeric ID of the single channel to watch for commands. */
	public String channelId = "";

	/**
	 * Optional prefix a message must start with to be treated as a command. Empty string
	 * (the default) means every message in the channel is run as a command. A leading
	 * {@code /} is always stripped before running.
	 */
	public String commandPrefix = "";

	/**
	 * Optional allow-list of Discord user (or webhook) IDs permitted to run commands.
	 * When empty, anyone who can post in the channel can run commands — which, per the
	 * request, makes channel access the only barrier.
	 */
	public List<String> allowedUserIds = new ArrayList<>();

	/** When true, the result/feedback of each command is posted back into the channel. */
	public boolean postCommandOutput = true;

	public static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(DiscordCommandBridge.MOD_ID + ".json");
	}

	public static ModConfig loadOrCreate() {
		Path path = configPath();
		try {
			if (Files.exists(path)) {
				String json = Files.readString(path);
				ModConfig loaded = GSON.fromJson(json, ModConfig.class);
				if (loaded != null) {
					loaded.normalize();
					// Rewrite to add any fields introduced in newer versions.
					loaded.save();
					return loaded;
				}
			}
		} catch (IOException | RuntimeException e) {
			DiscordCommandBridge.LOGGER.error("Could not read config at {}; using defaults.", path, e);
		}

		ModConfig fresh = new ModConfig();
		fresh.save();
		DiscordCommandBridge.LOGGER.info("Wrote a fresh config to {}. Fill in botToken and channelId.", path);
		return fresh;
	}

	private void normalize() {
		if (commandPrefix == null) {
			commandPrefix = "";
		}
		if (allowedUserIds == null) {
			allowedUserIds = new ArrayList<>();
		}
	}

	public void save() {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(this));
		} catch (IOException e) {
			DiscordCommandBridge.LOGGER.error("Could not write config to {}.", path, e);
		}
	}

	public boolean isAllowed(String userId) {
		return allowedUserIds.isEmpty() || allowedUserIds.contains(userId);
	}
}
