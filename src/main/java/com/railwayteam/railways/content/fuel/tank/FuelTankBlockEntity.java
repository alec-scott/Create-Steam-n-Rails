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
package com.railwayteam.railways.content.fuel.tank;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.catnip.animation.LerpedFloat.Chaser;
import com.railwayteam.railways.content.fuel.LiquidFuelTrainHandler;
import com.railwayteam.railways.content.fuel.tank.FuelTankBlock.Shape;
import com.zurrtum.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import com.zurrtum.create.infrastructure.fluids.BucketFluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static java.lang.Math.abs;

/**
 * A tank that only takes train fuel.
 *
 * <p>PORT-NOTE(fabric): upstream forked Create's fluid tank; this is re-derived from Create Fly's
 * {@code FluidTankBlockEntity} (minus the boiler), with fuel-only insertion. The goggle tooltip is a client behaviour
 * ({@code client/FuelTankTooltip}) and the fluid storage is exposed in {@code CRBlockEntitiesImpl}.</p>
 */
public class FuelTankBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Fluid {

    private static final int MAX_SIZE = 3;

    public @Nullable FluidInventory fluidCapability;
    protected boolean forceFluidLevelUpdate;
    protected FluidTank tankInventory;
    protected @Nullable BlockPos controller;
    protected @Nullable BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected boolean updateCapability;
    public boolean window;
    protected int luminosity;
    protected int width;
    protected int height;

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    // For rendering purposes only
    private @Nullable LerpedFloat fluidLevel;

