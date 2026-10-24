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
package com.railwayteam.railways.fabric.client;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.custom_tracks.generic_crossing.GenericCrossingBlock;
import com.railwayteam.railways.content.palettes.boiler.BoilerBlock;
import com.railwayteam.railways.port.registrate.util.entry.RegistryEntry;
import com.zurrtum.create.client.AllExtensions;
import com.zurrtum.create.client.foundation.block.render.MultiPosDestructionHandler;
import com.zurrtum.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;

/**
 * Registers Railways blocks with Create Fly's client block extensions.
 *
 * <p>PORT-NOTE(fabric): upstream Create used interfaces ({@code IHaveBigOutline}, {@code TrackBlock} implementing it,
 * multi-position destruction via the block). Create Fly lists the blocks in {@link AllExtensions} instead, so Railways'
 * tracks, crossings, boilers and doors are added here. Call once during client init.</p>
 */
public final class CRBlockExtensions {
    private CRBlockExtensions() {}

    /** Same as Create Fly's entry for its own track: the break animation covers the track's bezier connections. */
    private static final MultiPosDestructionHandler TRACK_DESTRUCTION = (level, pos, state, progress) -> {
        if (level.getBlockEntity(pos) instanceof TrackBlockEntity track)
            return new HashSet<>(track.getConnections().keySet());
        return null;
    };

    public static void register() {
        for (RegistryEntry<Block, Block> entry : Railways.registrate().<Block, Block>getAll(Registries.BLOCK)) {
            Block block = entry.get();
            if (block instanceof TrackBlock) {
                AllExtensions.BIG_OUTLINE.add(block);
                AllExtensions.MULTI_POS.put(block, TRACK_DESTRUCTION);
            } else if (block instanceof GenericCrossingBlock || block instanceof BoilerBlock || block instanceof SlidingDoorBlock) {
                AllExtensions.BIG_OUTLINE.add(block);
            }
        }
    }
}
