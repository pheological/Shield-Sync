package pheological.shieldSync;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import java.util.UUID;

public final class PingReceiveListener extends PacketListenerAbstract {
    public PingReceiveListener() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!ShieldSync.isPluginEnabled()) {
            return;
        }
        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE) {
            return;
        }

        final WrapperPlayClientKeepAlive wrapper = new WrapperPlayClientKeepAlive(event);
        if (wrapper.getId() != ShieldSync.PROBE_ID) {
            return;
        }

        event.setCancelled(true);

        final UUID uuid = event.getUser().getUUID();
        final PlayerData data = PlayerDataManager.get(uuid);
        if (data == null) {
            return;
        }

        final long[] probe = data.getPendingProbes().poll();
        if (probe == null || probe.length < 2) {
            ShieldSync.debugChat(data.getPlayer(), "Probe pong received but pending queue was empty.");
            return;
        }

        final long sampleMs = Math.max(0L, (System.nanoTime() - probe[1]) / 1_000_000L);
        data.recordPong(probe[1]);

        ShieldSync.debugChat(
                data.getPlayer(),
                "Probe pong sample=" + sampleMs + "ms, stablePing=" + data.getStablePing()
                        + "ms, compensation=" + data.getCompensationTicks() + " ticks"
        );
    }
}
