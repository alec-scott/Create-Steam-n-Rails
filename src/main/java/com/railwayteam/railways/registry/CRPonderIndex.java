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

package com.railwayteam.railways.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import com.railwayteam.railways.ponder.scenes.ConductorScenes;
import com.railwayteam.railways.ponder.scenes.DoorScenes;
import com.railwayteam.railways.ponder.scenes.TrainScenes;
import com.zurrtum.create.AllBlocks;
import com.railwayteam.railways.port.registrate.util.entry.ItemProviderEntry;
import com.railwayteam.railways.port.registrate.util.entry.RegistryEntry;
import com.zurrtum.create.client.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.Identifier;

public class CRPonderIndex {
    public static void register(PonderSceneRegistrationHelper<Identifier> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
        // PORT-NOTE(26.1): Create Fly's blocks are plain blocks, not Registrate entries
        PonderSceneRegistrationHelper<Block> BLOCK_HELPER = helper.withKeyFunction(BuiltInRegistries.BLOCK::getKey);
        
        HELPER.forComponents(CRBlocks.SEMAPHORE)
            .addStoryBoard("semaphore", TrainScenes::signaling);
        HELPER.forComponents(CRBlocks.TRACK_COUPLER)
                .addStoryBoard("coupler", TrainScenes::coupling);
        HELPER.forComponents(CRItems.ITEM_CONDUCTOR_CAP.values().toArray(ItemProviderEntry[]::new))
            .addStoryBoard("conductor", ConductorScenes::constructing)
            .addStoryBoard("conductor_redstone", ConductorScenes::redstoning)
            .addStoryBoard("conductor", ConductorScenes::toolboxing);
        BLOCK_HELPER.forComponents(
            AllBlocks.ANDESITE_DOOR,
            AllBlocks.BRASS_DOOR,
            AllBlocks.COPPER_DOOR,
            AllBlocks.TRAIN_DOOR,
            AllBlocks.FRAMED_GLASS_DOOR
        )
            .addStoryBoard("door_modes", DoorScenes::modes);
        HELPER.forComponents(CRBlocks.ANDESITE_SWITCH, CRBlocks.BRASS_SWITCH)
                .addStoryBoard("switch", TrainScenes::trackSwitch);
    }
}
