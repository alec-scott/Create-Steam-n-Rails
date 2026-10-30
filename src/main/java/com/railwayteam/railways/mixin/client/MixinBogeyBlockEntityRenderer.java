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

package com.railwayteam.railways.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.content.coupling.VirtualCouplerRendering;
import com.railwayteam.railways.mixin_interfaces.IExtraRenderParts;
import com.railwayteam.railways.mixin_interfaces.IStandardBogeyTEVirtualCoupling;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.trains.bogey.BogeyBlockEntityRenderer;
import com.zurrtum.create.client.content.trains.bogey.BogeyBlockEntityRenderer.BogeyBlockEntityRenderState;
import com.zurrtum.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.zurrtum.create.content.trains.bogey.StandardBogeyBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// PORT-NOTE(26.1): render-state renderer; the virtual coupler is extracted with the bogey and submitted before it
@Mixin(value = BogeyBlockEntityRenderer.class, remap = false)
public class MixinBogeyBlockEntityRenderer {
    @Inject(method = "extractRenderState(Lcom/zurrtum/create/content/trains/bogey/AbstractBogeyBlockEntity;Lcom/zurrtum/create/client/content/trains/bogey/BogeyBlockEntityRenderer$BogeyBlockEntityRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("TAIL"))
    private void railways$extractVirtualCoupling(AbstractBogeyBlockEntity be, BogeyBlockEntityRenderState state, float partialTicks,
                                                Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        List<SuperByteBufferRenderState> parts = ((IExtraRenderParts) state).railways$getExtraParts();
        parts.clear();
        if (be instanceof StandardBogeyBlockEntity sbte && be instanceof IStandardBogeyTEVirtualCoupling virtualCoupling) {
            double couplingDistance = virtualCoupling.getCouplingDistance();
            if (couplingDistance > 0) {
                VirtualCouplerRendering.extractCoupler(virtualCoupling.getCouplingDirection(), couplingDistance,
                    virtualCoupling.getFront(), state.lightCoords, sbte, parts);
            }
        }
    }

    @Inject(method = "submit(Lcom/zurrtum/create/client/content/trains/bogey/BogeyBlockEntityRenderer$BogeyBlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"))
    private void railways$submitVirtualCoupling(BogeyBlockEntityRenderState state, PoseStack ms, SubmitNodeCollector queue,
                                               CameraRenderState cameraState, CallbackInfo ci) {
        for (SuperByteBufferRenderState part : ((IExtraRenderParts) state).railways$getExtraParts())
            part.submit(ms, queue);
    }
}
