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

package com.railwayteam.railways.content.custom_tracks.generic_crossing.fabric;

import com.railwayteam.railways.mixin_interfaces.IGenericCrossingTrackBE;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.infrastructure.model.WrapperBlockStateModel;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import com.zurrtum.create.content.trains.track.TrackShape;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// PORT-NOTE(26.1): FRAPI emitBlockQuads -> Create Fly's WrapperBlockStateModel parts. There is no item.
public class GenericCrossingModel extends WrapperBlockStateModel {
    public GenericCrossingModel(BlockState state, UnbakedRoot unbaked) {
        super(state, unbaked);
    }

    @Override
    public void addPartsWithInfo(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random,
                                 List<BlockStateModelPart> parts) {
        if (world.getBlockEntity(pos) instanceof IGenericCrossingTrackBE genericCrossing) {
            addPiece(world, pos, random, genericCrossing.railways$getFirstCrossingPiece(), parts);
            addPiece(world, pos, random, genericCrossing.railways$getSecondCrossingPiece(), parts);
        }
    }

    private static void addPiece(BlockAndTintGetter world, BlockPos pos, RandomSource random,
                                 @Nullable Pair<TrackMaterial, TrackShape> piece, List<BlockStateModelPart> parts) {
        if (piece == null)
            return;
        BlockStateModel model = IGenericCrossingTrackBE.getModel(piece);
        if (model instanceof WrapperBlockStateModel wrapper) {
            BlockState pieceState = piece.getFirst().getBlock().defaultBlockState()
                .setValue(TrackBlock.SHAPE, piece.getSecond());
            wrapper.addPartsWithInfo(world, pos, pieceState, random, parts);
        } else {
            model.collectParts(random, parts);
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
    }
}
