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

package com.railwayteam.railways.content.conductor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.catnip.data.Iterate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class ConductorToolboxLayer extends RenderLayer<ConductorRenderState, ConductorEntityModel> {
  public ConductorToolboxLayer(RenderLayerParent<ConductorRenderState, ConductorEntityModel> pRenderer) {
    super(pRenderer);
  }

  @Override
  public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector queue, int packedLight,
                     @NotNull ConductorRenderState state, float yRot, float xRot) {
    if (!state.carryingToolbox)
      return;
    BlockState blockState = state.toolboxBlockState;
    SuperByteBuffer body =
        CachedBuffers.partial(CRBlockPartials.TOOLBOX_BODIES.get(state.toolboxColor), blockState);
    SuperByteBuffer lid =
        CachedBuffers.partial(AllPartialModels.TOOLBOX_LIDS.get(state.toolboxColor), blockState);
    SuperByteBuffer drawer = CachedBuffers.partial(AllPartialModels.TOOLBOX_DRAWER, blockState);

    float lidAngle = state.toolboxLidAngle;
    float drawerOffset = state.toolboxDrawerOffset;

    poseStack.pushPose();
    poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
    poseStack.translate(-0.5d, -1.2d, -0.94d);
    double rotate = 0;
    body.center()
        .rotateY((float) rotate)
        .uncenter()
        .translate(0, 6 / 16f, 12 / 16f)
        .translate(0, -6 / 16f, -12 / 16f)
        .light(packedLight)
        .submit(poseStack, queue);

    lid.center()
        .rotateY((float) rotate)
        .uncenter()
        .translate(0, 6 / 16f, 12 / 16f)
        .rotateXDegrees(60 * lidAngle)
        .translate(0, -6 / 16f, -12 / 16f)
        .light(packedLight)
        .submit(poseStack, queue);

    for (int offset : Iterate.zeroAndOne) {
      drawer.center()
          .rotateY((float) rotate)
          .uncenter()
          .translate(0, offset / 8f, -drawerOffset * .175f * (2 - offset))
          .light(packedLight)
          .submit(poseStack, queue);
    }
    poseStack.popPose();
  }
}
