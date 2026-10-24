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
package com.railwayteam.railways.content.fuel.psi;

import com.railwayteam.railways.mixin_interfaces.IContraptionFuel;
import com.zurrtum.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.infrastructure.fluids.CombinedTankWrapper;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import com.zurrtum.create.infrastructure.fluids.SidedFluidInventory;
import com.zurrtum.create.infrastructure.transfer.SlotRangeCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A portable fluid interface that only reaches a contraption's fuel tanks.
 *
 * <p>PORT-NOTE(fabric): re-derived from Create Fly's {@code PortableFluidInterfaceBlockEntity}; the storage comes from
 * {@link IContraptionFuel} instead of all of the contraption's fluids. Exposed in {@code CRBlockEntitiesImpl}.</p>
 */
public class PortableFuelInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {

    public final FluidInventory capability;

    public PortableFuelInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        capability = new InterfaceFluidHandler();
    }

    @Override
    public void startTransferringTo(Contraption contraption, float distance) {
        CombinedTankWrapper fuels = ((IContraptionFuel) contraption).railways$getFluidFuels();
        if (fuels != null)
            ((InterfaceFluidHandler) capability).setInventory(fuels);
        else
            ((InterfaceFluidHandler) capability).setEmpty();
        super.startTransferringTo(contraption, distance);
    }

    @Override
    protected void stopTransferring() {
        ((InterfaceFluidHandler) capability).setEmpty();
        super.stopTransferring();
    }

    public class InterfaceFluidHandler implements SidedFluidInventory {
        private static final FluidTank EMPTY = new FluidTank(0);

        private int[] slots = SlotRangeCache.EMPTY;
        private FluidInventory wrapped = EMPTY;

        @Override
        public int[] getAvailableSlots(@Nullable Direction side) {
            return slots;
        }

        @Override
        public boolean canExtract(int slot, FluidStack stack, Direction dir) {
            if (wrapped == EMPTY) {
                return false;
            }
            return ((SidedFluidInventory) wrapped).canExtract(slot, stack, dir);
        }

        @Override
        public boolean canInsert(int slot, FluidStack stack, @Nullable Direction dir) {
            if (wrapped == EMPTY) {
                return false;
            }
            return ((SidedFluidInventory) wrapped).canInsert(slot, stack, dir);
        }

        public void setInventory(CombinedTankWrapper wrapped) {
            this.wrapped = wrapped;
            slots = wrapped.getAvailableSlots(null);
        }

        public void setEmpty() {
            wrapped = EMPTY;
            slots = SlotRangeCache.EMPTY;
        }

        @Override
        public int size() {
            return slots.length;
        }

        @Override
        public int getMaxAmountPerStack() {
            return wrapped.getMaxAmountPerStack();
        }

        @Override
        public int getMaxAmount(FluidStack stack) {
            return wrapped.getMaxAmount(stack);
        }

        @Override
        public FluidStack getStack(int slot) {
            return wrapped.getStack(slot);
        }

        @Override
        public void setStack(int slot, FluidStack stack) {
            wrapped.setStack(slot, stack);
        }

        @Override
        public int insert(FluidStack stack, int maxAmount, Direction side) {
            int insert = wrapped.insert(stack, maxAmount, side);
            if (insert != 0) {
                markDirty();
            }
            return insert;
        }

        @Override
        public int extract(FluidStack stack, int maxAmount, Direction side) {
            int extract = wrapped.extract(stack, maxAmount, side);
            if (extract != 0) {
                markDirty();
            }
            return extract;
        }

        @Override
        public void markDirty() {
            onContentTransferred();
        }

        @Override
        public java.util.Iterator<FluidStack> iterator(Direction side) {
            return wrapped.iterator(side);
        }
    }

}
