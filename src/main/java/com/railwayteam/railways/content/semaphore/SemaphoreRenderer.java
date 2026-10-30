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

package com.railwayteam.railways.content.semaphore;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.client.foundation.render.CreateRenderTypes;
import com.zurrtum.create.content.redstone.nixieTube.NixieTubeBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

// PORT-NOTE(26.1): render-state renderer; lamps are drawn like Create Fly's nixie tube signals.
public class SemaphoreRenderer implements BlockEntityRenderer<SemaphoreBlockEntity, SemaphoreRenderer.SemaphoreRenderState> {
	public SemaphoreRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public SemaphoreRenderState createRenderState() {
		return new SemaphoreRenderState();
	}

	@Override
	public void extractRenderState(SemaphoreBlockEntity te, SemaphoreRenderState state, float partialTicks, Vec3 cameraPos,
								   @Nullable CrumblingOverlay crumblingOverlay) {
		Level level = SmartBlockEntityRenderer.extractBase(te, state, crumblingOverlay);
		CardinalLighting cardinalLighting = SmartBlockEntityRenderer.getCardinalLighting(level);
		BlockState blockState = te.getBlockState();

		state.yRot = AngleHelper.horizontalAngle(blockState.getValue(NixieTubeBlock.FACING)) + 180;

		boolean yellow = te.isDistantSignal;

		float pos = te.armPosition.getValue(partialTicks);

		float target = te.armPosition.getChaseTarget();

		pos = (2 * pos - 1) * (target - 0.5f) + 0.5f;//flips pos between 0 and 1 depending on target

		float fallTime = 0.3f;
		if (pos < fallTime)
			pos = 1f - pos * pos / (fallTime * fallTime);
		else {

			pos = (pos - fallTime) / (1f - fallTime);
			float bounce = (float) (Math.exp(-pos * 4.0) * Math.sin(pos * Math.PI * 3.0));
			float smoothing = 0.1f;
			bounce = (float) Math.sqrt(bounce * bounce + smoothing * smoothing) - smoothing;
			pos = bounce / 3f;
		}

		pos = -(2 * pos - 1) * (target - 0.5f) + 0.5f;

		float angle = pos * 0.78f;

		boolean top = pos < 0.2;
		boolean bottom = pos > 0.8;
		boolean flipped = blockState.getValue(SemaphoreBlock.FLIPPED);
		boolean upside_down = blockState.getValue(SemaphoreBlock.UPSIDE_DOWN);
		PartialModel arm;
		if (upside_down) {
			arm = flipped ?
					yellow ? CRBlockPartials.SEMAPHORE_ARM_YELLOW_FLIPPED_UPSIDE_DOWN : CRBlockPartials.SEMAPHORE_ARM_RED_FLIPPED_UPSIDE_DOWN :
					yellow ? CRBlockPartials.SEMAPHORE_ARM_YELLOW_UPSIDE_DOWN : CRBlockPartials.SEMAPHORE_ARM_RED_UPSIDE_DOWN;
		} else {
			arm = flipped ?
					yellow ? CRBlockPartials.SEMAPHORE_ARM_YELLOW_FLIPPED : CRBlockPartials.SEMAPHORE_ARM_RED_FLIPPED :
					yellow ? CRBlockPartials.SEMAPHORE_ARM_YELLOW : CRBlockPartials.SEMAPHORE_ARM_RED;
		}
		state.arm = CachedBuffers.partial(arm, blockState)
				.cardinalLighting(cardinalLighting)
				.light(state.lightCoords)
				.rotateCentered(angle * (upside_down ? -1 : 1), Direction.EAST)
				.extractRenderState();

		float renderTime = AnimationTickHolder.getRenderTime(level);

		top = top && (renderTime % 40 < 3 || te.isValid);

		state.lamp = null;
		if ((top || bottom)) {
			if (upside_down) {
				if (bottom)
					state.lampOffset = new Vec3(8 / 16.0, 9 / 16.0, 15 / 16.0);
				else
					state.lampOffset = new Vec3(8 / 16.0, 4 / 16.0, 14 / 16.0);
			} else {
				if (bottom)
					state.lampOffset = new Vec3(8 / 16.0, 7 / 16.0, 15 / 16.0);
				else
					state.lampOffset = new Vec3(8 / 16.0, 12 / 16.0, 14 / 16.0);
			}

			state.cube = CachedBuffers.partial(AllPartialModels.SIGNAL_WHITE_CUBE, blockState)
					.cardinalLighting(cardinalLighting)
					.light(LightCoordsUtil.FULL_BRIGHT)
					.disableDiffuse()
					.scale(1, 1, 1)
					.extractRenderState();

			state.glow = CachedBuffers
					.partial(
							bottom ? AllPartialModels.SIGNAL_WHITE_GLOW : yellow ? AllPartialModels.SIGNAL_YELLOW_GLOW : AllPartialModels.SIGNAL_RED_GLOW,
							blockState)
					.cardinalLighting(cardinalLighting)
					.light(LightCoordsUtil.FULL_BRIGHT)
					.disableDiffuse()
					.scale(1.5f, 2, 2)
					.extractRenderState();

			state.lamp = CachedBuffers
					.partial(bottom ? CRBlockPartials.SEMAPHORE_LAMP_WHITE : yellow ? CRBlockPartials.SEMAPHORE_LAMP_YELLOW : CRBlockPartials.SEMAPHORE_LAMP_RED
							, blockState)
					.cardinalLighting(cardinalLighting)
					.light(LightCoordsUtil.FULL_BRIGHT)
					.disableDiffuse()
					.scale(1 + 1 / 16f)
					.extractRenderState();
		}
	}

	@Override
	public void submit(SemaphoreRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState cameraState) {
		ms.pushPose();
		TransformStack.of(ms)
				.center()
				.rotateYDegrees(state.yRot)
				.uncenter();

		state.arm.submit(ms, queue);

		if (state.lamp != null) {
			ms.pushPose();
			ms.translate(state.lampOffset.x, state.lampOffset.y, state.lampOffset.z);
			state.cube.submit(CreateRenderTypes.translucent(), ms, queue);
			state.glow.submit(CreateRenderTypes.additive2(), ms, queue);
			state.glow.submit(CreateRenderTypes.additive(), ms, queue);
			state.lamp.submit(CreateRenderTypes.additive2(), ms, queue);
			state.lamp.submit(CreateRenderTypes.additive(), ms, queue);
			ms.popPose();
		}

		ms.popPose();
	}

	public static class SemaphoreRenderState extends BlockEntityRenderState {
		public float yRot;
		public @UnknownNullability SuperByteBufferRenderState arm;
		public @Nullable SuperByteBufferRenderState lamp;
		public @UnknownNullability SuperByteBufferRenderState cube;
		public @UnknownNullability SuperByteBufferRenderState glow;
		public @UnknownNullability Vec3 lampOffset;
	}
}
