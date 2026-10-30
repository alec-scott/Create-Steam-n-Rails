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

package com.railwayteam.railways.content.buffer.fabric;

import com.mojang.math.Axis;
import com.railwayteam.railways.content.buffer.IDyedBuffer;
import com.railwayteam.railways.content.buffer.IMaterialAdaptingBuffer;
import com.railwayteam.railways.content.buffer.TrackBufferBlock;
import com.zurrtum.create.client.foundation.model.BakedModelHelper;
import com.zurrtum.create.client.infrastructure.model.WrapperBlockStateModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.UnaryOperator;

import static com.railwayteam.railways.content.buffer.BufferModelUtils.addSwappedParts;
import static com.railwayteam.railways.content.buffer.BufferModelUtils.combineSwappers;
import static com.railwayteam.railways.content.buffer.BufferModelUtils.getSwapper;

// PORT-NOTE(26.1): FRAPI ForwardingBakedModel -> Create Fly's WrapperBlockStateModel. The item no longer reads
// material/colour from the stack (1.20 emitItemQuads); see PORTING-26.1.md.
@Environment(EnvType.CLIENT)
public class BufferModel extends WrapperBlockStateModel {

    public BufferModel(BlockState state, UnbakedRoot unbaked) {
        super(state, unbaked);
    }

    private static final Matrix3f diagonalTransform = getDiagonalRotationMatrix();

    private static BakedQuad rotateDiagonal(BakedQuad quad) {
        Direction direction = quad.direction();
        Vector3f faceNormal = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        faceNormal.mulTranspose(diagonalTransform);
        BakedQuad rotated = new BakedQuad(
            rotate(quad.position0()), rotate(quad.position1()), rotate(quad.position2()), rotate(quad.position3()),
            quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
            direction, quad.materialInfo()
        );
        BakedModelHelper.setNormals(rotated, new Vector3f[]{faceNormal, faceNormal, faceNormal, faceNormal});
        return rotated;
    }

    private static Vector3f rotate(Vector3fc pos) {
        Vector3f vertexPos = new Vector3f(pos);
        vertexPos.sub(0.5f, 0.5f, 0.5f);
        vertexPos.mulTranspose(diagonalTransform);
        vertexPos.add(0.5f, 0.5f, 0.5f);
        return vertexPos;
    }

    @Override
    public void addPartsWithInfo(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random,
                                 List<BlockStateModelPart> parts) {
        boolean isDiagonal = false;

        if (state.getBlock() instanceof TrackBufferBlock)
            isDiagonal = state.getValue(TrackBufferBlock.DIAGONAL);

        UnaryOperator<TextureAtlasSprite> materialSwapper = null;
        UnaryOperator<TextureAtlasSprite> colorSwapper = null;

        if (world.getBlockEntity(pos) instanceof IDyedBuffer be) {
            colorSwapper = getSwapper(be.getColor());
        }

        if (world.getBlockEntity(pos) instanceof IMaterialAdaptingBuffer be) {
            materialSwapper = getSwapper(be.getMaterial());
        }

        UnaryOperator<TextureAtlasSprite> swapper = materialSwapper != null || colorSwapper != null
            ? combineSwappers(materialSwapper, colorSwapper) : null;

        if (swapper == null && !isDiagonal) {
            model.collectParts(random, parts);
            return;
        }
        addSwappedParts(model, random, swapper, null, isDiagonal ? BufferModel::rotateDiagonal : null, parts);
    }

    private static Matrix3f getDiagonalRotationMatrix() {
        Matrix3f matrix = new Matrix3f();
        matrix.mul(Axis.YP.rotationDegrees(45).get(new Matrix3f()));
        return matrix;
    }
}
