/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2026 The Railways Team
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

package com.railwayteam.railways.content.custom_tracks.casing;

import com.railwayteam.railways.Railways;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.foundation.model.BakedModelHelper;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code baseModel}'s geometry, textured with {@code spriteSourceModel}'s sprite for each side.
 *
 * <p>PORT-NOTE(26.1): BakedModel.getQuads(state, side, random) is gone, so the quads are copied once into fixed
 * parts (both models are state-less here: a partial model and a block's default state).</p>
 */
public class SpriteCopyingBakedModel implements BlockStateModel {

    private final List<BlockStateModelPart> parts = new ArrayList<>();
    private final Material.Baked particleMaterial;
    private final int materialFlags;

    public SpriteCopyingBakedModel(BlockStateModel baseModel, BlockStateModel spriteSourceModel) {
        RandomSource random = RandomSource.create(42L);
        List<BlockStateModelPart> sourceParts = new ArrayList<>();
        spriteSourceModel.collectParts(random, sourceParts);
        List<BlockStateModelPart> baseParts = new ArrayList<>();
        baseModel.collectParts(random, baseParts);

        boolean ao = sourceParts.isEmpty() || sourceParts.getFirst().useAmbientOcclusion();
        for (BlockStateModelPart part : baseParts) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            for (Direction side : Iterate.directions) {
                BakedQuad overrideQuad = findSourceQuad(sourceParts, side);
                for (BakedQuad quad : part.getQuads(side))
                    builder.addCulledFace(side, copySprite(quad, overrideQuad, side));
            }
            BakedQuad overrideQuad = findSourceQuad(sourceParts, null);
            for (BakedQuad quad : part.getQuads(null))
                builder.addUnculledFace(copySprite(quad, overrideQuad, null));
            parts.add(new SimpleModelWrapper(builder.build(), ao, spriteSourceModel.particleMaterial()));
        }
        this.particleMaterial = spriteSourceModel.particleMaterial();
        this.materialFlags = baseModel.materialFlags();
    }

    private static @Nullable BakedQuad findSourceQuad(List<BlockStateModelPart> sourceParts, @Nullable Direction side) {
        for (BlockStateModelPart part : sourceParts) {
            List<BakedQuad> quads = part.getQuads(side);
            if (!quads.isEmpty())
                return quads.getFirst();
        }
        if (side != null) {
            for (BlockStateModelPart part : sourceParts) {
                List<BakedQuad> quads = part.getQuads(null);
                if (!quads.isEmpty())
                    return quads.getFirst();
            }
        }
        return null;
    }

    private BakedQuad copySprite(BakedQuad quad, @Nullable BakedQuad overrideQuad, @Nullable Direction side) {
        if (overrideQuad == null) {
            Railways.LOGGER.error("No overriding sprites found for side " + (side == null ? "null" : side.toString()));
            return quad;
        }
        MaterialInfo info = quad.materialInfo();
        TextureAtlasSprite baseSprite = info.sprite();
        TextureAtlasSprite goalSprite = overrideQuad.materialInfo().sprite();
        return BakedModelHelper.replaceBakedQuadUV(
            quad,
            BakedModelHelper.calcSpriteUv(quad.packedUV0(), baseSprite, goalSprite),
            BakedModelHelper.calcSpriteUv(quad.packedUV1(), baseSprite, goalSprite),
            BakedModelHelper.calcSpriteUv(quad.packedUV2(), baseSprite, goalSprite),
            BakedModelHelper.calcSpriteUv(quad.packedUV3(), baseSprite, goalSprite),
            new MaterialInfo(goalSprite, info.layer(), info.itemRenderType(), info.tintIndex(), true, info.lightEmission())
        );
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.addAll(parts);
    }

    @Override
    public Material.Baked particleMaterial() {
        return particleMaterial;
    }

    @Override
    public int materialFlags() {
        return materialFlags;
    }
}
