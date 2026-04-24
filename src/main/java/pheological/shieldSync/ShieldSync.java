package pheological.shieldSync;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShieldSync extends JavaPlugin implements TabExecutor {
    public static final long PROBE_ID = 41701L;
    private static ShieldSync instance;
    private static final Set<UUID> DEBUG_VIEWERS = ConcurrentHashMap.newKeySet();

    private static volatile boolean intensiveDebugEnabled;
    private static volatile UUID filteredPlayer;
    private static volatile boolean pluginEnabled = true;

    private PlayerConnectionListener connectionListener;

    @Override
    public void onLoad() {
        instance = this;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        pluginEnabled = true;
        PacketEvents.getAPI().init();

        final var shieldSyncCommand = getCommand("shieldsync");
        if (shieldSyncCommand != null) {
            shieldSyncCommand.setExecutor(this);
            shieldSyncCommand.setTabCompleter(this);
        }

        this.connectionListener = new PlayerConnectionListener(this);
        Bukkit.getPluginManager().registerEvents(this.connectionListener, this);

        PacketEvents.getAPI().getEventManager().registerListener(new PingSendListener());
        PacketEvents.getAPI().getEventManager().registerListener(new PingReceiveListener());
        PacketEvents.getAPI().getEventManager().registerListener(new ShieldPacketListener(this));

        Bukkit.getOnlinePlayers().forEach(this.connectionListener::trackPlayer);
    }

    @Override
    public void onDisable() {
        if (this.connectionListener != null) {
            this.connectionListener.cancelAllTasks();
        }
        PlayerDataManager.clear();
        PacketEvents.getAPI().terminate();

        DEBUG_VIEWERS.clear();
        intensiveDebugEnabled = false;
        filteredPlayer = null;
        pluginEnabled = false;
        instance = null;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!command.getName().equalsIgnoreCase("shieldsync")) {
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " debug [player]");
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <enable|disable|status>");
            return true;
        }

        if (args[0].equalsIgnoreCase("enable")) {
            pluginEnabled = true;
            sender.sendMessage(ChatColor.GREEN + "ShieldSync enabled.");
            debugChat((UUID) null, "Plugin enabled.");
            return true;
        }
        if (args[0].equalsIgnoreCase("disable")) {
            pluginEnabled = false;
            sender.sendMessage(ChatColor.YELLOW + "ShieldSync disabled.");
            debugChat((UUID) null, "Plugin disabled.");
            return true;
        }
        if (args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.AQUA + "ShieldSync: " + (pluginEnabled ? "ENABLED" : "DISABLED"));
            return true;
        }

        if (!args[0].equalsIgnoreCase("debug")) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " debug [player]");
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <enable|disable|status>");
            return true;
        }

        if (args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " debug [player]");
            return true;
        }

        if (sender instanceof Player playerSender) {
            DEBUG_VIEWERS.add(playerSender.getUniqueId());
        }

        if (args.length == 1) {
            if (intensiveDebugEnabled && filteredPlayer == null) {
                intensiveDebugEnabled = false;
                sender.sendMessage(ChatColor.YELLOW + "ShieldSync debug disabled.");
            } else {
                intensiveDebugEnabled = true;
                filteredPlayer = null;
                sender.sendMessage(ChatColor.GREEN + "ShieldSync debug enabled for all players.");
            }
            return true;
        }

        final Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        final UUID targetUuid = target.getUniqueId();
        if (intensiveDebugEnabled && targetUuid.equals(filteredPlayer)) {
            intensiveDebugEnabled = false;
            filteredPlayer = null;
            sender.sendMessage(ChatColor.YELLOW + "ShieldSync debug disabled.");
            return true;
        }

        intensiveDebugEnabled = true;
        filteredPlayer = targetUuid;
        sender.sendMessage(ChatColor.GREEN + "ShieldSync debug enabled for " + target.getName() + " only.");
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        final List<String> completions = new ArrayList<>();
        if (!command.getName().equalsIgnoreCase("shieldsync")) {
            return completions;
        }

        if (args.length == 1) {
            final String prefix = args[0].toLowerCase();
            if ("debug".startsWith(prefix)) {
                completions.add("debug");
            }
            if ("enable".startsWith(prefix)) {
                completions.add("enable");
            }
            if ("disable".startsWith(prefix)) {
                completions.add("disable");
            }
            if ("status".startsWith(prefix)) {
                completions.add("status");
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            final String prefix = args[1].toLowerCase();
            for (final Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(online.getName());
                }
            }
            return completions;
        }

        return completions;
    }

    public static boolean isPluginEnabled() {
        return pluginEnabled;
    }


    public static void debugChat(final UUID subjectUuid, final String message) {
        if (!intensiveDebugEnabled) {
            return;
        }
        if (subjectUuid != null && filteredPlayer != null && !filteredPlayer.equals(subjectUuid)) {
            return;
        }

        final ShieldSync plugin = instance;
        if (plugin == null) {
            return;
        }

        final Runnable send = () -> {
            DEBUG_VIEWERS.removeIf(uuid -> {
                final Player viewer = Bukkit.getPlayer(uuid);
                if (viewer == null || !viewer.isOnline()) {
                    return true;
                }
                final String subjectName;
                if (subjectUuid == null) {
                    subjectName = "global";
                } else {
                    final Player subject = Bukkit.getPlayer(subjectUuid);
                    subjectName = subject != null ? subject.getName() : subjectUuid.toString();
                }
                viewer.sendMessage("[ShieldSync Debug] [" + subjectName + "] " + message);
                return false;
            });
        };

        if (Bukkit.isPrimaryThread()) {
            send.run();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, send);
    }

    public static void debugChat(final Player player, final String message) {
        debugChat(player != null ? player.getUniqueId() : null, message);
    }
}
