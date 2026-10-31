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
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class ConductorFlagLayer extends RenderLayer<ConductorRenderState, ConductorEntityModel> {
	public ConductorFlagLayer(RenderLayerParent<ConductorRenderState, ConductorEntityModel> pRenderer) {
		super(pRenderer);
	}

	@Override
	public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector queue, int packedLight,
					   @NotNull ConductorRenderState state, float yRot, float xRot) {
		if (state.holdingSchedules) {
			poseStack.pushPose();
			CachedBuffers.partial(CRBlockPartials.CONDUCTOR_WHISTLE_FLAGS.get(state.color), Blocks.AIR.defaultBlockState())
					.translate(-0.78125, 0.15, -0.688)
					.light(packedLight)
					.submit(poseStack, queue);
			poseStack.popPose();
		}
	}
}
