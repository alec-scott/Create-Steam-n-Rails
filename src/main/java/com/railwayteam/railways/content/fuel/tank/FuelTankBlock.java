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

import com.railwayteam.railways.registry.fabric.CRBlockEntitiesImpl;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.content.fluids.transfer.GenericItemEmptying;
import com.zurrtum.create.content.fluids.transfer.GenericItemFilling;
import com.zurrtum.create.foundation.advancement.AdvancementBehaviour;
import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.foundation.blockEntity.ComparatorUtil;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import com.zurrtum.create.foundation.fluid.FluidHelper.FluidExchange;
import com.zurrtum.create.foundation.utility.BlockHelper;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidInventoryProvider;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * PORT-NOTE(fabric): re-derived from Create Fly's {@code FuelTankBlock} (upstream Railways forked Create's), without
 * the creative variant and boiler hooks. Like upstream, only creative players exchange fluids by hand. The quieter
 * batch-placement sound is not ported.
 */
public class FuelTankBlock extends Block implements IWrenchable, IBE<FuelTankBlockEntity>, FluidInventoryProvider<FuelTankBlockEntity> {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

    @Override
    public void setPlacedBy(
        Level pLevel,
        BlockPos pPos,
        BlockState pState,
        @Nullable LivingEntity pPlacer,
        ItemStack pStack
    ) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
    }

    public FuelTankBlock(Properties properties) {
        super(properties.lightLevel(FuelTankBlock::getLight));
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true)
            .setValue(SHAPE, Shape.WINDOW).setValue(LIGHT_LEVEL, 0));
    }

    public static boolean isTank(BlockState state) {
        return state.getBlock() instanceof FuelTankBlock;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock()) {
            return;
        }
        if (moved) {
            return;
        }
        withBlockEntityDo(world, pos, FuelTankBlockEntity::updateConnectivity);

        // updateConnectivity may have changed the in-world block state, which prevents the call to markAndNotifyBlock
        // in net.neoforged.neoforge.common.CommonHooks#onPlaceItemIntoWorld from doing anything
        BlockState newState = world.getBlockState(pos);
        if (state != newState && newState.getBlock() == this) {
            BlockHelper.markAndNotifyBlock(world, pos, world.getChunkAt(pos), oldState, newState, UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(TOP, BOTTOM, SHAPE, LIGHT_LEVEL);
    }

    public static int getLight(BlockState state) {
        return state.getValue(LIGHT_LEVEL);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        withBlockEntityDo(context.getLevel(), context.getClickedPos(), FuelTankBlockEntity::toggleWindows);
        return InteractionResult.SUCCESS;
    }

    static final VoxelShape CAMPFIRE_SMOKE_CLIP = Block.box(0, 4, 0, 16, 16, 16);

    @Override
    public VoxelShape getCollisionShape(
        BlockState pState,
        BlockGetter pLevel,
        BlockPos pPos,
        CollisionContext pContext
    ) {
        if (pContext == CollisionContext.empty()) {
            return CAMPFIRE_SMOKE_CLIP;
        }
        return pState.getShape(pLevel, pPos);
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return Shapes.block();
    }

    @Override
    public FluidInventory getFluidInventory(
        LevelAccessor world,
        BlockPos pos,
        BlockState state,
        FuelTankBlockEntity blockEntity,
        @Nullable Direction context
    ) {
        if (blockEntity.fluidCapability == null) {
            blockEntity.refreshCapability();
        }
        return blockEntity.fluidCapability;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        boolean onClient = level.isClientSide();

        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!player.isCreative()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        FluidExchange exchange = null;
        FuelTankBlockEntity be = ConnectivityHandler.partAt(getBlockEntityType(), level, pos);
        if (be == null) {
            return InteractionResult.FAIL;
        }

        if (!(state.getBlock() instanceof FluidInventoryProvider<?> provider)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        FluidInventory tankCapability = provider.getFluidInventory(state, level, pos, be, null);
        if (tankCapability == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        FluidStack prevFluidInTank = tankCapability.getStack(0).copy();

        if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, be)) {
            exchange = FluidExchange.ITEM_TO_TANK;
        } else if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, be)) {
            exchange = FluidExchange.TANK_TO_ITEM;
        }

        if (exchange == null) {
            if (GenericItemEmptying.canItemBeEmptied(level, stack) || GenericItemFilling.canItemBeFilled(
                level,
                stack
            )) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        SoundEvent soundevent = null;
        BlockState fluidState = null;
        FluidStack fluidInTank = tankCapability.getStack(0);

        if (exchange == FluidExchange.ITEM_TO_TANK) {
            Fluid fluid = fluidInTank.getFluid();
            fluidState = fluid.defaultFluidState().createLegacyBlock();
            soundevent = FluidHelper.getEmptySound(fluidInTank);
        }

        if (exchange == FluidExchange.TANK_TO_ITEM) {
            Fluid fluid = prevFluidInTank.getFluid();
            fluidState = fluid.defaultFluidState().createLegacyBlock();
            soundevent = FluidHelper.getFillSound(prevFluidInTank);
        }

        if (soundevent != null && !onClient) {
            float pitch = Mth.clamp(
                1 - (1f * fluidInTank.getAmount() / (FuelTankBlockEntity.getCapacityMultiplier() * 16)),
                0,
                1
            );
            pitch /= 1.5f;
            pitch += .5f;
            pitch += (level.getRandom().nextFloat() - .5f) / 4f;
            level.playSound(null, pos, soundevent, SoundSource.BLOCKS, .5f, pitch);
        }

        if (!FluidStack.areFluidsAndComponentsEqual(fluidInTank, prevFluidInTank)) {
            FuelTankBlockEntity controllerBE = be.getControllerBE();
            if (controllerBE != null) {
                if (fluidState != null && onClient) {
                    BlockParticleOption blockParticleData = new BlockParticleOption(ParticleTypes.BLOCK, fluidState);
                    float fluidLevel = (float) fluidInTank.getAmount() / tankCapability.getMaxAmountPerStack();

                    //TODO
                    //                    boolean reversed = fluidInTank.getFluid()
                    //                        .getFluidType()
                    //                        .isLighterThanAir();
                    if (false/*reversed*/) {
                        fluidLevel = 1 - fluidLevel;
                    }

                    Vec3 vec = hitResult.getLocation();
                    vec = new Vec3(
                        vec.x,
                        controllerBE.getBlockPos().getY() + fluidLevel * (controllerBE.height - .5f) + .25f,
                        vec.z
                    );
                    Vec3 motion = player.position().subtract(vec).scale(1 / 20f);
                    vec = vec.add(motion);
                    level.addParticle(blockParticleData, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
                    return InteractionResult.SUCCESS;
                }

                controllerBE.sendDataImmediately();
                controllerBE.setChanged();
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<FuelTankBlockEntity> getBlockEntityClass() {
        return FuelTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FuelTankBlockEntity> getBlockEntityType() {
        return CRBlockEntitiesImpl.FUEL_TANK.get();
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        boolean x = mirror == Mirror.FRONT_BACK;
        switch (state.getValue(SHAPE)) {
            case WINDOW_NE:
                return state.setValue(SHAPE, x ? Shape.WINDOW_NW : Shape.WINDOW_SE);
            case WINDOW_NW:
                return state.setValue(SHAPE, x ? Shape.WINDOW_NE : Shape.WINDOW_SW);
            case WINDOW_SE:
                return state.setValue(SHAPE, x ? Shape.WINDOW_SW : Shape.WINDOW_NE);
            case WINDOW_SW:
                return state.setValue(SHAPE, x ? Shape.WINDOW_SE : Shape.WINDOW_NW);
            default:
                return state;
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        for (int i = 0; i < rotation.ordinal(); i++) {
            state = rotateOnce(state);
        }
        return state;
    }

    private BlockState rotateOnce(BlockState state) {
        switch (state.getValue(SHAPE)) {
            case WINDOW_NE:
                return state.setValue(SHAPE, Shape.WINDOW_SE);
            case WINDOW_NW:
                return state.setValue(SHAPE, Shape.WINDOW_NE);
            case WINDOW_SE:
                return state.setValue(SHAPE, Shape.WINDOW_SW);
            case WINDOW_SW:
                return state.setValue(SHAPE, Shape.WINDOW_NW);
            default:
                return state;
        }
    }

    public enum Shape implements StringRepresentable {
        PLAIN, WINDOW, WINDOW_NW, WINDOW_SW, WINDOW_NE, WINDOW_SE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos, Direction direction) {
        return getBlockEntityOptional(worldIn, pos).map(FuelTankBlockEntity::getControllerBE)
            .map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillState())).orElse(0);
    }

}
