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

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;

public class ItemUtils {
	@Contract // shut
	public static boolean blocksEndermanView(ItemStack stack, Player wearer, EnderMan enderman) {
		return com.railwayteam.railways.util.fabric.ItemUtilsImpl.blocksEndermanView(stack, wearer, enderman);
	}

	public static InteractionHand oppositeHand(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	/**
	 * Railways' own item data, kept in {@code minecraft:custom_data} under its 1.20 NBT keys.
	 *
	 * <p>PORT-NOTE(fabric): item NBT became data components in 1.20.5. Vanilla's world upgrade moves unknown item NBT
	 * into {@code custom_data}, so storing it there keeps old items working. Returns a copy.</p>
	 */
	public static CompoundTag getCustomData(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	/** Replaces the stack's custom data (1.20 {@code setTag}); the component is removed when the tag is empty. */
	public static void setCustomData(ItemStack stack, CompoundTag tag) {
		CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
	}

	/** Edits the stack's custom data; the component is removed when the tag ends up empty. */
	public static void updateCustomData(ItemStack stack, Consumer<CompoundTag> updater) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
	}

	/** PORT-NOTE(fabric): item NBT became data components (1.20.5); copies the component changes. */
	public static void copyStackData(ItemStack source, ItemStack target) {
		target.applyComponents(source.getComponentsPatch());
	}

	public static boolean isUnbreakable(ItemStack stack) {
		if (stack.isEmpty()) return false;
		return stack.has(DataComponents.UNBREAKABLE);
	}
}
