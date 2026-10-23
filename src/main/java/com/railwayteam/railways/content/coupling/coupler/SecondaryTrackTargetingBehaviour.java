/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2025 The Railways Team
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

import net.minecraft.core.UUIDUtil;

import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import com.railwayteam.railways.mixin.AccessorTrackTargetingBehavior;
import com.zurrtum.create.content.trains.graph.EdgePointType;
import com.zurrtum.create.content.trains.signal.TrackEdgePoint;
import com.zurrtum.create.infrastructure.component.BezierTrackPointLocation;
import com.zurrtum.create.content.trains.track.TrackTargetingBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;
import com.zurrtum.create.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public class SecondaryTrackTargetingBehaviour<T extends TrackEdgePoint> extends TrackTargetingBehaviour<T> {

    public static final BehaviourType<SecondaryTrackTargetingBehaviour<?>> TYPE = new BehaviourType<>();

    public SecondaryTrackTargetingBehaviour(SmartBlockEntity te, EdgePointType<T> edgePointType) {
        super(te, edgePointType);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void write(ValueOutput nbt, boolean clientPacket) {
        AccessorTrackTargetingBehavior accessor = (AccessorTrackTargetingBehavior) this;
        nbt.store("SecondaryId", UUIDUtil.CODEC, accessor.getId());
        nbt.put("SecondaryTargetTrack", NbtUtils.writeBlockPos(accessor.getTargetTrack()));
        nbt.putBoolean("SecondaryOrtho", accessor.isOrthogonal());
        nbt.putBoolean("SecondaryTargetDirection", accessor.getTargetDirection() == Direction.AxisDirection.POSITIVE);
        if (accessor.getRotatedDirection() != null)
            nbt.put("SecondaryRotatedAxis", VecHelper.writeNBT(accessor.getRotatedDirection()));
        if (accessor.getPrevDirection() != null)
            nbt.put("SecondaryPrevAxis", VecHelper.writeNBT(accessor.getPrevDirection()));
        if (accessor.getMigrationData() != null && !clientPacket)
            nbt.put("SecondaryMigrate", accessor.getMigrationData());
        if (accessor.getTargetBezier() != null) {
            CompoundTag bezierNbt = new CompoundTag();
            bezierNbt.putInt("Segment", accessor.getTargetBezier().segment());
            bezierNbt.put("Key", NbtUtils.writeBlockPos(accessor.getTargetBezier().curveTarget()
                .subtract(getPos())));
            nbt.put("SecondaryBezier", bezierNbt);
        }
    }

    @Override
    public void read(ValueInput nbt, boolean clientPacket) {
        AccessorTrackTargetingBehavior accessor = (AccessorTrackTargetingBehavior) this;
        accessor.setId(nbt.contains("SecondaryId") ? nbt.read("SecondaryId", UUIDUtil.CODEC).orElseThrow() : UUID.randomUUID());
        accessor.setTargetTrack(NbtUtils.readBlockPos(nbt.childOrEmpty("SecondaryTargetTrack")));
        accessor.setTargetDirection(nbt.getBooleanOr("SecondaryTargetDirection", false) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        accessor.setOrthogonal(nbt.getBooleanOr("SecondaryOrtho", false));
        if (nbt.contains("SecondaryPrevAxis"))
            accessor.setPrevDirection(VecHelper.readNBT(nbt.getList("SecondaryPrevAxis", Tag.TAG_DOUBLE)));
        if (nbt.contains("SecondaryRotatedAxis"))
            accessor.setRotatedDirection(VecHelper.readNBT(nbt.getList("SecondaryRotatedAxis", Tag.TAG_DOUBLE)));
        if (nbt.contains("SecondaryMigrate"))
            accessor.setMigrationData(nbt.childOrEmpty("SecondaryMigrate"));
        if (clientPacket)
            accessor.setEdgePoint(null);
        if (nbt.contains("SecondaryBezier")) {
            CompoundTag bezierNbt = nbt.childOrEmpty("SecondaryBezier");
            BlockPos key = NbtUtils.readBlockPos(bezierNbt.getCompound("Key"));
            accessor.setTargetBezier(new BezierTrackPointLocation(bezierNbt.contains("FromStack") ? key : key.offset(getPos()),
                bezierNbt.getInt("Segment")));
        }
    }
}
