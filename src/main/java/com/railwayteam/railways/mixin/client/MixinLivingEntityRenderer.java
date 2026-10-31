/*
 * Steam 'n' Rails
 * Copyright (c) 2026 The Railways Team
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

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.content.conductor.ConductorRenderState;
import com.railwayteam.railways.content.conductor.ConductorRenderer;
import com.railwayteam.railways.mixin_interfaces.IConductorDisguiseState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PORT-NOTE(26.1): {@code AvatarRenderer} inherits {@code submit}, so the disguised player is swapped for its conductor
 * here, where the player renderer would otherwise submit the player model and layers.
 */
@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"), cancellable = true)
    private void railways$submitConductorDisguise(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector queue,
                                                  CameraRenderState cameraState, CallbackInfo ci) {
        if (!(state instanceof IConductorDisguiseState disguiseState))
            return;
        ConductorRenderState disguise = disguiseState.railways$getDisguise();
        if (disguise == null)
            return;
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(disguise) instanceof ConductorRenderer renderer) {
            ci.cancel();
            renderer.submit(disguise, poseStack, queue, cameraState);
        }
    }
}
