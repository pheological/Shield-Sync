package pheological.shieldSync;

import com.github.retrooper.packetevents.protocol.player.User;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class PlayerDataManager {
    private static final ConcurrentHashMap<UUID, PlayerData> DATA = new ConcurrentHashMap<>();

    private PlayerDataManager() {}

    public static void add(final User user, final Player player) {
        if (user == null || player == null) {
            return;
        }
        DATA.put(player.getUniqueId(), new PlayerData(user, player));
    }

    public static void remove(final UUID uuid) {
        if (uuid == null) {
            return;
        }
        DATA.remove(uuid);
    }

    public static PlayerData get(final UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return DATA.get(uuid);
    }

    public static void clear() {
        DATA.clear();
    }
}

