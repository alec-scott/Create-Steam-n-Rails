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

package com.railwayteam.railways.content.buffer;

import net.minecraft.world.level.ScheduledTickAccess;

import net.minecraft.world.level.LevelReader;

import net.minecraft.util.RandomSource;

import com.railwayteam.railways.registry.CRBlocks;
import com.railwayteam.railways.util.AdventureUtils;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class TrackBufferBlock<BE extends TrackBufferBlockEntity> extends HorizontalDirectionalBlock implements IBE<BE>, IWrenchable, ProperWaterloggedBlock {

	public static final BooleanProperty DIAGONAL = BooleanProperty.create("diagonal");

	protected TrackBufferBlock(Properties pProperties) {
		super(pProperties);
		registerDefaultState(defaultBlockState()
				.setValue(FACING, Direction.NORTH)
				.setValue(WATERLOGGED, false)
				.setValue(DIAGONAL, false));
	}

	@Override
	public abstract Class<BE> getBlockEntityClass();

	@Override
	public abstract BlockEntityType<? extends BE> getBlockEntityType();

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, WATERLOGGED, DIAGONAL));
	}


	protected abstract BlockState getCycledStyle(BlockState originalState, Direction targetedFace);

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		if (targetedFace.getAxis() == originalState.getValue(FACING).getAxis()) {
			return getCycledStyle(originalState, targetedFace);
		} else {
			return originalState.setValue(FACING, originalState.getValue(FACING).getOpposite());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public FluidState getFluidState(BlockState state) {
		return fluidState(state);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state != null && context instanceof BufferBlockPlaceContext bufferBlockPlaceContext) {
			state = state.setValue(FACING, bufferBlockPlaceContext.facing).setValue(DIAGONAL, bufferBlockPlaceContext.diagonal);
		}
		return withWater(state, context);
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		updateWater(level, ticks, state, currentPos);
		return state;
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return CRBlocks.TRACK_BUFFER.asStack();
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.empty();
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand,
															 BlockHitResult pHit) {
		if (AdventureUtils.isAdventure(pPlayer))
			return InteractionResult.PASS;
		return onBlockEntityUse(pLevel, pPos, be -> be.applyDyeIfValid(pPlayer.getItemInHand(pHand)));
	}

	public static int getBaseModelYRotationOf(BlockState state) {
		return getBaseModelYRotationOf(state, 0);
	}

	public static int getBaseModelYRotationOf(Direction facing) {
		return getBaseModelYRotationOf(facing, 0);
	}

	public static int getBaseModelYRotationOf(BlockState state, int offset) {
		return getBaseModelYRotationOf(state.getValue(NarrowTrackBufferBlock.FACING), offset);
	}

	public static int getBaseModelYRotationOf(Direction facing, int offset) {
		return (int) (
			(facing.toYRot() + 180) + offset
		) % 360;
	}
}
