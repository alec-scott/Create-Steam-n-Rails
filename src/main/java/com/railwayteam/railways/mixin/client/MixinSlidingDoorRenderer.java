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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.railwayteam.railways.content.palettes.doors.PalettesSlidingDoorBlock;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.content.decoration.slidingDoor.SlidingDoorRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(SlidingDoorRenderer.class)
public class MixinSlidingDoorRenderer {
    @WrapOperation(
        // PORT-NOTE(26.1): Create Fly picks the partials in extractRenderState (the block state is on the render state)
        method = "extractRenderState(Lcom/zurrtum/create/content/decoration/slidingDoor/SlidingDoorBlockEntity;Lcom/zurrtum/create/client/content/decoration/slidingDoor/SlidingDoorRenderer$DoorRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object getPalettesPartials(@SuppressWarnings("rawtypes") Map instance, Object key, Operation<Object> original,
                                       @Local(argsOnly = true) SlidingDoorRenderer.DoorRenderState doorState) {
        BlockState state = doorState.blockState;
        if (state.getBlock() instanceof PalettesSlidingDoorBlock block && block.isFoldingDoor()) {
            return CRBlockPartials.FOLDING_DOORS.get(block.color).get(state.getValue(PalettesSlidingDoorBlock.WINDOWED));
        }
        Object partial = original.call(instance, key);
        // PORT-NOTE(26.1): Create Fly draws sliding doors from one combined partial per door (1.20 drew the block
        // models of both halves), and Railways has none yet; fall back to Create's train door so nothing is null.
        // TODO: combined palettes sliding door models.
        if (partial == null && state.getBlock() instanceof PalettesSlidingDoorBlock)
            return AllPartialModels.SLIDING_DOORS.get(Identifier.fromNamespaceAndPath("create", "train_door"));
        return partial;
    }
}
