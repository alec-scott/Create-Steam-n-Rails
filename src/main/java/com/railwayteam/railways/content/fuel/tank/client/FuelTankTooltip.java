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
package com.railwayteam.railways.content.fuel.tank.client;

import com.railwayteam.railways.content.fuel.tank.FuelTankBlockEntity;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import net.minecraft.network.chat.Component;

import java.util.List;

/** PORT-NOTE(fabric): the fuel tank's goggle tooltip, a client behaviour in Create Fly (was on the block entity). */
public class FuelTankTooltip extends TooltipBehaviour<FuelTankBlockEntity> implements IHaveGoggleInformation {
    public FuelTankTooltip(FuelTankBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        FuelTankBlockEntity controllerBE = blockEntity.getControllerBE();
        if (controllerBE == null)
            return false;
        if (controllerBE.fluidCapability == null)
            controllerBE.refreshCapability();
        return containedFluidTooltip(tooltip, isPlayerSneaking, controllerBE.fluidCapability);
    }
}
