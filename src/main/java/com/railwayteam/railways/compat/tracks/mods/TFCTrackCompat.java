/*
 * Steam 'n' Rails
 * Copyright (c) 2024 The Railways Team
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

package com.railwayteam.railways.compat.tracks.mods;

import net.minecraft.core.registries.BuiltInRegistries;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.compat.Mods;
import com.railwayteam.railways.compat.tracks.GenericTrackCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.stream.Stream;

public class TFCTrackCompat extends GenericTrackCompat {
    TFCTrackCompat() {
        super(Mods.TFC);
    }

    @Override
    protected Ingredient getIngredientForRail() {
        // PORT-NOTE(26.1): single HolderSet ingredient (was wrought iron OR zinc rods); datagen use only
        return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("forge", "rods/wrought_iron"))));
    }

    @Override
    protected Identifier getSlabLocation(String name) {
        return asResource("wood/planks/" + name + "_slab");
    }

    private static boolean registered = false;
    public static void register() {
        if (registered) {
            Railways.LOGGER.error("Duplicate registration of TerraFirmaCraft track compat");
            return;
        }
        registered = true;
        Railways.LOGGER.info("Registering tracks for TerraFirmaCraft");
        new TFCTrackCompat().register(
            "acacia", "ash", "aspen", "birch", "blackwood",
                "chestnut", "douglas_fir", "hickory", "kapok", "mangrove",
                "maple", "oak", "palm", "pine", "rosewood",
                "sequoia", "spruce", "sycamore", "white_cedar", "willow"
        );
    }
}