    public FuelTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tankInventory = createInventory();
        forceFluidLevelUpdate = true;
        updateConnectivity = false;
        updateCapability = false;
        window = true;
        height = 1;
        width = 1;
        refreshCapability();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        level.removeBlockEntity(pos);
        ConnectivityHandler.splitMulti(this);
    }

    protected FluidTank createInventory() {
        return new FuelTankInventory(getCapacityMultiplier());
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide()) {
            return;
        }
        if (!isController()) {
            return;
        }
        ConnectivityHandler.formMulti(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) {
                sendData();
            }
        }

        if (lastKnownPos == null) {
            lastKnownPos = worldPosition;
        } else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
            onPositionChanged();
            return;
        }

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (updateConnectivity) {
            updateConnectivity();
        }
        if (fluidLevel != null) {
            fluidLevel.tickChaser();
        }
    }

    @Override
    @Nullable
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX() && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
        if (level.isClientSide()) {
            invalidateRenderBoundingBox();
        }
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (!hasLevel()) {
            return;
        }

        int luminosity = (int) (newFluidStack.getFluid().defaultFluidState().createLegacyBlock()
            .getLightEmission() / 1.2f);
        boolean reversed = false;
        int maxY = (int) ((getFillState() * height) + 1);

        for (int yOffset = 0; yOffset < height; yOffset++) {
            boolean isBright = reversed ? (height - yOffset <= maxY) : (yOffset < maxY);
            int actualLuminosity = isBright ? luminosity : luminosity > 0 ? 1 : 0;

            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    FuelTankBlockEntity tankAt = ConnectivityHandler.partAt(getType(), level, pos);
                    if (tankAt == null) {
                        continue;
                    }
                    level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState().getBlock());
                    if (tankAt.luminosity == actualLuminosity) {
                        continue;
                    }
                    tankAt.setLuminosity(actualLuminosity);
                }
            }
        }

        if (!level.isClientSide()) {
            setChanged();
            sendData();
        }

        if (isVirtual()) {
            if (fluidLevel == null) {
                fluidLevel = LerpedFloat.linear().startWithValue(getFillState());
            }
            fluidLevel.chase(getFillState(), .5f, Chaser.EXP);
        }
    }

    protected void setLuminosity(int luminosity) {
        if (level.isClientSide()) {
            return;
        }
        if (this.luminosity == luminosity) {
            return;
        }
        this.luminosity = luminosity;
        updateStateLuminosity();
        sendData();
    }

    protected void updateStateLuminosity() {
        if (level.isClientSide()) {
            return;
        }
        int actualLuminosity = luminosity;
        FuelTankBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null || !controllerBE.window) {
            actualLuminosity = 0;
        }
        refreshBlockState();
        BlockState state = getBlockState();
        if (state.getValue(FuelTankBlock.LIGHT_LEVEL) != actualLuminosity) {
            level.setBlock(worldPosition, state.setValue(FuelTankBlock.LIGHT_LEVEL, actualLuminosity), 23);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public FuelTankBlockEntity getControllerBE() {
        if (isController() || !hasLevel()) {
            return this;
        }
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof FuelTankBlockEntity) {
            return (FuelTankBlockEntity) blockEntity;
        }
        return null;
    }

    public void applyFluidTankSize(int blocks) {
        int capacity = blocks * getCapacityMultiplier();
        tankInventory.setCapacity(capacity);
        FluidStack stack = tankInventory.getFluid();
        if (stack.getAmount() > capacity) {
            stack.setAmount(capacity);
            tankInventory.markDirty();
        }
        forceFluidLevelUpdate = true;
    }

    public void removeController(boolean keepFluids) {
        if (level.isClientSide()) {
            return;
        }
        updateConnectivity = true;
        if (!keepFluids) {
            applyFluidTankSize(1);
        }
        controller = null;
        width = 1;
        height = 1;
        onFluidStackChanged(tankInventory.getFluid());

        BlockState state = getBlockState();
        if (FuelTankBlock.isTank(state)) {
            state = state.setValue(FuelTankBlock.BOTTOM, true);
            state = state.setValue(FuelTankBlock.TOP, true);
            state = state.setValue(FuelTankBlock.SHAPE, window ? Shape.WINDOW : Shape.PLAIN);
            getLevel().setBlock(
                worldPosition,
                state,
                Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE
            );
        }

        refreshCapability();
        setChanged();
        sendData();
    }

    public void toggleWindows() {
        FuelTankBlockEntity be = getControllerBE();
        if (be == null) {
            return;
        }
        be.setWindows(!be.window);
    }

    public void sendDataImmediately() {
        syncCooldown = 0;
        queuedSync = false;
        sendData();
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    public void setWindows(boolean window) {
        this.window = window;
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {

                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    BlockState blockState = level.getBlockState(pos);
                    if (!FuelTankBlock.isTank(blockState)) {
                        continue;
                    }

                    Shape shape = Shape.PLAIN;
                    if (window) {
                        // SIZE 1: Every tank has a window
                        if (width == 1) {
                            shape = Shape.WINDOW;
                        }
                        // SIZE 2: Every tank has a corner window
                        if (width == 2) {
                            shape = xOffset == 0 ? zOffset == 0 ? Shape.WINDOW_NW : Shape.WINDOW_SW : zOffset == 0 ? Shape.WINDOW_NE : Shape.WINDOW_SE;
                        }
                        // SIZE 3: Tanks in the center have a window
                        if (width == 3 && abs(abs(xOffset) - abs(zOffset)) == 1) {
                            shape = Shape.WINDOW;
                        }
                    }

                    level.setBlock(
                        pos,
                        blockState.setValue(FuelTankBlock.SHAPE, shape),
                        Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE
                    );
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof FuelTankBlockEntity tankAt) {
                        tankAt.updateStateLuminosity();
                    }
                    level.getChunkSource().getLightEngine().checkBlock(pos);
                }
            }
        }
    }

    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide() && !isVirtual()) {
            return;
        }
        if (controller.equals(this.controller)) {
            return;
        }
        this.controller = controller;
        refreshCapability();
        setChanged();
        sendData();
    }

    public void refreshCapability() {
        fluidCapability = handlerForCapability();
    }

    private FluidInventory handlerForCapability() {
        return isController() ? tankInventory : ((getControllerBE() != null) ? getControllerBE().handlerForCapability() : new FluidTank(
            0));
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (isController()) {
            return super.createRenderBoundingBox().expandTowards(width - 1, height - 1, width - 1);
        } else {
            return super.createRenderBoundingBox();
        }
    }

    @Nullable
    public FuelTankBlockEntity getOtherFuelTankBlockEntity(Direction direction) {
        BlockEntity otherBE = level.getBlockEntity(worldPosition.relative(direction));
        if (otherBE instanceof FuelTankBlockEntity) {
            return (FuelTankBlockEntity) otherBE;
        }
        return null;
    }

    @Override
    protected void read(ValueInput view, boolean clientPacket) {
        super.read(view, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;
        int prevLum = luminosity;

        updateConnectivity = view.getBooleanOr("Uninitialized", false);
        luminosity = view.getIntOr("Luminosity", 0);

        lastKnownPos = view.read("LastKnownPos", BlockPos.CODEC).orElse(null);

        controller = view.read("Controller", BlockPos.CODEC).orElse(null);

        if (isController()) {
            window = view.getBooleanOr("Window", false);
            width = view.getIntOr("Size", 0);
            height = view.getIntOr("Height", 0);
            tankInventory.setCapacity(getTotalTankSize() * getCapacityMultiplier());

            tankInventory.read(view);
        }

        if (view.getBooleanOr("ForceFluidLevel", false) || fluidLevel == null) {
            fluidLevel = LerpedFloat.linear().startWithValue(getFillState());
        }

        updateCapability = true;

        if (!clientPacket) {
            return;
        }

        boolean changeOfController = !Objects.equals(controllerBefore, controller);
        if (changeOfController || prevSize != width || prevHeight != height) {
            if (hasLevel()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
            }
            if (isController()) {
                tankInventory.setCapacity(getCapacityMultiplier() * getTotalTankSize());
            }
            invalidateRenderBoundingBox();
        }
        if (isController()) {
            float fillState = getFillState();
            if (view.getBooleanOr("ForceFluidLevel", false) || fluidLevel == null) {
                fluidLevel = LerpedFloat.linear().startWithValue(fillState);
            }
            fluidLevel.chase(fillState, 0.5f, Chaser.EXP);
        }
        if (luminosity != prevLum && hasLevel()) {
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);
        }

        if (view.getBooleanOr("LazySync", false)) {
            fluidLevel.chase(fluidLevel.getChaseTarget(), 0.125f, Chaser.EXP);
        }
    }

    public float getFillState() {
        return (float) tankInventory.getFluid().getAmount() / tankInventory.getMaxAmountPerStack();
    }

    @Override
    public void write(ValueOutput view, boolean clientPacket) {
        if (updateConnectivity) {
            view.putBoolean("Uninitialized", true);
        }
        if (lastKnownPos != null) {
            view.store("LastKnownPos", BlockPos.CODEC, lastKnownPos);
        }
        if (!isController()) {
            view.store("Controller", BlockPos.CODEC, controller);
        }
        if (isController()) {
            view.putBoolean("Window", window);
            tankInventory.write(view);
            view.putInt("Size", width);
            view.putInt("Height", height);
        }
        view.putInt("Luminosity", luminosity);
        super.write(view, clientPacket);

        if (!clientPacket) {
            return;
        }
        if (forceFluidLevelUpdate) {
            view.putBoolean("ForceFluidLevel", true);
        }
        if (queuedSync) {
            view.putBoolean("LazySync", true);
        }
        forceFluidLevelUpdate = false;
    }

    @Override
    public void writeSafe(ValueOutput view) {
        if (isController()) {
            view.putBoolean("Window", window);
            view.putInt("Size", width);
            view.putInt("Height", height);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
    }

    public FluidTank getTankInventory() {
        return tankInventory;
    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    public static int getMaxSize() {
        return MAX_SIZE;
    }

    public static int getCapacityMultiplier() {
        return AllConfigs.server().fluids.fluidTankCapacity.get() * BucketFluidInventory.CAPACITY;
    }

    public static int getMaxHeight() {
        return AllConfigs.server().fluids.fluidTankMaxHeight.get();
    }

    @Nullable
    public LerpedFloat getFluidLevel() {
        return fluidLevel;
    }

    public void setFluidLevel(LerpedFloat fluidLevel) {
        this.fluidLevel = fluidLevel;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = getBlockState();
        if (FuelTankBlock.isTank(state)) { // safety
            state = state.setValue(FuelTankBlock.BOTTOM, getController().getY() == worldPosition.getY());
            state = state.setValue(FuelTankBlock.TOP, getController().getY() + height - 1 == worldPosition.getY());
            level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
        }
        if (isController()) {
            setWindows(window);
        }
        onFluidStackChanged(tankInventory.getFluid());
        setChanged();
    }

    @Override
    public void setExtraData(@Nullable Object data) {
        if (data instanceof Boolean) {
            window = (boolean) data;
        }
    }

    @Override
    @Nullable
    public Object getExtraData() {
        return window;
    }

    @Override
    @Nullable
    public Object modifyExtraData(@Nullable Object data) {
        if (data instanceof Boolean windows) {
            windows |= window;
            return windows;
        }
        return data;
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        if (longAxis == Direction.Axis.Y) {
            return getMaxHeight();
        }
        return getMaxWidth();
    }

    @Override
    public int getMaxWidth() {
        return MAX_SIZE;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int getTankSize(int tank) {
        return getCapacityMultiplier();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    @Override
    public FluidTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return tankInventory.getFluid().copy();
    }

    @Override
    public boolean matches(FluidStack stack, FluidStack otherStack) {
        return tankInventory.matches(stack, otherStack);
    }

    public class FuelTankInventory extends FluidTank {
        public FuelTankInventory(int capacity) {
            super(capacity);
        }

        @Override
        public void markDirty() {
            onFluidStackChanged(fluid);
        }

        @Override
        public boolean isValid(int slot, FluidStack stack) {
            return LiquidFuelTrainHandler.isFuelForTanks(stack.getFluid());
        }
    }
}
