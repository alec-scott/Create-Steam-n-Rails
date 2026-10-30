package com.railwayteam.railways.port.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 1.20 NBT helpers that 1.21.5+ removed ({@code CompoundTag.putUUID/getUUID/hasUUID},
 * {@code NbtUtils.writeBlockPos/readBlockPos(CompoundTag)}).
 *
 * <p>PORT-NOTE(26.1): these keep the 1.20 on-disk formats (UUIDs as a 4-int array, block positions as an
 * {@code {X, Y, Z}} compound) so data saved by the 1.20 mod still reads.</p>
 */
public final class NbtCompat {
    private NbtCompat() {
    }

    public static void putUUID(CompoundTag tag, String key, UUID uuid) {
        tag.store(key, UUIDUtil.CODEC, uuid);
    }

    public static boolean hasUUID(CompoundTag tag, String key) {
        return tag.getIntArray(key).map(array -> array.length == 4).orElse(false);
    }

    /** Like 1.20's {@code getUUID}, but {@code null} instead of throwing when absent. */
    public static @Nullable UUID getUUID(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElse(null);
    }

    public static CompoundTag writeBlockPos(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }

    public static BlockPos readBlockPos(CompoundTag tag) {
        return new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
    }
}
