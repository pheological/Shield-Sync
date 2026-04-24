package pheological.shieldSync;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import java.util.UUID;

public final class PingSendListener extends PacketListenerAbstract {
    public PingSendListener() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (!ShieldSync.isPluginEnabled()) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacketType() != PacketType.Play.Server.KEEP_ALIVE) {
            return;
        }

        final WrapperPlayServerKeepAlive wrapper = new WrapperPlayServerKeepAlive(event);
        if (wrapper.getId() != ShieldSync.PROBE_ID) {
            return;
        }

        final UUID uuid = event.getUser().getUUID();
        final PlayerData data = PlayerDataManager.get(uuid);
        if (data == null) {
            return;
        }

        data.getPendingProbes().add(new long[] {wrapper.getId(), System.nanoTime()});
        ShieldSync.debugChat(data.getPlayer(), "Probe sent; pendingQueue=" + data.getPendingProbes().size());
    }
}
