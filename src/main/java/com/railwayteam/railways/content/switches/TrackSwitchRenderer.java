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

package com.railwayteam.railways.content.switches;

import com.railwayteam.railways.content.switches.client.TrackSwitchClientBehaviours;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.content.switches.TrackSwitchBlock.SwitchState;
import com.railwayteam.railways.content.switches.TrackSwitchBlockEntity.PonderData;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.railwayteam.railways.util.CustomTrackOverlayRendering;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackTargetingBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer.SmartRenderState;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.ponder.api.level.PonderLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// PORT-NOTE(26.1): render-state renderer; flag parts and the track overlay are extracted, then submitted.
public class TrackSwitchRenderer extends SmartBlockEntityRenderer<TrackSwitchBlockEntity, TrackSwitchRenderer.TrackSwitchRenderState> {
  public TrackSwitchRenderer(Context ctx) {
    super(ctx);
  }

  @Override
  public TrackSwitchRenderState createRenderState() {
    return new TrackSwitchRenderState();
  }

  @Override
  public void extractRenderState(TrackSwitchBlockEntity te, TrackSwitchRenderState state, float partialTicks, Vec3 cameraPos,
                                 @Nullable CrumblingOverlay crumblingOverlay) {
    super.extractRenderState(te, state, partialTicks, cameraPos, crumblingOverlay);
    extractFlagState(te, state, partialTicks);
    extractTrackOverlay(te, state, te.edgePoint);

    if (te.ponderData != null && te.getLevel() instanceof PonderLevel ponderWorld) {
      renderPonderData(ponderWorld, te.getState(), te.ponderData);
    }
  }

  @Override
  public void submit(TrackSwitchRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState cameraState) {
    super.submit(state, ms, queue, cameraState);

    ms.pushPose();
    TransformStack.of(ms)
      .center()
      .rotateYDegrees(state.yRot)
      .uncenter();
    if (state.automatic)
      ms.translate(0, -2.0 / 16, 0);
    for (SuperByteBufferRenderState part : state.flagParts)
      part.submit(ms, queue);
    ms.popPose();

    if (state.overlay != null) {
      ms.pushPose();
      ms.translate(state.overlayOffset.getX(), state.overlayOffset.getY(), state.overlayOffset.getZ());
      state.overlay.submit(ms, queue);
      ms.popPose();
    }
  }

  // Not rendering: the ponder outliner draws these lines.
  private void renderPonderData(PonderLevel ponderWorld, SwitchState state, PonderData ponderData) {
    Vec3 offset = new Vec3(0, 0.4, 0);
    float width = 1 / 16f;

    Vec3 from = ponderData.basePos();
    for (Map.Entry<SwitchState, Vec3> to : ponderData.getBranches().entrySet()) {
      boolean active = to.getKey() == state;
      ponderWorld.scene.getOutliner().showLine(to,
                      from.add(offset),
                      to.getValue().add(offset))
              .colored(active ? new Color(0, 203, 150) : new Color(255, 50, 150))
              .lineWidth(width);
    }
  }

  private void extractFlagState(TrackSwitchBlockEntity te, TrackSwitchRenderState renderState, float partialTicks) {
    BlockState state = te.getBlockState();
    CardinalLighting cardinalLighting = getCardinalLighting(te.getLevel());
    int light = renderState.lightCoords;
    renderState.flagParts.clear();

    renderState.yRot = AngleHelper.horizontalAngle(state.getValue(TrackSwitchBlock.FACING));
    renderState.automatic = te.isAutomatic();

    SuperByteBuffer buf;
    if (te.isAutomatic()) {
      buf = CachedBuffers.partial(CRBlockPartials.BRASS_SWITCH_FLAG, state)
        .cardinalLighting(cardinalLighting)
        .light(light)
        .rotateCentered(1.5708f, Direction.UP)
        .translate(0.5, 8.5 / 16, 0.5);

      // Rotate just enough to touch the front or back edge
      if (te.isReverseLeft() || (te.isNormal() && te.exitCount == 2 && te.hasExit(SwitchState.REVERSE_RIGHT))) {
        te.lerpedAngle.updateChaseTarget(-0.40f);
      } else if (te.isReverseRight() || (te.isNormal() && te.exitCount == 2 && te.hasExit(SwitchState.REVERSE_LEFT))) {
        te.lerpedAngle.updateChaseTarget(0.40f);
      } else {
        te.lerpedAngle.updateChaseTarget(0.0f);
      }
      buf = buf.rotate(te.lerpedAngle.getValue(partialTicks), Direction.NORTH);

      renderState.flagParts.add(buf
        .translate(-0.5, -7.5 / 16, -0.5)
        .extractRenderState());
    } else {
      buf = CachedBuffers.partial(CRBlockPartials.ANDESITE_SWITCH_FLAG, state)
        .cardinalLighting(cardinalLighting)
        .light(light);

      if (te.isReverseLeft()) {
//        buf = buf.rotateCentered(Direction.UP, 1.5708f);
        te.lerpedAngle.updateChaseTarget(1.5708f);
      } else if (te.isReverseRight()) {
//        buf = buf.rotateCentered(Direction.UP, -1.5708f);  // 90°
        te.lerpedAngle.updateChaseTarget(-1.5708f);
      } else {
        te.lerpedAngle.updateChaseTarget(0.0f);
      }
      buf = buf.rotateCentered(te.lerpedAngle.getValue(partialTicks), Direction.UP);
      renderState.flagParts.add(buf.extractRenderState());

      renderState.flagParts.add(CachedBuffers.partial(CRBlockPartials.ANDESITE_SWITCH_HANDLE, state)
        .cardinalLighting(cardinalLighting)
        .light(light)
        .rotateCentered(-1.5708f, Direction.UP)  // 90°
        .extractRenderState());
    }
  }

  private void extractTrackOverlay(TrackSwitchBlockEntity te, TrackSwitchRenderState renderState,
                                   TrackTargetingBehaviour<TrackSwitch> target) {
    renderState.overlay = null;
    BlockPos pos = te.getBlockPos();
    boolean offsetToSide = CustomTrackOverlayRendering.overlayWillOverlap(target);

    BlockPos targetPosition = target.getGlobalPosition();
    Level level = te.getLevel();
    BlockState trackState = level.getBlockState(targetPosition);
    Block block = trackState.getBlock();

    if (!(block instanceof ITrackBlock))
      return;

    renderState.overlayOffset = targetPosition.subtract(pos);
    renderState.overlay = CustomTrackOverlayRendering.extractOverlay(level, targetPosition, target.getTargetDirection(),
            target.getTargetBezier(), TrackSwitchClientBehaviours.overlayModel(te), 1, offsetToSide);
  }

  public static class TrackSwitchRenderState extends SmartRenderState {
    public float yRot;
    public boolean automatic;
    public final List<SuperByteBufferRenderState> flagParts = new ArrayList<>();
    public @Nullable SuperByteBufferRenderState overlay;
    public BlockPos overlayOffset = BlockPos.ZERO;
  }
}
