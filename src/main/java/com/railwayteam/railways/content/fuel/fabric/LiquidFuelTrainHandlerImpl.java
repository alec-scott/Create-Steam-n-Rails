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
package com.railwayteam.railways.content.fuel.fabric;

import com.railwayteam.railways.content.fuel.LiquidFuelTrainHandler;
import com.zurrtum.create.api.contraption.storage.fluid.MountedFluidStorageWrapper;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

public class LiquidFuelTrainHandlerImpl {
    /** 100 mB (a tenth of a bucket) in droplets. */
    private static final int FUEL_PER_BURN = 8100;

    /**
     * PORT-NOTE(fabric): Create Fly's mounted fluid storage is a plain {@code FluidInventory} without transactions, so
     * the fuel and amount are checked before extracting instead of rolling a transaction back.
     */
    public static int handleFuelDraining(MountedFluidStorageWrapper fuelFluids) {
        for (int slot = 0; slot < fuelFluids.size(); slot++) {
            FluidStack held = fuelFluids.getStack(slot);
            if (held.isEmpty() || held.getAmount() < FUEL_PER_BURN)
                continue;

            int burnTime = LiquidFuelTrainHandler.handleFuelChecking(held);
            if (burnTime <= 0)
                continue;

            if (fuelFluids.extract(held.copyWithAmount(FUEL_PER_BURN), FUEL_PER_BURN, null) != FUEL_PER_BURN)
                continue;
            return burnTime;
        }
        return 0;
    }
}
