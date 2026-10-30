package com.railwayteam.railways.util.packet;

import com.railwayteam.railways.mixin_interfaces.ILimited;
import com.railwayteam.railways.multiloader.C2SPacket;
import com.zurrtum.create.Create;
import com.zurrtum.create.content.trains.graph.TrackGraphLocation;
import com.zurrtum.create.content.trains.station.GlobalStation;
import com.zurrtum.create.content.trains.station.StationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Toggles a station's train limit.
 *
 * <p>PORT-NOTE(26.1): 1.20 piggybacked a field on Create's {@code StationEditPacket}
 * ({@code MixinStationEditPacket}); Create Fly's is a record with a fixed codec, so this is Railways' own packet,
 * with the same checks as Create's block entity configuration packets.</p>
 */
public class StationLimitPacket implements C2SPacket {
    final BlockPos pos;
    final boolean limitEnabled;

    public StationLimitPacket(BlockPos pos, boolean limitEnabled) {
        this.pos = pos;
        this.limitEnabled = limitEnabled;
    }

    public StationLimitPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        limitEnabled = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(limitEnabled);
    }

    @Override
    public void handle(ServerPlayer sender) {
        if (sender.isSpectator() || !sender.mayBuild())
            return;
        ServerLevel level = sender.level();
        if (!level.isLoaded(pos) || !pos.closerThan(sender.blockPosition(), 20))
            return;
        if (!(level.getBlockEntity(pos) instanceof StationBlockEntity te))
            return;
        GlobalStation station = te.getStation();
        TrackGraphLocation graphLocation = te.edgePoint.determineGraphLocation();
        if (station != null && graphLocation != null) {
            ((ILimited) station).setLimitEnabled(limitEnabled);
            Create.RAILWAYS.sync.pointAdded(graphLocation.graph, station);
            Create.RAILWAYS.markTracksDirty();
        }
    }
}
