package pheological.shieldSync;

import com.github.retrooper.packetevents.protocol.player.User;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.entity.Player;

public final class PlayerData {
    private final Player player;
    private final User user;
    private final ConcurrentLinkedQueue<long[]> pendingProbes = new ConcurrentLinkedQueue<>();

    private volatile long ping;
    private volatile long previousPing;

    public PlayerData(final User user, final Player player) {
        this.user = user;
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public User getUser() {
        return this.user;
    }

    public ConcurrentLinkedQueue<long[]> getPendingProbes() {
        return this.pendingProbes;
    }

    public void recordPong(final long sendNanos) {
        final long roundTripNanos = System.nanoTime() - sendNanos;
        final long sampleMs = Math.max(0L, roundTripNanos / 1_000_000L);

        final long currentPing = this.ping;
        this.previousPing = currentPing;

        if (currentPing > 0L && sampleMs > currentPing + 80L) {
            this.ping = currentPing;
            return;
        }

        this.ping = sampleMs;
    }

    public long getStablePing() {
        final long sampledPing = this.ping;
        return sampledPing > 0L ? sampledPing : Math.max(0L, this.player.getPing());
    }

    public int getCompensationTicks() {
        return calculateCompensationTicksFromPing(getStablePing());
    }

    public static int calculateCompensationTicksFromPing(final long pingMs) {
        final long normalizedPing = Math.max(0L, pingMs);
        final int compensationTicks = (int) (normalizedPing / 50L);
        return Math.max(0, Math.min(5, compensationTicks));
    }
}

