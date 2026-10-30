package com.railwayteam.railways.port.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Lets Railways' copycat block entities keep their own {@link BlockEntityType}.
 *
 * <p>PORT-NOTE(26.1): Create Fly's {@code CopycatBlockEntity} only has a {@code (pos, state)} constructor that passes
 * Create's {@code COPYCAT} type, and 26.1 validates the block state against the type inside the {@code BlockEntity}
 * constructor. Subclasses call {@code super(CopycatTypeOverride.withType(type, pos), state)};
 * {@code MixinCopycatBlockEntity} hands the type to the super call.</p>
 */
public final class CopycatTypeOverride {
    private static final ThreadLocal<BlockEntityType<?>> TYPE = new ThreadLocal<>();

    private CopycatTypeOverride() {
    }

    public static BlockPos withType(BlockEntityType<?> type, BlockPos pos) {
        TYPE.set(type);
        return pos;
    }

    public static BlockEntityType<?> take(BlockEntityType<?> fallback) {
        @Nullable BlockEntityType<?> override = TYPE.get();
        if (override == null)
            return fallback;
        TYPE.remove();
        return override;
    }
}
