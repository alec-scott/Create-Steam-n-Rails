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
package com.railwayteam.railways.registry;

import com.railwayteam.railways.Railways;
import com.zurrtum.create.infrastructure.fluids.FlowableFluid;
import com.zurrtum.create.infrastructure.fluids.FluidEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class CRFluids {
    /**
     * Paint: a fluid without block or bucket, colored by the {@code railways:paint_color} component.
     *
     * <p>PORT-NOTE(fabric): was a Registrate {@code virtualFluid}; registered the way Create Fly registers its own
     * virtual fluids ({@code AllFluids}).</p>
     */
    public static final FlowableFluid PAINT = registerVirtual("paint");

    public static void register() {
        com.railwayteam.railways.registry.fabric.CRFluidsImpl.registerAttributes();
    }

    private static FlowableFluid registerVirtual(String name) {
        Identifier id = Railways.asResource(name);
        FluidEntry entry = new FluidEntry();
        entry.still = new FlowableFluid.Still(entry);
        entry.flowing = new FlowableFluid.Flowing(entry);
        Registry.register(BuiltInRegistries.FLUID, id, entry.still);
        Registry.register(BuiltInRegistries.FLUID, id.withPrefix("flowing_"), entry.flowing);
        return entry.still;
    }

    @Environment(EnvType.CLIENT)
    public static void initRendering() {
        com.railwayteam.railways.registry.fabric.CRFluidsImpl.initRendering();
    }
}
