/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2026 The Railways Team
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

import com.railwayteam.railways.port.create.track.TrackMaterialData;

import com.railwayteam.railways.port.create.track.TrackType;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.mixin_interfaces.IExtraRenderParts;
import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import com.zurrtum.create.client.content.trains.track.TrackRenderer;
import com.zurrtum.create.client.content.trains.track.TrackRenderer.TrackRenderState;
import com.zurrtum.create.content.trains.track.TrackShape;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.railwayteam.railways.content.custom_tracks.casing.CasingRenderUtils.extractBezierCasings;
import static com.railwayteam.railways.content.custom_tracks.casing.CasingRenderUtils.reTexture;

/**
 * Track casings: the block's own casing and casings along its curves.
 *
 * <p>PORT-NOTE(26.1): Create Fly's {@code TrackRenderer} extracts render state (and returns early, without a block
 * position, when there are no curves), so casing parts are extracted at every return into {@link IExtraRenderParts}
 * and submitted after the track. 1.20's "No casing for shape" debug text is not drawn.</p>
 */
@Mixin(value = TrackRenderer.class, remap = false)
public class MixinTrackRenderer {
    @Inject(method = "extractRenderState(Lcom/zurrtum/create/content/trains/track/TrackBlockEntity;Lcom/zurrtum/create/client/content/trains/track/TrackRenderer$TrackRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("RETURN"))
    private void extractCasings(TrackBlockEntity te, TrackRenderState state, float partialTicks, Vec3 cameraPos,
                                @Nullable CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        List<SuperByteBufferRenderState> parts = ((IExtraRenderParts) state).railways$getExtraParts();
        parts.clear();
        Level level = te.getLevel();
        CardinalLighting cardinalLighting = SmartBlockEntityRenderer.getCardinalLighting(level);

        Block casingBlock = ((IHasTrackCasing) te).railways$getTrackCasing();
        if (casingBlock != null) {
            TrackShape shape = te.getBlockState().getValue(TrackBlock.SHAPE);
            if (CRBlockPartials.TRACK_CASINGS.containsKey(shape)) {
                PoseStack ms = new PoseStack();
                if (te.isTilted()) {
                    double angle = te.tilt.smoothingAngle.get();
                    switch (te.getBlockState().getValue(TrackBlock.SHAPE)) {
                        case ZO -> TransformStack.of(ms)
                            .rotateXDegrees((float) -angle);
                        case XO -> TransformStack.of(ms)
                            .rotateZDegrees((float) angle);
                    }
                }

                TrackType trackType = null;
                if (te.getBlockState().getBlock() instanceof TrackBlock trackBlock)
                    trackType = TrackMaterialData.of(trackBlock.getMaterial()).trackType;

                CRBlockPartials.TrackCasingSpec spec = CRBlockPartials.TRACK_CASINGS.get(shape);
                if (((IHasTrackCasing) te).railways$isAlternate())
                    spec = spec.getNonNullAltSpec(trackType);
                else
                    spec = spec.getFor(trackType);
                CRBlockPartials.ModelTransform transform = spec.transform;

                PartialModel texturedPartial = reTexture(spec.model, casingBlock);
                int light = SmartBlockEntityRenderer.getLightCoords(level, te.getBlockPos());

                parts.add(CachedBuffers.partial(texturedPartial, casingBlock.defaultBlockState())
                    .transform(ms.last())
                    .rotateX(transform.rx()).rotateY(transform.ry()).rotateZ(transform.rz())
                    .translate(transform.x(), transform.y(), transform.z())
                    .cardinalLighting(cardinalLighting)
                    .light(light)
                    .extractRenderState());

                for (CRBlockPartials.ModelTransform additionalTransform : spec.additionalTransforms) {
                    parts.add(CachedBuffers.partial(texturedPartial, casingBlock.defaultBlockState())
                        .transform(ms.last())
                        .rotateX(additionalTransform.rx()).rotateY(additionalTransform.ry()).rotateZ(additionalTransform.rz())
                        .translate(additionalTransform.x(), additionalTransform.y(), additionalTransform.z())
                        .cardinalLighting(cardinalLighting)
                        .light(light)
                        .extractRenderState());
                }
            }
        }

        for (BezierConnection bc : te.getConnections().values()) {
            if (!bc.isPrimary())
                continue;
            Block curveCasing = ((IHasTrackCasing) bc).railways$getTrackCasing();
            if (curveCasing != null) {
                extractBezierCasings(level, reTexture(CRBlockPartials.TRACK_CASING_FLAT_THICK, curveCasing),
                    curveCasing.defaultBlockState(), bc, cardinalLighting, parts);
            }
        }

        if (!parts.isEmpty() && state.blockPos == null) {
            state.blockPos = te.getBlockPos();
            state.blockEntityType = te.getType();
        }
    }

    @Inject(method = "submit(Lcom/zurrtum/create/client/content/trains/track/TrackRenderer$TrackRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("TAIL"))
    private void submitCasings(TrackRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState cameraState,
                               CallbackInfo ci) {
        for (SuperByteBufferRenderState part : ((IExtraRenderParts) state).railways$getExtraParts())
            part.submit(ms, queue);
    }
}
