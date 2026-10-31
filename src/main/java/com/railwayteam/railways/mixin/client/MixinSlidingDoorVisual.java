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
import com.zurrtum.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

// PORT-NOTE(26.1): with Flywheel active Create Fly draws doors through these visuals instead of SlidingDoorRenderer,
// so the palettes partials are swapped in here too (see MixinSlidingDoorRenderer)
@Mixin(targets = {
    "com.zurrtum.create.client.content.decoration.slidingDoor.SlidingDoorVisual$SlidingVisual",
    "com.zurrtum.create.client.content.decoration.slidingDoor.SlidingDoorVisual$FoldingVisual"
})
public class MixinSlidingDoorVisual {
    @WrapOperation(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object getPalettesPartials(@SuppressWarnings("rawtypes") Map instance, Object key, Operation<Object> original,
                                       @Local(argsOnly = true) SlidingDoorBlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof PalettesSlidingDoorBlock block) {
            boolean windowed = state.getValue(PalettesSlidingDoorBlock.WINDOWED);
            return (block.isFoldingDoor() ? CRBlockPartials.FOLDING_DOORS : CRBlockPartials.SLIDING_DOORS)
                .get(block.color).get(windowed);
        }
        return original.call(instance, key);
    }
}
