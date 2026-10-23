/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.railwayteam.railways.content.coupling.coupler;

import com.railwayteam.railways.mixin_interfaces.IHandcarTrain;
import com.zurrtum.create.Create;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.graph.DimensionPalette;
import com.zurrtum.create.content.trains.graph.TrackEdge;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.signal.SignalPropagator;
import com.zurrtum.create.content.trains.signal.SingleBlockEntityEdgePoint;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class TrackCoupler extends SingleBlockEntityEdgePoint {
    private int activated;
    private UUID currentTrain;

    public TrackCoupler() {
        activated = 0;
        currentTrain = null;
    }

    @Override
    public void tick(MinecraftServer server, TrackGraph graph, boolean preTrains) {
        super.tick(server, graph, preTrains);
        if (isActivated())
            activated--;
        if (!isActivated())
            currentTrain = null;
    }

    public UUID getCurrentTrain() {
        return currentTrain;
    }

    public boolean isActivated() {
        return activated > 0;
    }

    public void keepAlive(Train train) {
        if (((IHandcarTrain) train).railways$isHandcar()) return;
        activated = 8;
        currentTrain = train.id;
    }

    @Override
    public void blockEntityAdded(BlockEntity tile, boolean front) {
        super.blockEntityAdded(tile, front);
        notifyTrains(tile.getLevel());
    }

    private void notifyTrains(Level level) {
        TrackGraph graph = Create.RAILWAYS.sided(level)
            .getGraph(edgeLocation.getFirst());
        if (graph == null)
            return;
        TrackEdge edge = graph.getConnection(edgeLocation.map(graph::locateNode));
        if (edge == null)
            return;
        SignalPropagator.notifyTrains(graph, edge);
    }

    @Override
    public void read(ValueInput view, boolean migration, DimensionPalette dimensions) {
        super.read(view, migration, dimensions);
        activated = view.getIntOr("Activated", 0);
        currentTrain = view.read("TrainId", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public <T> void decode(DynamicOps<T> ops, T input, boolean migration, DimensionPalette dimensions) {
        super.decode(ops, input, migration, dimensions);
        MapLike<T> map = ops.getMap(input).getOrThrow();
        T activatedValue = map.get("Activated");
        activated = activatedValue == null ? 0 : ops.getNumberValue(activatedValue, 0).intValue();
        currentTrain = map.get("TrainId") == null ? null : UUIDUtil.CODEC.parse(ops, map.get("TrainId")).result().orElse(null);
    }

    @Override
    public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.read(buffer, dimensions);
        if (buffer.readBoolean())
            blockEntityPos = buffer.readBlockPos();
    }

    @Override
    public void write(ValueOutput view, DimensionPalette dimensions) {
        super.write(view, dimensions);
        view.putInt("Activated", activated);
        view.storeNullable("TrainId", UUIDUtil.CODEC, currentTrain);
    }

    @Override
    public <T> DataResult<T> encode(DynamicOps<T> ops, T empty, DimensionPalette dimensions) {
        DataResult<T> prefix = super.encode(ops, empty, dimensions);
        RecordBuilder<T> map = ops.mapBuilder();
        map.add("Activated", ops.createInt(activated));
        if (currentTrain != null)
            map.add("TrainId", currentTrain, UUIDUtil.CODEC);
        return map.build(prefix);
    }

    @Override
    public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.write(buffer, dimensions);
        buffer.writeBoolean(blockEntityPos != null);
        if (blockEntityPos != null)
            buffer.writeBlockPos(blockEntityPos);
    }
}
