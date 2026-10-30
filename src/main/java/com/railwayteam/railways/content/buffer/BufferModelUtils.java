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

package com.railwayteam.railways.content.buffer;

import com.railwayteam.railways.Railways;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.catnip.render.StitchedSprite;
import com.zurrtum.create.client.foundation.model.BakedModelHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@Environment(EnvType.CLIENT)
public class BufferModelUtils {
    public static final StitchedSprite SPRUCE_PLANKS_TEMPLATE = new StitchedSprite(Identifier.parse("block/spruce_planks"));
    public static final StitchedSprite BIG_BUFFER_TEMPLATE = new StitchedSprite(Railways.asResource("block/buffer/big_buffer"));
    public static final StitchedSprite SMALL_BUFFER_TEMPLATE = new StitchedSprite(Railways.asResource("block/buffer/small_buffer"));
    public static final StitchedSprite SMALL_BUFFER_MONORAIL_TEMPLATE = new StitchedSprite(Railways.asResource("block/buffer/small_buffer_monorail"));
    public static final EnumMap<DyeColor, StitchedSprite> BIG_BUFFER_COLORS = new EnumMap<>(DyeColor.class);
    public static final EnumMap<DyeColor, StitchedSprite> SMALL_BUFFER_COLORS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            BIG_BUFFER_COLORS.put(color, new StitchedSprite(Railways.asResource("block/buffer/big_buffer/big_buffer_" + color.getName())));
            SMALL_BUFFER_COLORS.put(color, new StitchedSprite(Railways.asResource("block/buffer/small_buffer/small_buffer_" + color.getName())));
        }
    }

    public static UnaryOperator<TextureAtlasSprite> getSwapper(@Nullable BlockState planksState) {
        if (planksState == null) return sprite -> null;
        Block planksBlock = planksState.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(planksBlock);
        String path = id.getPath();

        if (path.endsWith("_planks")) {
            return sprite -> {
                if (sprite == SPRUCE_PLANKS_TEMPLATE.get()) {
                    return getSpriteOnSide(planksState, Direction.UP);
                }
                return null;
            };
        } else {
            return sprite -> null;
        }
    }

    @SafeVarargs
    public static UnaryOperator<TextureAtlasSprite> combineSwappers(@Nullable UnaryOperator<TextureAtlasSprite>... swappers) {
        return sprite -> {
            for (UnaryOperator<TextureAtlasSprite> swapper : swappers) {
                if (swapper == null) continue;
                TextureAtlasSprite newSprite = swapper.apply(sprite);
                if (newSprite != null) {
                    return newSprite;
                }
            }
            return null;
        };
    }

    public static UnaryOperator<TextureAtlasSprite> getSwapper(@Nullable DyeColor color) {
        if (color == null) return sprite -> null;
        return sprite -> {
            if (sprite == SMALL_BUFFER_TEMPLATE.get() || sprite == SMALL_BUFFER_MONORAIL_TEMPLATE.get()) {
                return SMALL_BUFFER_COLORS.get(color).get();
            }
            if (sprite == BIG_BUFFER_TEMPLATE.get()) {
                return BIG_BUFFER_COLORS.get(color).get();
            }
            return null;
        };
    }

    /**
     * PORT-NOTE(26.1): replaces the 1.20 FRAPI sprite-replacing wrappers. Copies {@code model}'s parts into
     * {@code output} with sprites swapped, quads failing {@code filter} dropped and {@code transform} applied (a
     * transformed quad loses its cull face).
     */
    public static void addSwappedParts(BlockStateModel model, RandomSource random,
                                       @Nullable UnaryOperator<TextureAtlasSprite> spriteSwapper,
                                       @Nullable Predicate<BakedQuad> filter, @Nullable UnaryOperator<BakedQuad> transform,
                                       List<BlockStateModelPart> output) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            for (Direction cullFace : Iterate.directions) {
                for (BakedQuad quad : process(part.getQuads(cullFace), spriteSwapper, filter, transform)) {
                    if (transform == null)
                        builder.addCulledFace(cullFace, quad);
                    else
                        builder.addUnculledFace(quad);
                }
            }
            process(part.getQuads(null), spriteSwapper, filter, transform).forEach(builder::addUnculledFace);
            output.add(new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleMaterial()));
        }
    }

    private static List<BakedQuad> process(List<BakedQuad> quads, @Nullable UnaryOperator<TextureAtlasSprite> spriteSwapper,
                                           @Nullable Predicate<BakedQuad> filter, @Nullable UnaryOperator<BakedQuad> transform) {
        if (filter != null)
            quads = quads.stream().filter(filter).toList();
        if (spriteSwapper != null)
            quads = BakedModelHelper.swapSprites(quads, spriteSwapper);
        if (transform != null)
            quads = quads.stream().map(transform).toList();
        return quads;
    }

    private static TextureAtlasSprite getSpriteOnSide(BlockState state, Direction side) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        if (model == null)
            return null;
        RandomSource random = RandomSource.create(42L);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(side);
            if (!quads.isEmpty())
                return quads.getFirst().materialInfo().sprite();
        }
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) {
                if (quad.direction() == side)
                    return quad.materialInfo().sprite();
            }
        }
        return model.particleMaterial().sprite();
    }

    // ensure that sprites get loaded
    public static void register() {}
}
