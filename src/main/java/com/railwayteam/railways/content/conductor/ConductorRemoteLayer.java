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
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class ConductorRemoteLayer extends RenderLayer<ConductorRenderState, ConductorEntityModel> {
	public ConductorRemoteLayer(RenderLayerParent<ConductorRenderState, ConductorEntityModel> pRenderer) {
		super(pRenderer);
	}

	@Override
	public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector queue, int packedLight,
					   @NotNull ConductorRenderState state, float yRot, float xRot) {
		if (state.job == ConductorEntity.Job.REMOTE_CONTROL) {
			poseStack.pushPose();
			getParentModel().getHead().translateAndRotate(poseStack);
			CachedBuffers.partial(CRBlockPartials.CONDUCTOR_ANTENNA, Blocks.AIR.defaultBlockState())
					.rotateXDegrees(180)
					.translate(3 / 16.0, 3.5 / 16.0, 0 / 16.0)
					.rotateZDegrees(-30)
					.light(packedLight)
					.submit(poseStack, queue);
			poseStack.popPose();
		} else if (state.job == ConductorEntity.Job.SPY) {
			poseStack.pushPose();
			getParentModel().getHead().translateAndRotate(poseStack);
			CachedBuffers.partial(AllPartialModels.BLAZE_GOGGLES, Blocks.AIR.defaultBlockState())
					.rotateZDegrees(180)
					.translate(-8 / 16.0, 2 / 16.0, -8 / 16.0)
					.light(packedLight)
					.submit(poseStack, queue);
			poseStack.popPose();
		}
	}
}
