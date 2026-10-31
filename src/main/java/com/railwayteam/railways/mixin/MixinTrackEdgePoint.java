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

package com.railwayteam.railways.mixin;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.TagValueOutput;
import com.railwayteam.railways.content.coupling.coupler.SecondaryTrackTargetingBehaviour;
import com.railwayteam.railways.registry.CREdgePointTypes;
import com.zurrtum.create.content.trains.graph.DimensionPalette;
import com.zurrtum.create.content.trains.graph.EdgePointType;
import com.zurrtum.create.content.trains.signal.TrackEdgePoint;
import com.zurrtum.create.content.trains.track.TrackTargetingBehaviour;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.trains.observer.TrackObserver;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TrackEdgePoint.class, remap = false)
public abstract class MixinTrackEdgePoint {
    @Shadow public abstract void write(ValueOutput view, DimensionPalette dimensions);

    @Shadow public abstract EdgePointType<?> getType();

    @Inject(method = "invalidateAt", at = @At("RETURN"))
    private void invalidateSecondaryEdgePoint(LevelAccessor level, BlockPos tilePos, CallbackInfo ci) {
        TrackTargetingBehaviour<?> behaviour = BlockEntityBehaviour.get(level, tilePos, SecondaryTrackTargetingBehaviour.TYPE);
        if (behaviour == null)
            return;
        // PORT-NOTE(26.1): as Create Fly's own invalidateAt
        TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        DimensionPalette dimensions = new DimensionPalette();
        write(view, dimensions);
        view.store("DimensionPalette", DimensionPalette.CODEC, dimensions);
        behaviour.invalidateEdgePoint(view.buildResult());
    }

    @Inject(method = "canCoexistWith", at = @At("RETURN"), cancellable = true)
    private void couplerCoExistWithEverything(EdgePointType<?> otherType, boolean front, CallbackInfoReturnable<Boolean> cir) {
        if (getType() == CREdgePointTypes.COUPLER || otherType == CREdgePointTypes.COUPLER
                || getType() == CREdgePointTypes.SWITCH || otherType == CREdgePointTypes.SWITCH)
            cir.setReturnValue(true);
    }

    // PORT-NOTE(26.1): TrackObserver no longer overrides the network read/write, so its block entity position is synced
    // from here (was MixinTrackObserver)
    @Inject(method = "write(Lnet/minecraft/network/FriendlyByteBuf;Lcom/zurrtum/create/content/trains/graph/DimensionPalette;)V", at = @At("RETURN"))
    private void writeObserverPositions(FriendlyByteBuf buffer, DimensionPalette dimensions, CallbackInfo ci) {
        if (!((Object) this instanceof TrackObserver observer))
            return;
        buffer.writeBoolean(observer.blockEntityPos != null);
        if (observer.blockEntityPos != null)
            buffer.writeBlockPos(observer.blockEntityPos);
    }

    @Inject(method = "read(Lnet/minecraft/network/FriendlyByteBuf;Lcom/zurrtum/create/content/trains/graph/DimensionPalette;)V", at = @At("RETURN"))
    private void readObserverPositions(FriendlyByteBuf buffer, DimensionPalette dimensions, CallbackInfo ci) {
        if ((Object) this instanceof TrackObserver observer && buffer.readBoolean())
            observer.blockEntityPos = buffer.readBlockPos();
    }
}
