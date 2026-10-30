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

package com.railwayteam.railways.mixin.client;

import com.railwayteam.railways.config.CRConfigs;
import com.railwayteam.railways.registry.CREntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.equipment.hats.HatFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// PORT-NOTE(26.1): Create Fly picks train hats inline while extracting render state, so this skips the hat layer
@Mixin(value = HatFeatureRenderer.class, remap = false)
public class MixinEntityHats {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void railways$shouldRenderTrainHat(PoseStack ms, SubmitNodeCollector queue, int light, LivingEntityRenderState renderState,
                                               float limbAngle, float limbDistance, CallbackInfo ci) {
        // If its conductor and the config says to not render the double cap then dont render it
        if (renderState.entityType == CREntities.CONDUCTOR.get() && !CRConfigs.client().renderNormalCap.get())
            ci.cancel();
    }
}
