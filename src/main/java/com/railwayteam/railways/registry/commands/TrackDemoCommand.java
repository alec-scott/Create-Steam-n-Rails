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

package com.railwayteam.railways.registry.commands;

import com.railwayteam.railways.port.create.track.TrackMaterialData;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Optional;

public class TrackDemoCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("track_demo")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> {
                    BlockPos origin = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                    BlockPos.MutableBlockPos pos = origin.mutable();
                    for (TrackMaterial material : TrackMaterial.ALL.values()) {
                        ServerLevel level = ctx.getSource().getLevel();

                        BlockState trackState = material.getBlock().defaultBlockState();
                        level.setBlockAndUpdate(pos, trackState);

                        // PORT-NOTE(fabric): sleeper ingredients are lazy Optionals now (SoftIngredient is gone).
                        TrackMaterialData.of(material).sleeperIngredient()
                            .flatMap(ingredient -> ingredient.items().findFirst())
                            .map(holder -> holder.value())
                            .filter(item -> item instanceof BlockItem)
                            .ifPresent(item -> {
                                BlockState baseState = ((BlockItem) item).getBlock().defaultBlockState();
                                if (baseState.hasProperty(SlabBlock.TYPE))
                                    baseState = baseState.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
                                level.setBlockAndUpdate(pos.east(3), baseState);
                                level.setBlockAndUpdate(pos.east(3).above(), baseState);
                            });

                        pos.move(0, 0, 1);
                    }

                    ctx.getSource().sendSuccess(() -> Component.literal("Placed tracks"), true);
                    return 1;
                }));
    }
}
