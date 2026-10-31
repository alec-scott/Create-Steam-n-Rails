package com.railwayteam.railways.port.minecraft;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/**
 * PORT-NOTE(26.1): Create Fly saves trains and carriages through {@code encode}/{@code decode} on {@link DynamicOps}
 * map builders rather than {@link CompoundTag}s. These helpers let Railways keep writing its extra fields as NBT.
 */
public final class DynamicNbtCompat {
    private DynamicNbtCompat() {}

    /** Adds every entry of {@code tag} to {@code map} as a top-level field. */
    public static <T> void addAll(RecordBuilder<T> map, DynamicOps<T> ops, CompoundTag tag) {
        for (String key : tag.keySet())
            map.add(key, Dynamic.convert(NbtOps.INSTANCE, ops, tag.get(key)));
    }

    /** Reads the given top-level fields of an encoded map back into a {@link CompoundTag}; missing keys are skipped. */
    public static <T> CompoundTag read(DynamicOps<T> ops, T input, String... keys) {
        CompoundTag tag = new CompoundTag();
        MapLike<T> map = ops.getMap(input).result().orElse(null);
        if (map == null)
            return tag;
        for (String key : keys) {
            T value = map.get(key);
            if (value != null)
                tag.put(key, Dynamic.convert(ops, NbtOps.INSTANCE, value));
        }
        return tag;
    }
}
