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

import com.railwayteam.railways.content.buffer.IDyedBuffer;
import com.railwayteam.railways.content.buffer.headstock.CopycatHeadstockBarsBlock;
import com.railwayteam.railways.content.buffer.headstock.CopycatHeadstockBlock;
import com.railwayteam.railways.registry.CRBlocks;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.foundation.model.BakedModelHelper;
import com.zurrtum.create.client.infrastructure.model.CopycatModel;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import com.zurrtum.create.content.decoration.copycat.CopycatSpecialCases;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.UnaryOperator;

import static com.railwayteam.railways.content.buffer.BufferModelUtils.addSwappedParts;
import static com.railwayteam.railways.content.buffer.BufferModelUtils.getSwapper;

/**
 * PORT-NOTE(26.1): FRAPI quad transforms -> Create Fly's {@link CopycatModel} parts. Faces culled by neighbours now
 * keep their cull face on the cropped quad (as Create Fly's copycat step/panel do) instead of the 1.20 occlusion pass,
 * and connected textures are not filtered by {@code canConnectTexturesToward} (Create Fly's copycats don't either).
 * The item renders the plain model; it no longer reads material/colour from the stack.
 */
@Environment(EnvType.CLIENT)
public class CopycatHeadstockModel extends CopycatModel {

    private static final Identifier COPYCAT_BASE = Identifier.fromNamespaceAndPath("create", "block/copycat_base");

    public CopycatHeadstockModel(BlockState state, UnbakedRoot unbaked) {
        super(state, unbaked);
    }

    private static boolean filterCopycatParts(BakedQuad quad) {
        return !quad.materialInfo().sprite().contents().name().equals(COPYCAT_BASE);
    }

    @Override
    protected void addPartsWithInfo(BlockAndTintGetter world, BlockPos pos, BlockState state, CopycatBlock block,
                                    BlockState material, RandomSource random, List<BlockStateModelPart> parts) {
        UnaryOperator<TextureAtlasSprite> colorSwapper = null;
        if (world.getBlockEntity(pos) instanceof IDyedBuffer be)
            colorSwapper = getSwapper(be.getColor());

        // the headstock itself, minus the copycat base quads
        addSwappedParts(model, random, colorSwapper, CopycatHeadstockModel::filterCopycatParts, null, parts);

        Direction facing = state.getOptionalValue(CopycatHeadstockBlock.FACING).orElse(Direction.NORTH);
        boolean upsideDown = state.getValue(CopycatHeadstockBlock.UPSIDE_DOWN);

        if (CopycatSpecialCases.isBarsMaterial(material)) {
            BlockState specialState = CRBlocks.COPYCAT_HEADSTOCK_BARS.getDefaultState()
                .setValue(CopycatHeadstockBarsBlock.FACING, facing)
                .setValue(CopycatHeadstockBarsBlock.UPSIDE_DOWN, upsideDown);

            if (getModelOf(specialState) instanceof CopycatHeadstockBarsModel cm) {
                cm.addBarsParts(world, pos, state, block, material, random, parts);
                return;
            }
        }

        List<BlockStateModelPart> materialParts = getMaterialParts(world, pos, material, random, getModelOf(material));
        if (materialParts.isEmpty())
            return;

        Vec3 normal = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        Vec3 normalScaled14 = normal.scale(14 / 16f);
        DirectionData directionData = gatherDirectionData(block, state);

        for (BlockStateModelPart part : materialParts) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            for (BakedQuad quad : part.getQuads(null))
                addPieces(builder, null, quad, facing, normal, normalScaled14, upsideDown);
            for (Direction cullFace : Iterate.directions) {
                Direction target = directionData.isUncull(cullFace) ? null : cullFace;
                for (BakedQuad quad : part.getQuads(cullFace))
                    addPieces(builder, target, quad, facing, normal, normalScaled14, upsideDown);
            }
            parts.add(new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleMaterial()));
        }
    }

    // 4 Pieces
    private static void addPieces(QuadCollection.Builder builder, Direction cullFace, BakedQuad quad, Direction facing,
                                  Vec3 normal, Vec3 normalScaled14, boolean upsideDown) {
        Direction direction = quad.direction();
        for (boolean top : Iterate.trueAndFalse) {
            for (boolean front : Iterate.trueAndFalse) {
                if (front && direction == facing)
                    continue;
                if (!front && direction == facing.getOpposite())
                    continue;
                if (top && direction == Direction.DOWN)
                    continue;
                if (!top && direction == Direction.UP)
                    continue;

                Vec3 offset = normal.scale(front ? 0 : -13 / 16f);
                float contract = 16 - (front ? 1 : 2);
                AABB bb = CUBE_AABB.contract(normal.x * contract / 16, 10 / 16., normal.z * contract / 16);
                if (!front)
                    bb = bb.move(normalScaled14);
                if (top)
                    bb = bb.move(0, 10 / 16., 0);
                else
                    offset = offset.add(0, 4 / 16., 0);

                if (upsideDown)
                    offset = offset.add(0, -4 / 16., 0);

                BakedQuad cropped = BakedModelHelper.cropAndMove(quad, bb, offset);
                if (cullFace == null)
                    builder.addUnculledFace(cropped);
                else
                    builder.addCulledFace(cullFace, cropped);
            }
        }
    }
}
