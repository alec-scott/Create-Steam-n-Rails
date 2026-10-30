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

package com.railwayteam.railways.util.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.railwayteam.railways.mixin_interfaces.IHasCustomOutline;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws {@link IHasCustomOutline} block outlines in place of the vanilla shape outline.
 * <p>
 * Implementation was inspired/derived from <a href="https://github.com/XFactHD/FramedBlocks/blob/17c8274ca380c3a868763b1b05657d07860c364b/src/main/java/xfacthd/framedblocks/client/render/special/BlockOutlineRenderer.java">Framed Blocks</a>
 * <p>
 * Which is licensed under <a href="https://github.com/XFactHD/FramedBlocks/blob/17c8274ca380c3a868763b1b05657d07860c364b/LICENSE">LGPL</a>
 * <p>
 * PORT-NOTE(26.1): was {@code MixinLevelRenderer} wrapping {@code renderHitOutline}; now Fabric's
 * {@link LevelRenderEvents#BEFORE_BLOCK_OUTLINE}.
 */
public final class CustomOutlineRendering {
    private CustomOutlineRendering() {
    }

    public static void register() {
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(CustomOutlineRendering::beforeBlockOutline);
    }

    private static boolean beforeBlockOutline(LevelRenderContext context, BlockOutlineRenderState outline) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return true;
        BlockPos pos = outline.pos();
        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof IHasCustomOutline hasCustomOutline))
            return true;

        PoseStack poseStack = context.poseStack();
        VertexConsumer lineVb = context.bufferSource().getBuffer(RenderTypes.lines());

        Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(mc.gameRenderer.getMainCamera().position());

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        poseStack.translate(0.5, 0.5, 0.5);
        hasCustomOutline.matrixRotation(poseStack, state);
        poseStack.translate(-0.5, -0.5, -0.5);

        hasCustomOutline.customOutline(poseStack, lineVb, state);

        poseStack.popPose();

        return false;
    }
}
