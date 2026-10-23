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

package com.railwayteam.railways.util;

import com.railwayteam.railways.content.conductor.ConductorEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Contract;

import java.util.function.Predicate;

public class EntityUtils {
	/**
	 * Create's per-player toolbox data.
	 *
	 * <p>PORT-NOTE(fabric): was {@code getPersistentData(player).getCompound("CreateToolboxData")}; Create Fly keeps it
	 * in synced entity data ({@code AllSynchedDatas.TOOLBOX}). The returned tag is a copy unless written back with
	 * {@link #setToolboxData}.</p>
	 */
	public static CompoundTag getToolboxData(Player player) {
		return com.zurrtum.create.AllSynchedDatas.TOOLBOX.get(player);
	}

	public static void setToolboxData(Player player, CompoundTag data) {
		com.zurrtum.create.AllSynchedDatas.TOOLBOX.set(player, data);
	}

	/**
	 * Gives a player an item. Plays the pickup sound, and drops whatever can't be picked up.
	 */
	public static void givePlayerItem(Player player, ItemStack stack) {
		com.railwayteam.railways.util.fabric.EntityUtilsImpl.givePlayerItem(player, stack);
	}

	public static ServerPlayer createConductorFakePlayer(ServerLevel level, ConductorEntity conductor) {
		return com.railwayteam.railways.util.fabric.EntityUtilsImpl.createConductorFakePlayer(level, conductor);
	}

	public static double getReachDistance(Player player) {
		return com.railwayteam.railways.util.fabric.EntityUtilsImpl.getReachDistance(player);
	}

	/**
	 * Fire a use event.
	 * @return true if the use is allowed, false otherwise
	 */
	@Contract // shut
	public static boolean handleUseEvent(Player player, InteractionHand hand, BlockHitResult hit) {
		return com.railwayteam.railways.util.fabric.EntityUtilsImpl.handleUseEvent(player, hand, hit);
	}

	public static boolean isHolding(Player player, Predicate<ItemStack> predicate) {
		return predicate.test(player.getItemInHand(InteractionHand.MAIN_HAND))
				|| predicate.test(player.getItemInHand(InteractionHand.OFF_HAND));
	}

	public static boolean isHoldingItem(Player player, Predicate<Item> predicate) {
		return isHolding(player, (stack) -> predicate.test(stack.getItem()));
	}
}
