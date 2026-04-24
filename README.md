# ShieldSync
https://modrinth.com/plugin/shield-sync

ShieldSync is a Paper plugin that keeps shield raise timing consistent across different pings.

In vanilla, shields have a 5 tick (250 ms) raise delay built in, regardless of ping. However, when you are higher ping, the delay that raises your shield will be the 5 tick delay + your ping. What this mod does is subtract your ping from the 5 tick delay, which reduces the shield delay closer to 250 ms regardless of ping, simulating how shielding would be on lower ping.

## What It Does

- Fetches a players ping using `keepAlive` packets.
- Converts ping into shield compensation ticks.
- Due to how the ticking system works, the ping must be calculated into ticks. **This means that the delay can only be reduced by multiples of 50 ms. For example, if a player is 80 ms, it can only reduce the delay by 50 ms (1 tick), so the shields will feel like what it would on 30 ms, not 0. If they are 180 ms, itll reduce it by 150 ms (3 ticks), and it'll feel like 30 ms.**
- If ping is over 250 ms, it'll remove all 5 ticks.
  
![Pretty much how it works, 150 ms is subtracted so shielding will feel synonymous to someone who's 30 ms.](https://cdn.modrinth.com/data/cached_images/ce032bdd0f9873dec62762588738191bd78ecc51.png)
150 ms is subtracted so shielding will feel synonymous to someone who's 30 ms.
## Requirements
**NOTE: This mod has only been tested on 1.21.11. Use this version for the best results. DM me on discord (pheological) if you want another version for your server.**
**
Must be after version 1.21
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

## Credits
- xSweetJapan for idea (sweetjapan on discord)
- [caseload](https://modrinth.com/user/caseload) for ping fetching implementation
