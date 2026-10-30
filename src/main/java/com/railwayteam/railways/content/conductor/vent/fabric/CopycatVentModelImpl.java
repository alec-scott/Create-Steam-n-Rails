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

package com.railwayteam.railways.content.conductor.vent.fabric;

import com.railwayteam.railways.content.conductor.ClientHandler;
import com.railwayteam.railways.content.conductor.vent.CopycatVentModel;
import com.railwayteam.railways.content.conductor.vent.VentBlock;
import com.railwayteam.railways.registry.CRBlocks;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CopycatVentModelImpl extends CopycatVentModel {
    public CopycatVentModelImpl(BlockState state, UnbakedRoot unbaked) {
        super(state, unbaked);
    }

    @Override
    protected void addPartsWithInfo(BlockAndTintGetter world, BlockPos pos, BlockState state, CopycatBlock block,
                                    BlockState material, RandomSource random, List<BlockStateModelPart> parts) {
        if (ClientHandler.isPlayerMountedOnCamera() || state.getValue(VentBlock.CONDUCTOR_VISIBLE)) {
            BlockState visible = CRBlocks.CONDUCTOR_VENT.getDefaultState().setValue(VentBlock.CONDUCTOR_VISIBLE, true);
            if (getModelOf(visible) instanceof CopycatVentModelImpl impl)
                impl.model.collectParts(random, parts);
            return;
        }

        addModelParts(world, pos, material, random, getModelOf(material), parts);
    }

    public static CopycatVentModel create(BlockState state, UnbakedRoot unbaked) {
        return new CopycatVentModelImpl(state, unbaked);
    }
}
