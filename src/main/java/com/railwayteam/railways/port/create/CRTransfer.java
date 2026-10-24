package com.railwayteam.railways.port.create;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.CachedFluidInventoryBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.CachedInventoryBehaviour;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Exposes block entity inventories to Fabric's transfer API the way Create Fly exposes its own.
 *
 * <p>PORT-NOTE(fabric): copies of Create Fly's private {@code AllTransfer.registerItemSide/registerFluidSide}
 * (a candidate for the Create Fly fork, see the CBC port notes).</p>
 */
public final class CRTransfer {
	private CRTransfer() {
	}

	public static <T extends SmartBlockEntity> void registerItemSide(BlockEntityType<T> type, Function<T, @Nullable Container> factory) {
		BlockEntityBehaviour.add(type, (T be) -> new CachedInventoryBehaviour<>(be, factory));
		ItemStorage.SIDED.registerForBlockEntity(CachedInventoryBehaviour::get, type);
	}

	public static <T extends SmartBlockEntity> void registerFluidSide(BlockEntityType<T> type, Function<T, @Nullable FluidInventory> factory) {
		BlockEntityBehaviour.add(type, (T be) -> new CachedFluidInventoryBehaviour<>(be, factory));
		FluidStorage.SIDED.registerForBlockEntity(CachedFluidInventoryBehaviour::get, type);
	}
}
