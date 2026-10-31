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

package com.railwayteam.railways.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing;
import com.railwayteam.railways.port.create.track.TrackMaterialData;
import com.railwayteam.railways.port.create.track.TrackType;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.client.content.trains.station.StationRenderer;
import com.zurrtum.create.client.content.trains.track.TrackBlockRenderState;
import com.zurrtum.create.client.content.trains.track.TrackBlockRenderer;
import com.zurrtum.create.content.trains.station.StationBlockEntity;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Raises the train assembly overlay onto encased tracks.
 *
 * <p>PORT-NOTE(26.1): Create Fly builds the overlay in {@code TrackBlockRenderer.getAssemblyRenderState} and shares the
 * piece render states between track positions, so the shift for the station's target track is applied to the whole
 * overlay (1.20 shifted each piece by its own track's casing).</p>
 */
@Mixin(StationRenderer.class)
public class MixinStationRenderer {
    @WrapOperation(method = "extractRenderState(Lcom/zurrtum/create/content/trains/station/StationBlockEntity;Lcom/zurrtum/create/client/content/trains/station/StationRenderer$StationRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/content/trains/track/TrackBlockRenderer;getAssemblyRenderState(Lcom/zurrtum/create/content/trains/station/StationBlockEntity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lcom/zurrtum/create/client/content/trains/track/TrackBlockRenderState;"))
    private TrackBlockRenderState shiftAssemblyOverlayOnEncasedTracks(TrackBlockRenderer renderer, StationBlockEntity be, Vec3 offset,
                                                                      Level level, BlockPos pos, BlockState trackState,
                                                                      Operation<TrackBlockRenderState> original) {
        if (trackState.getBlock() instanceof TrackBlock trackBlock
            && level.getBlockEntity(pos) instanceof IHasTrackCasing casing && casing.railways$getTrackCasing() != null) {
            TrackShape shape = trackState.getValue(TrackBlock.SHAPE);
            CRBlockPartials.TrackCasingSpec spec = CRBlockPartials.TRACK_CASINGS.get(shape);
            TrackType trackType = TrackMaterialData.of(trackBlock.getMaterial()).trackType;
            if (spec != null)
                offset = offset.add(
                    spec.getXShift(trackType),
                    (spec.getTopSurfacePixelHeight(trackType, casing.railways$isAlternate()) - 2) / 16f,
                    spec.getZShift(trackType)
                );
        }
        return original.call(renderer, be, offset, level, pos, trackState);
    }
}
