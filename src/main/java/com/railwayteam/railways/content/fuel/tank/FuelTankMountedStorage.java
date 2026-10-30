/*
 * Steam 'n' Rails
 * Copyright (c) 2025 The Railways Team
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

package com.railwayteam.railways.content.fuel.tank;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.railwayteam.railways.content.fuel.LiquidFuelTrainHandler;
import com.railwayteam.railways.content.fuel.tank.FuelTankMountedStorage.Handler;
import com.railwayteam.railways.registry.fabric.CRMountedStorageTypesImpl;
import com.zurrtum.create.api.contraption.storage.SyncedMountedStorage;
import com.zurrtum.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import com.zurrtum.create.AllClientHandle;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FuelTankMountedStorage extends WrapperMountedFluidStorage<Handler> implements SyncedMountedStorage {
	public static final MapCodec<FuelTankMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(FuelTankMountedStorage::getCapacity),
			FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(FuelTankMountedStorage::getFluid)
	).apply(i, FuelTankMountedStorage::new));

	private boolean dirty;

	protected FuelTankMountedStorage(int capacity, FluidStack stack) {
		super(CRMountedStorageTypesImpl.FUEL_TANK.get());
		wrapped = new Handler(capacity, stack);
	}

	@Override
	public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		if (be instanceof FuelTankBlockEntity tank && tank.isController()) {
			FluidTank inventory = tank.getTankInventory();
			// capacity shouldn't change, leave it
			inventory.setFluid(this.wrapped.getFluid());
		}
	}

	public FluidStack getFluid() {
		return Objects.requireNonNull(this.wrapped.getFluid());
	}

	public int getCapacity() {
		return this.wrapped.getMaxAmountPerStack();
	}

	@Override
	public boolean isDirty() {
		return this.dirty;
	}

	@Override
	public void markClean() {
		this.dirty = false;
	}

	@Override
	public void markDirty() {
		this.dirty = true;
	}

	@Override
	public void afterSync(Contraption contraption, BlockPos localPos) {
		BlockEntity be = AllClientHandle.INSTANCE.getBlockEntityClientSide(contraption, localPos);
		if (!(be instanceof FuelTankBlockEntity tank))
			return;

		FluidTank inv = tank.getTankInventory();
		inv.setFluid(this.getFluid());
		float fillLevel = inv.getFluid().getAmount() / (float) inv.getMaxAmountPerStack();
		if (tank.getFluidLevel() == null) {
			tank.setFluidLevel(LerpedFloat.linear().startWithValue(fillLevel));
		}
		tank.getFluidLevel().chase(fillLevel, 0.5, LerpedFloat.Chaser.EXP);
	}

	public static FuelTankMountedStorage fromTank(FuelTankBlockEntity tank) {
		// tank has update callbacks, make an isolated copy
		FluidTank inventory = tank.getTankInventory();
		return new FuelTankMountedStorage(inventory.getMaxAmountPerStack(), inventory.getFluid().copy());
	}

	public final class Handler extends FluidTank {
		public Handler(int capacity, FluidStack stack) {
			super(capacity);
			Objects.requireNonNull(stack);
			this.setFluid(stack);
		}

		@Override
		public void markDirty() {
			dirty = true;
		}

		@Override
		public boolean isValid(int slot, FluidStack stack) {
			return LiquidFuelTrainHandler.isFuelForTanks(stack.getFluid());
		}
	}
}