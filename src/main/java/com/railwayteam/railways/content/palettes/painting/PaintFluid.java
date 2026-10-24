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
package com.railwayteam.railways.content.palettes.painting;

import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.registry.CRDataComponents;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentPatch;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Paint fluid color helpers.
 *
 * <p>PORT-NOTE(fabric): the color was a {@code Color} key in the fluid's NBT; it's the {@code railways:paint_color}
 * component now, read from fluid variants or Create Fly fluid stacks alike.</p>
 */
public class PaintFluid {
    public static final String LANG_PREFIX = "fluid.railways.paint.";

    public static Optional<PalettesColor> getColor(DataComponentHolder components) {
        return Optional.ofNullable(components.get(CRDataComponents.PAINT_COLOR));
    }

    public static DataComponentPatch colorPatch(@Nullable PalettesColor color) {
        if (color == null) return DataComponentPatch.EMPTY;
        return DataComponentPatch.builder().set(CRDataComponents.PAINT_COLOR, color).build();
    }
}
