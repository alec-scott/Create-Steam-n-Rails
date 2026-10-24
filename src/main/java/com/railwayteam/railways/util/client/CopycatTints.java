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
package com.railwayteam.railways.util.client;

import com.zurrtum.create.client.AllBlockTints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Tints for copycat blocks: each tint index forwards to the copied material's tint.
 *
 * <p>PORT-NOTE(fabric): upstream used {@code CopycatBlock::wrappedColor}. Create Fly's equivalent
 * ({@link AllBlockTints.WrappedBlockColor}) needs the {@code BlockColors} instance, which doesn't exist yet when blocks
 * register, so this looks it up when a colour is first asked for.</p>
 */
public record CopycatTints(int tintIndex) implements BlockTintSource {
    public static List<BlockTintSource> wrapped() {
        return List.of(new CopycatTints(0), new CopycatTints(1), new CopycatTints(2));
    }

    private BlockTintSource delegate() {
        return new AllBlockTints.WrappedBlockColor(Minecraft.getInstance().getBlockColors(), tintIndex);
    }

    @Override
    public int color(BlockState state) {
        return delegate().color(state);
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return delegate().colorInWorld(state, level, pos);
    }

    @Override
    public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return delegate().colorAsTerrainParticle(state, level, pos);
    }
}
