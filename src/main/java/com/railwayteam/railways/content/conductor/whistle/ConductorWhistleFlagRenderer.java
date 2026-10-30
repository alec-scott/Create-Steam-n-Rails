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

package com.railwayteam.railways.content.conductor.whistle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.railwayteam.railways.util.CustomTrackOverlayRendering;
import com.railwayteam.railways.util.CustomTrackOverlayRendering.OffsetOverlay;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.content.trains.station.GlobalStation;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackTargetingBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer.SmartRenderState;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class ConductorWhistleFlagRenderer extends SmartBlockEntityRenderer<ConductorWhistleFlagBlockEntity, ConductorWhistleFlagRenderer.WhistleFlagRenderState> {
    public ConductorWhistleFlagRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public WhistleFlagRenderState createRenderState() {
        return new WhistleFlagRenderState();
    }

    @Override
    public void extractRenderState(ConductorWhistleFlagBlockEntity te, WhistleFlagRenderState state, float partialTicks, Vec3 cameraPos,
                                   @Nullable CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(te, state, partialTicks, cameraPos, crumblingOverlay);
        state.overlay = extractEdgePoint(te);

        state.flag = CachedBuffers.partial(CRBlockPartials.CONDUCTOR_WHISTLE_FLAGS.get(te.getColor()), Blocks.AIR.defaultBlockState())
            .extractRenderState();
    }

    @Override
    public void submit(WhistleFlagRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);
        if (state.overlay != null)
            state.overlay.submit(ms, queue);
        state.flag.submit(ms, queue);
    }

    private @Nullable OffsetOverlay extractEdgePoint(ConductorWhistleFlagBlockEntity te) {
        TrackTargetingBehaviour<GlobalStation> target = te.station;
        BlockPos pos = te.getBlockPos();

        BlockPos targetPosition = target.getGlobalPosition();
        Level level = te.getLevel();
        BlockState trackState = level.getBlockState(targetPosition);
        Block block = trackState.getBlock();

        if (!(block instanceof ITrackBlock))
            return null;

        SuperByteBufferRenderState overlay = CustomTrackOverlayRendering.extractOverlay(level, targetPosition, target.getTargetDirection(),
            target.getTargetBezier(), AllPartialModels.TRACK_STATION_OVERLAY, 1, false);
        return overlay == null ? null : new OffsetOverlay(BlockPos.ZERO.subtract(pos), overlay);
    }

    public static class WhistleFlagRenderState extends SmartRenderState {
        public @Nullable OffsetOverlay overlay;
        public @UnknownNullability SuperByteBufferRenderState flag;
    }
}
