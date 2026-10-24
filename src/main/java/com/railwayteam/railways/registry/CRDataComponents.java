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
package com.railwayteam.railways.registry;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.palettes.PalettesColor;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.function.UnaryOperator;

/**
 * Railways' data components.
 *
 * <p>PORT-NOTE(fabric): new in the 26.1 port. Item data Railways stored as NBT mostly lives in
 * {@code minecraft:custom_data} (see {@code ItemUtils.getCustomData}); fluids get proper components.</p>
 */
public class CRDataComponents {
    /** The color of the paint fluid (was the {@code Color} key of its fluid NBT). */
    public static final DataComponentType<PalettesColor> PAINT_COLOR = register("paint_color", b -> b
        .persistent(PalettesColor.CODEC)
        .networkSynchronized(ByteBufCodecs.VAR_INT.map(PalettesColor::byId, PalettesColor::getId)));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Railways.asResource(name),
            builder.apply(DataComponentType.builder()).build());
    }

    public static void register() {}
}
