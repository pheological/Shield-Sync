# ShieldSync

https://modrinth.com/plugin/shield-sync

ShieldSync is a Paper plugin that keeps shield raise timing fair across different pings.

In vanilla, shields have a 5-tick (250 ms) raise delay. On high ping, the server receives shield input later, so blocking feels worse. ShieldSync measures ping with PacketEvents keep-alive probes and reduces the server-side delay so effective raise timing stays close to vanilla intent.

## What It Does

- Sends periodic keep-alive probes to each player.
- Measures round-trip time and keeps a stable ping sample.
- Converts ping into shield compensation ticks.
- Intercepts shield `USE_ITEM` packets (off-hand shield use).
- Applies adjusted shield delay:
  - `adjustedDelay = max(0, 5 - compensationTicks)`

## Requirements

- Paper server (1.21 API target in `plugin.yml`)
- PacketEvents plugin installed on the server
- Java runtime matching your server/build setup (project currently builds with Java 21 toolchain)

## Installation

1. Build the plugin jar.
2. Put `ShieldSync` jar in your server `plugins/` folder.
3. Ensure PacketEvents is also in `plugins/`.
4. Start/restart the server.

## Commands

- `/shieldsync enable` - Enable ShieldSync compensation logic.
- `/shieldsync disable` - Disable ShieldSync compensation logic.
- `/shieldsync status` - Show whether ShieldSync is enabled.
- `/shieldsync debug` - Toggle intensive debug output for all players.
- `/shieldsync debug <player>` - Toggle intensive debug output filtered to one player.

## Notes

- This plugin does not use NMS or reflection.
- PacketEvents is used as an external dependency plugin (not shaded).
- Debug output is intended for testing/tuning and can be noisy.

