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

package com.railwayteam.railways.registry.fabric;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.fuel.psi.PortableFuelInterfaceBlockEntity;
import com.railwayteam.railways.content.fuel.tank.FuelTankBlockEntity;
import com.railwayteam.railways.content.fuel.tank.FuelTankRenderer;
import com.zurrtum.create.client.content.contraptions.actors.psi.PSIVisual;
import com.zurrtum.create.client.content.contraptions.actors.psi.PortableStorageInterfaceRenderer;
import com.railwayteam.railways.port.create.CRTransfer;
import com.railwayteam.railways.port.create.foundation.data.CreateRegistrate;
import com.railwayteam.railways.port.registrate.util.entry.BlockEntityEntry;

public class CRBlockEntitiesImpl {
    private static final CreateRegistrate REGISTRATE = Railways.registrate();

    public static final BlockEntityEntry<FuelTankBlockEntity> FUEL_TANK = REGISTRATE
            .blockEntity("fuel_tank", FuelTankBlockEntity::new)
            .validBlocks(CRBlocksImpl.FUEL_TANK)
            .renderer(() -> FuelTankRenderer::new)
            // PORT-NOTE(fabric): was SidedStorageBlockEntity on the block entity
            .onRegister(type -> CRTransfer.registerFluidSide(type, be -> {
                if (be.fluidCapability == null)
                    be.refreshCapability();
                return be.fluidCapability;
            }))
            .register();

    public static final BlockEntityEntry<PortableFuelInterfaceBlockEntity> PORTABLE_FUEL_INTERFACE = REGISTRATE
            .blockEntity("portable_fuel_interface", PortableFuelInterfaceBlockEntity::new)
            .visual(() -> PSIVisual::new)
            .validBlocks(CRBlocksImpl.PORTABLE_FUEL_INTERFACE)
            .renderer(() -> PortableStorageInterfaceRenderer::new)
            .onRegister(type -> CRTransfer.registerFluidSide(type, be -> be.capability))
            .register();

    public static void init() {}
}
