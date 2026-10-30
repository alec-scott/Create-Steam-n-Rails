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

package com.railwayteam.railways.registry;

import com.google.common.collect.ImmutableSet;
import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.mixin.AccessorBlockEntityType;
import com.zurrtum.create.AllBlockEntityTypes;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.HashSet;
import java.util.Set;

/**
 * Attaches Railways content to Create's block entity types.
 *
 * <p>PORT-NOTE(fabric): 1.20 waited on both mods' Registrate entries ({@code MultiRegistryCallback}) to work with any
 * load order. Create Fly registers its block entity types eagerly in {@link AllBlockEntityTypes}, so this runs once
 * Railways' own blocks exist ({@code Railways.postRegistrationInit}).</p>
 */
public class CRExtraRegistration {
    private static final Set<BlockEntityType<?>> modifiedTypes = new HashSet<>();

    public static void register() {}

    public static void afterRegistration() {
        DisplaySource.BY_BLOCK_ENTITY.add(AllBlockEntityTypes.TRACK_SIGNAL, CRDisplaySources.SIGNAL.get());

        addBlockToBE(AllBlockEntityTypes.COPYCAT, CRBlocks.CONDUCTOR_VENT.get());
        for (PalettesColor color : PalettesColor.values()) {
            addBlockToBE(AllBlockEntityTypes.FLYWHEEL, CRPalettes.Styles.FLYWHEEL.get(color).get());
            addBlockToBE(AllBlockEntityTypes.SLIDING_DOOR, CRPalettes.Styles.SLIDING_DOOR.get(color).get());
            addBlockToBE(AllBlockEntityTypes.SLIDING_DOOR, CRPalettes.Styles.FOLDING_DOOR.get(color).get());
        }

        finalizeBlockEntityTypes();
    }

    private static void addBlockToBE(BlockEntityType<?> be, Block block) {
        Set<Block> validBlocks = ((AccessorBlockEntityType) be).getValidBlocks();
        try {
            validBlocks.add(block);
        } catch (UnsupportedOperationException e) {
            validBlocks = new HashSet<>(validBlocks);
            validBlocks.add(block);
            ((AccessorBlockEntityType) be).setValidBlocks(validBlocks);
        }
        modifiedTypes.add(be);
    }

    // it is likely that an ImmutableSet will be more efficient in terms of memory and query time than a HashSet, so we
    // refreeze the set after all modifications are done
    private static void finalizeBlockEntityTypes() {
        for (BlockEntityType<?> type : modifiedTypes) {
            Set<Block> validBlocks = ((AccessorBlockEntityType) type).getValidBlocks();
            if (!(validBlocks instanceof HashSet)) continue;
            ((AccessorBlockEntityType) type).setValidBlocks(ImmutableSet.copyOf(validBlocks));
        }
        modifiedTypes.clear();
    }
}
