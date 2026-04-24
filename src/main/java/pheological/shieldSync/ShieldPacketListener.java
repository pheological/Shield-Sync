package pheological.shieldSync;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class ShieldPacketListener extends PacketListenerAbstract {
    private final JavaPlugin plugin;

    public ShieldPacketListener(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacketType() != PacketType.Play.Client.USE_ITEM) {
            return;
        }

        final WrapperPlayClientUseItem wrapper = new WrapperPlayClientUseItem(event);

        final UUID uuid = event.getUser().getUUID();
        final PlayerData data = PlayerDataManager.get(uuid);
        final Player player = data != null ? data.getPlayer() : Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        final boolean offHandShieldUse = wrapper.getHand() == InteractionHand.OFF_HAND
                && player.getInventory().getItemInOffHand().getType() == Material.SHIELD;

        if (!ShieldSync.isPluginEnabled()) {
            return;
        }

        if (!offHandShieldUse) {
            return;
        }

        event.setCancelled(true);

        final int compensationTicks = data != null ? data.getCompensationTicks() : fallbackCompensationTicks(player);
        final int adjustedTicks = Math.max(0, 5 - compensationTicks);
        final long pingMs = data != null ? data.getStablePing() : Math.max(0, player.getPing());

        ShieldSync.debugChat(
                player,
                "Shield use intercepted: ping=" + pingMs + "ms, compensation=" + compensationTicks
                        + " ticks, adjustedDelay=" + adjustedTicks + " ticks"
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setShieldBlockingDelay(0);
                    // Audio cue lines up with the compensated raise moment.
                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.7f, 1.35f);
                }
            }
        }.runTaskLater(this.plugin, adjustedTicks);
    }

    private int fallbackCompensationTicks(final Player player) {
        return PlayerData.calculateCompensationTicksFromPing(player.getPing());
    }
}

