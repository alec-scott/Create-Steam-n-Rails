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

package com.railwayteam.railways.mixin;

import com.railwayteam.railways.mixin_interfaces.CopycatDuck;
import com.railwayteam.railways.port.create.CopycatTypeOverride;
import com.zurrtum.create.content.decoration.copycat.CopycatBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CopycatBlockEntity.class)
public abstract class MixinCopycatBlockEntity extends SmartBlockEntity implements CopycatDuck {
    @Shadow private BlockState material;

    @Shadow protected abstract void redraw();

    // PORT-NOTE(26.1): see CopycatTypeOverride
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
        target = "Lcom/zurrtum/create/foundation/blockEntity/SmartBlockEntity;<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"),
        index = 0)
    private static BlockEntityType<?> railways$overrideType(BlockEntityType<?> type) {
        return CopycatTypeOverride.take(type);
    }

    private MixinCopycatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void railways$setMaterialSimple(BlockState material) {
        this.material = material;

        if (level == null || !level.isClientSide()) {
            notifyUpdate();
            return;
        }
        redraw();
    }
}
