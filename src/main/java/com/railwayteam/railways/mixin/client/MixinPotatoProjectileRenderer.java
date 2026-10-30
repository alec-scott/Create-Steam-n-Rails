/*
 * Steam 'n' Rails
 * Copyright (c) 2025 The Railways Team
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
import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.content.palettes.painting.PaintPitcherItem;
import com.railwayteam.railways.mixin_interfaces.IExtraRenderParts;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.equipment.potatoCannon.PotatoProjectileRenderer;
import com.zurrtum.create.client.content.equipment.potatoCannon.PotatoProjectileRenderer.PotatoProjectileState;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Paint pitcher projectiles render as paint blobs.
 *
 * <p>PORT-NOTE(26.1): was a wrap of {@code ItemRenderer.renderStatic} with Create's {@code PartialItemModelRenderer}.
 * The blob is extracted into the projectile's render state and submitted with Create Fly's projectile transforms;
 * the 1.20 item display transform is approximated (centred, half scale). Check visually.</p>
 */
@Mixin(value = PotatoProjectileRenderer.class, remap = false)
public class MixinPotatoProjectileRenderer {
    @Inject(method = "extractRenderState(Lcom/zurrtum/create/content/equipment/potatoCannon/PotatoProjectileEntity;Lcom/zurrtum/create/client/content/equipment/potatoCannon/PotatoProjectileRenderer$PotatoProjectileState;F)V",
        at = @At("TAIL"))
    private void railways$extractPaintBlob(PotatoProjectileEntity entity, PotatoProjectileState state, float tickProgress, CallbackInfo ci) {
        List<SuperByteBufferRenderState> parts = ((IExtraRenderParts) state).railways$getExtraParts();
        parts.clear();
        ItemStack stack = entity.getItem();
        if (!(stack.getItem() instanceof PaintPitcherItem item))
            return;
        PalettesColor color = item.getColor();
        PartialModel model = color == null ? CRBlockPartials.PAINT_STRIPPER_BLOB : CRBlockPartials.PAINT_BLOBS.get(color);
        parts.add(CachedBuffers.partial(model, Blocks.AIR.defaultBlockState())
            .scale(.5f)
            .translate(-.5f, -.5f, -.5f)
            .light(state.lightCoords)
            .extractRenderState());
        state.item = new ItemStackRenderState(); // empty: Create Fly's submit skips the item
    }

    @Inject(method = "submit(Lcom/zurrtum/create/client/content/equipment/potatoCannon/PotatoProjectileRenderer$PotatoProjectileState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"))
    private void railways$submitPaintBlob(PotatoProjectileState state, PoseStack ms, SubmitNodeCollector queue,
                                         CameraRenderState cameraState, CallbackInfo ci) {
        List<SuperByteBufferRenderState> parts = ((IExtraRenderParts) state).railways$getExtraParts();
        if (parts.isEmpty())
            return;
        ms.pushPose();
        ms.translate(0, state.translateY, 0);
        if (state.transformer != null)
            state.transformer.transform(state.mode, ms, state);
        for (SuperByteBufferRenderState part : parts)
            part.submit(ms, queue);
        ms.popPose();
    }
}
