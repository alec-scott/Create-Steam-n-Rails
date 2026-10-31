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

package com.railwayteam.railways.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.railwayteam.railways.registry.CRTags;
import com.zurrtum.create.content.trains.entity.Train;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Train.class)
public class TrainMixin {
    // PORT-NOTE(26.1): Create Fly extracts fuel with a predicate over FuelValues.burnDuration, so items tagged as not
    // train fuel get no burn time there (1.20 filtered Fabric transfer storage views).
    @ModifyExpressionValue(method = "lambda$burnFuel$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/FuelValues;burnDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    private static int railways$disableFuelConsumptionBasedOnTag(int original, @Local(argsOnly = true) ItemStack stack) {
        return stack.is(CRTags.AllItemTags.NOT_TRAIN_FUEL.tag) ? 0 : original;
    }
}
