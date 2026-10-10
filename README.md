# Discord Command Bridge

A **server-side Fabric mod for Minecraft 26.2** that runs commands posted in a Discord
channel on your server, with **full (level 4 / console) permissions**. The only gate on who
can run commands is **who can post in the watched Discord channel** — exactly as requested.

Messages posted by a **Discord webhook** are treated the same as messages typed by a person,
so a webhook pointed at the watched channel is enough to drive the server.

> ⚠️ **Read this first.** Anyone who can post in the watched channel gets full control of your
> server: `op`, `stop`, `ban`, `whitelist`, world edits, everything. Treat access to that
> channel exactly like handing out the server console. Lock the channel's posting permission
> down to people you trust, and consider using the optional allow-list (below).

## Why a bot token and not just a webhook URL

A Discord **webhook can only send** messages into a channel — it cannot *receive* them, so a
webhook alone can't pick up the commands you post. To read the channel, the mod logs in as a
small **Discord bot** and listens. You still get the webhook behaviour you asked for: anything a
webhook posts into the channel is picked up and run. The bot is just the receiving half.

## Requirements

- A Minecraft **26.2** server running **Fabric Loader 0.19.0+** on **Java 21+**
- **Fabric API** installed on the server
- A Discord application with a **bot** (free, via the Discord Developer Portal)

## Setup

### 1. Create the Discord bot

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications) and
   **New Application**.
2. Open **Bot** in the sidebar. Click **Reset Token** and copy the token — this is your
   `botToken`.
3. Still on the **Bot** page, under **Privileged Gateway Intents**, enable
   **Message Content Intent**. (The mod needs this to read what's posted.)
4. Open **OAuth2 → URL Generator**, tick the **bot** scope, and under bot permissions tick at
   least **View Channel** and **Read Message History**. Open the generated URL and invite the
   bot to your server.

### 2. Get the channel ID

In Discord, enable **Settings → Advanced → Developer Mode**, then right-click the channel you
want to use and choose **Copy Channel ID**.

### 3. Install the mod

Drop the built jar into your server's `mods/` folder (alongside Fabric API) and start the
server once. It will generate a config file and log that it needs filling in.

### 4. Configure

Edit `config/discordcmd.json`:

```json
{
  "botToken": "PUT-YOUR-BOT-TOKEN-HERE",
  "channelId": "123456789012345678",
  "commandPrefix": "",
  "allowedUserIds": [],
  "postCommandOutput": true
}
```

| Field | Meaning |
|-------|---------|
| `botToken` | Your Discord bot token. |
| `channelId` | The one channel the mod watches. |
| `commandPrefix` | If set (e.g. `"!"`), only messages starting with it are run (the prefix is stripped). Empty means **every** message in the channel runs as a command. A leading `/` is always stripped. |
| `allowedUserIds` | Optional allow-list of Discord user/webhook IDs. **Empty = anyone who can post in the channel** (channel access is the only barrier). Add IDs to restrict further. |
| `postCommandOutput` | If true, each command's result is posted back into the channel. |

Restart the server. On start you'll see `Connected to Discord. Watching channel … for commands.`

## Usage

Post in the channel (or have a webhook post):

```
say Hello from Discord
time set day
weather clear
op SomePlayer
```

With `postCommandOutput` on, the command's feedback is posted back in a code block.

## Building

You don't need to build it yourself — every push is built by GitHub Actions
(`.github/workflows/build.yml`); download the jar from the run's **Artifacts**. To build
locally instead:

```bash
./gradlew build
```

The finished jar is in `build/libs/`. JDA and its dependencies are shaded into that jar, so
no extra libraries are needed on the server.

## Notes on versions

Built against Minecraft 26.2, which ships unobfuscated, so the mod uses the non-remapping
`net.fabricmc.fabric-loom` toolchain and Mojang mappings (no Yarn). The exact Loom / Fabric
API / Loader versions are pinned in `gradle.properties` and `build.gradle`; if a build fails
because one has moved, bump it there and rebuild — nothing in the mod's own code needs to
change.

## License

MIT — see [LICENSE](LICENSE).
