/*
 * Steam 'n' Rails
 * Copyright (c) 2026 The Railways Team
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

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Bridges Railways' NBT-shaped save code to 26.1's {@link ValueInput}/{@link ValueOutput}.
 *
 * <p>PORT-NOTE(fabric): block entity and entity data no longer hand out a {@link CompoundTag}. Nested compounds and
 * lists are stored through {@link ExtraCodecs#NBT}, so saved data keeps its shape. Adapted from the CBC port's
 * {@code CBCValueIO}.</p>
 */
public final class CRValueIO {
	private CRValueIO() {
	}

	/** Runs a save method against a fresh compound and returns it (for packets and other NBT consumers). */
	public static CompoundTag save(HolderLookup.Provider registries, Consumer<ValueOutput> writer) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		writer.accept(output);
		return output.buildResult();
	}

	/** Wraps a compound for a load method. */
	public static ValueInput input(HolderLookup.Provider registries, CompoundTag tag) {
		return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
	}

	public static boolean contains(ValueInput input, String key) {
		return input.read(key, ExtraCodecs.NBT).isPresent();
	}

	public static CompoundTag getCompound(ValueInput input, String key) {
		return input.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new);
	}

	public static ListTag getList(ValueInput input, String key) {
		return input.read(key, ExtraCodecs.NBT).filter(ListTag.class::isInstance).map(ListTag.class::cast).orElseGet(ListTag::new);
	}

	public static @Nullable UUID getUUID(ValueInput input, String key) {
		return input.read(key, UUIDUtil.CODEC).orElse(null);
	}

	public static void put(ValueOutput output, String key, Tag tag) {
		output.store(key, ExtraCodecs.NBT, tag);
	}

	/** Replaces 1.20 {@code ItemStack.save(CompoundTag)}: an empty stack saves as an empty compound. */
	public static Tag saveItem(HolderLookup.Provider registries, ItemStack stack) {
		return stack.isEmpty() ? new CompoundTag() : ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
	}

	/** Replaces 1.20 {@code ItemStack.of(CompoundTag)}. */
	public static ItemStack parseItem(HolderLookup.Provider registries, Tag tag) {
		if (tag instanceof CompoundTag compound && compound.isEmpty()) return ItemStack.EMPTY;
		return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
	}

	/** Replaces 1.20 {@code NbtUtils.writeBlockPos} (note: 26.1 stores block positions as int arrays). */
	public static Tag writeBlockPos(BlockPos pos) {
		return BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow();
	}

	public static Optional<BlockPos> readBlockPos(CompoundTag tag, String key) {
		return tag.read(key, BlockPos.CODEC);
	}
}
