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

package com.railwayteam.railways.content.fuel.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.client.catnip.render.FluidRenderHelper;
import com.zurrtum.create.client.content.fluids.tank.FluidTankRenderer.FluidTankRenderState;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// PORT-NOTE(26.1): render-state renderer modelled on Create Fly's FluidTankRenderer (window fluid only, no boiler).
public class FuelTankRenderer implements BlockEntityRenderer<FuelTankBlockEntity, FluidTankRenderState> {
    public FuelTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public FluidTankRenderState createRenderState() {
        return new FluidTankRenderState();
    }

    @Override
    public boolean shouldRender(FuelTankBlockEntity be, Vec3 cameraPosition) {
        return be.isController() && BlockEntityRenderer.super.shouldRender(be, cameraPosition);
    }

    @Override
    public void extractRenderState(FuelTankBlockEntity be, FluidTankRenderState state, float partialTicks, Vec3 cameraPos,
                                   @Nullable CrumblingOverlay crumblingOverlay) {
        Level world = SmartBlockEntityRenderer.extractBase(be, state, crumblingOverlay);
        state.fluid = null;
        if (!be.isController())
            return;
        if (!be.window) {
            return;
        }

        LerpedFloat fluidLevel = be.getFluidLevel();
        if (fluidLevel == null)
            return;

        float capHeight = 1 / 4f;
        float tankHullWidth = 1 / 16f + 1 / 128f;
        float minPuddleHeight = 1 / 16f;
        float totalHeight = be.getHeight() - 2 * capHeight - minPuddleHeight;

        float level = fluidLevel.getValue(partialTicks);
        if (level < 1 / (512f * totalHeight))
            return;
        float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

        FluidStack fluidStack = be.getTankInventory().getFluid();

        if (fluidStack.isEmpty())
            return;

        boolean top = FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluidStack.getFluid()));

        float xMin = tankHullWidth;
        float xMax = xMin + be.getWidth() - 2 * tankHullWidth;
        float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
        float yMax = yMin + clampedLevel;

        if (top) {
            yMin += totalHeight - clampedLevel;
            yMax += totalHeight - clampedLevel;
        }

        float zMin = tankHullWidth;
        float zMax = zMin + be.getWidth() - 2 * tankHullWidth;

        state.translate = clampedLevel - totalHeight;
        state.fluid = FluidRenderHelper.extractFluidRenderState(
            world instanceof BlockAndTintGetter getter ? getter : null,
            state.blockPos,
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            fluidStack.getFluid(),
            fluidStack.getComponentChanges(),
            xMin, yMin, zMin, xMax, yMax, zMax,
            state.lightCoords,
            false,
            true
        );
    }

    @Override
    public void submit(FluidTankRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (state.fluid != null) {
            ms.translate(0, state.translate, 0);
            state.fluid.submit(ms, queue);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
