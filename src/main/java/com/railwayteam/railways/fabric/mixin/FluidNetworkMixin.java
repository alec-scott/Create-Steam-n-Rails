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

package com.railwayteam.railways.fabric.mixin;

import com.railwayteam.railways.content.fuel.psi.PortableFuelInterfaceBlockEntity.InterfaceFluidHandler;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.fluids.FluidNetwork;
import com.zurrtum.create.content.fluids.PipeConnection;
import com.zurrtum.create.catnip.math.BlockFace;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

// PORT-NOTE(26.1): Create Fly keeps its own interface engaged with source.markDirty(); do the same for fuel interfaces
@Mixin(value = FluidNetwork.class, remap = false)
public class FluidNetworkMixin {
    @Shadow Set<Pair<BlockFace, PipeConnection>> frontier;
    @Shadow @Nullable FluidInventory source;

    @Inject(method = "keepPortableFluidInterfaceEngaged", at = @At("HEAD"))
    private void keepPortableFluidInterfaceEngaged(CallbackInfo ci) {
        if (!(source instanceof InterfaceFluidHandler))
            return;
        if (frontier.isEmpty())
            return;
        source.markDirty();
    }
}
