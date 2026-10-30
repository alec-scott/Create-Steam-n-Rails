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

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.railwayteam.railways.util.CustomTrackOverlayRendering;
import com.railwayteam.railways.util.CustomTrackOverlayRendering.OffsetOverlay;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackTargetingBehaviour;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer.SmartRenderState;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TrackCouplerRenderer extends SmartBlockEntityRenderer<TrackCouplerBlockEntity, TrackCouplerRenderer.TrackCouplerRenderState> {

    public TrackCouplerRenderer(Context context) {
        super(context);
    }

    @Nullable
    public static PartialModel getCouplerOverlayModel(TrackCouplerBlockEntity te) {
        if (te.areEdgePointsOk()) {
            TrackCouplerBlockEntity.AllowedOperationMode mode = te.getAllowedOperationMode();
            if (mode.canCouple && mode.canDecouple) return CRBlockPartials.COUPLER_BOTH;
            if (mode.canCouple) return CRBlockPartials.COUPLER_COUPLE;
            if (mode.canDecouple) return CRBlockPartials.COUPLER_DECOUPLE;
        } else {
            return CRBlockPartials.COUPLER_NONE;
        }
        return null;
    }

    @Override
    public TrackCouplerRenderState createRenderState() {
        return new TrackCouplerRenderState();
    }

    @Override
    public void extractRenderState(TrackCouplerBlockEntity te, TrackCouplerRenderState state, float partialTicks, Vec3 cameraPos,
                                   @Nullable CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(te, state, partialTicks, cameraPos, crumblingOverlay);
        state.overlays.clear();
        extractEdgePoint(te, state, te.edgePoint);
        extractEdgePoint(te, state, te.secondEdgePoint);
    }

    @Override
    public void submit(TrackCouplerRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);
        for (OffsetOverlay overlay : state.overlays)
            overlay.submit(ms, queue);
    }

    private void extractEdgePoint(TrackCouplerBlockEntity te, TrackCouplerRenderState state, TrackTargetingBehaviour<TrackCoupler> target) {
        BlockPos pos = te.getBlockPos();
        boolean offsetToSide = CustomTrackOverlayRendering.overlayWillOverlap(target);

        BlockPos targetPosition = target.getGlobalPosition();
        Level level = te.getLevel();
        BlockState trackState = level.getBlockState(targetPosition);
        Block block = trackState.getBlock();

        if (!(block instanceof ITrackBlock))
            return;

        PartialModel model = getCouplerOverlayModel(te);
        if (model == null)
            return;
        SuperByteBufferRenderState overlay = CustomTrackOverlayRendering.extractOverlay(level, targetPosition,
            target.getTargetDirection(), target.getTargetBezier(), model, 1, offsetToSide);
        if (overlay != null)
            state.overlays.add(new OffsetOverlay(targetPosition.subtract(pos), overlay));
    }

    public static class TrackCouplerRenderState extends SmartRenderState {
        public final List<OffsetOverlay> overlays = new ArrayList<>();
    }
}
