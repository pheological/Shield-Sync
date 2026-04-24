package pheological.shieldSync;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerConnectionListener implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> probeTasks = new ConcurrentHashMap<>();

    public PlayerConnectionListener(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        trackPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final BukkitTask task = this.probeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        PlayerDataManager.remove(uuid);
        ShieldSync.debugChat(player, "Stopped ping probe task.");
    }

    public void trackPlayer(final Player player) {
        final User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            ShieldSync.debugChat(player, "PacketEvents user not ready yet; probe task was not started.");
            return;
        }

        PlayerDataManager.add(user, player);

        final UUID uuid = player.getUniqueId();
        final BukkitTask previousTask = this.probeTasks.remove(uuid);
        if (previousTask != null) {
            previousTask.cancel();
        }

        final BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    probeTasks.remove(uuid);
                    return;
                }

                if (!ShieldSync.isPluginEnabled()) {
                    return;
                }

                ChannelHelper.runInEventLoop(user.getChannel(), () ->
                        user.sendPacket(new WrapperPlayServerKeepAlive(ShieldSync.PROBE_ID))
                );
            }
        }.runTaskTimerAsynchronously(this.plugin, 0L, 10L);

        this.probeTasks.put(uuid, task);
        ShieldSync.debugChat(player, "Started ping probe task (every 10 ticks, id=" + ShieldSync.PROBE_ID + ").");
    }

    public void cancelAllTasks() {
        this.probeTasks.values().forEach(BukkitTask::cancel);
        this.probeTasks.clear();
    }
}