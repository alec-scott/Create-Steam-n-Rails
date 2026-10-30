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

package com.railwayteam.railways.content.buffer.headstock.fabric;

import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.foundation.model.BakedModelHelper;
import com.zurrtum.create.client.infrastructure.model.CopycatModel;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Model holder for {@link com.railwayteam.railways.content.buffer.headstock.CopycatHeadstockBarsBlock}: the copycat
 * headstock delegates to {@link #addBarsParts} when its material is a bars block.
 */
public class CopycatHeadstockBarsModel extends CopycatModel {
    public CopycatHeadstockBarsModel(BlockState state, UnbakedRoot unbaked) {
        super(state, unbaked);
    }

    @Override
    protected void addPartsWithInfo(BlockAndTintGetter world, BlockPos pos, BlockState state, CopycatBlock block,
                                    BlockState material, RandomSource random, List<BlockStateModelPart> parts) {
        addBarsParts(world, pos, state, block, material, random, parts);
    }

    /** {@code state} and {@code block} are the copycat headstock's. */
    public void addBarsParts(BlockAndTintGetter world, BlockPos pos, BlockState state, CopycatBlock block,
                             BlockState material, RandomSource random, List<BlockStateModelPart> parts) {
        BlockStateModel materialModel = getModelOf(material);
        TextureAtlasSprite mainTargetSprite = materialModel.particleMaterial().sprite();

        // This is very cursed
        TextureAtlasSprite altTargetSprite = mainTargetSprite;
        find:
        for (BlockStateModelPart part : getMaterialParts(world, pos, material, random, materialModel)) {
            for (BakedQuad quad : part.getQuads(null)) {
                if (quad.direction() == Direction.UP) {
                    altTargetSprite = quad.materialInfo().sprite();
                    break find;
                }
            }
        }

        DirectionData directionData = gatherDirectionData(block, state);
        List<BlockStateModelPart> ownParts = new ObjectArrayList<>();
        model.collectParts(random, ownParts);
        TextureAtlasSprite verticalSprite = altTargetSprite;
        for (BlockStateModelPart part : ownParts) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            BakedModelHelper.swapSprites(part.getQuads(null), sprite -> mainTargetSprite)
                .forEach(builder::addUnculledFace);
            for (Direction cullFace : Iterate.directions) {
                TextureAtlasSprite target = cullFace.getAxis() == Direction.Axis.Y ? verticalSprite : mainTargetSprite;
                for (BakedQuad quad : BakedModelHelper.swapSprites(part.getQuads(cullFace), sprite -> target)) {
                    if (directionData.isUncull(cullFace))
                        builder.addUnculledFace(quad);
                    else
                        builder.addCulledFace(cullFace, quad);
                }
            }
            parts.add(new SimpleModelWrapper(builder.build(), false, part.particleMaterial()));
        }
    }
}
