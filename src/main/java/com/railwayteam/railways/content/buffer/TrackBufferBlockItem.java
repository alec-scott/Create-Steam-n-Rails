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

package com.railwayteam.railways.content.buffer;

import com.zurrtum.create.foundation.block.IBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.core.component.DataComponents;
import com.railwayteam.railways.port.minecraft.NbtCompat;

import com.railwayteam.railways.port.create.track.TrackMaterialData;


import com.railwayteam.railways.registry.CRBlocks;
import com.railwayteam.railways.registry.CRTrackMaterials;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.content.trains.graph.EdgePointType;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.client.content.trains.track.TrackBlockOutline;
import com.zurrtum.create.content.trains.track.TrackShape;
import com.zurrtum.create.content.trains.track.TrackTargetingBlockItem;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullBiFunction;
import com.zurrtum.create.catnip.data.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TrackBufferBlockItem extends TrackTargetingBlockItem {
    
    public static <T extends Block> NonNullBiFunction<? super T, Properties, TrackTargetingBlockItem> ofType(EdgePointType<?> type) {
        return (b, p) -> new TrackBufferBlockItem(b, p, type);
    }
    
    public TrackBufferBlockItem(Block pBlock, Properties pProperties, EdgePointType<?> type) {
        super(pBlock, pProperties, type);
    }
    
    @Nullable
    @Override
    protected BlockState getPlacementState(@NotNull BlockPlaceContext context) {
        if (context instanceof BufferBlockPlaceContext bufferContext && bufferContext.overrideBlock != null) {
            BlockState blockState = bufferContext.overrideBlock.getStateForPlacement(context);
            return blockState != null && this.canPlace(context, blockState) ? blockState : null;
        }
        return super.getPlacementState(context);
    }
    
    private static boolean isOkShape(BlockState state) {
        TrackShape shape = state.getValue(TrackBlock.SHAPE);
        return switch (shape) {
            case ZO, XO, PD, ND,
                 TE, TN, TS, TW -> true;
            default -> false;
        };
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        
        if (player == null)
            return InteractionResult.FAIL;
        
        if (state.getBlock() instanceof ITrackBlock track) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;
            
            Vec3 lookAngle = player.getLookAngle();
            Pair<Vec3, AxisDirection> nearestTrackAxis = track.getNearestTrackAxis(level, pos, state, lookAngle);
            boolean front = nearestTrackAxis.getSecond() == AxisDirection.NEGATIVE;
            Vec3 biasedDirection = nearestTrackAxis.getFirst().yRot(10);
            Axis axis = Direction.getApproximateNearest(
                biasedDirection.x,
                biasedDirection.y,
                biasedDirection.z
            ).getAxis();
            EdgePointType<?> type = getType(stack);
            
            MutableObject<OverlapResult> result = new MutableObject<>(null);
            withGraphLocation(level, pos, front, null, type, (overlap, location) -> result.setValue(overlap));
            
            if (result.getValue().feedback != null) {
                player.sendOverlayMessage(CreateLang.translateDirect(result.getValue().feedback)
                        .withStyle(ChatFormatting.RED));
                AllSoundEvents.DENY.play(level, null, pos, .5f, 1);
                return InteractionResult.FAIL;
            }
            if (!isOkShape(state)) {
                player.sendOverlayMessage(Component.translatable("railways.buffer.invalid_shape")
                        .withStyle(ChatFormatting.RED));
                AllSoundEvents.DENY.play(level, null, pos, .5f, 1);
                return InteractionResult.FAIL;
            }
            
            // PORT-NOTE(26.1): 1.20's "BlockEntityTag" stack NBT is the BLOCK_ENTITY_DATA component, typed by the
            // placed block's block entity type; the stack's own data is restored after placing.
            TypedEntityData<BlockEntityType<?>> oldData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            CompoundTag oldTeTag = oldData == null ? null : oldData.copyTagWithoutId();
            
            CompoundTag teTag = new CompoundTag();
            if (oldTeTag != null) {
                if (oldTeTag.contains("Material"))
                    //noinspection DataFlowIssue
                    teTag.put("Material", oldTeTag.get("Material"));
                if (oldTeTag.contains("Color"))
                    //noinspection DataFlowIssue
                    teTag.put("Color", oldTeTag.get("Color"));
            }
            teTag.putBoolean("TargetDirection", front);
            
            BlockPos placedPos = pos.above();
            Direction placeDirection = Direction.UP;
            
            TrackBufferBlock<?> overrideBlock = null;
            if (TrackMaterialData.of(track.getMaterial()).trackType == CRTrackMaterials.CRTrackType.NARROW_GAUGE) {
                overrideBlock = CRBlocks.TRACK_BUFFER_NARROW.get();
            } else if (TrackMaterialData.of(track.getMaterial()).trackType == CRTrackMaterials.CRTrackType.WIDE_GAUGE) {
                overrideBlock = CRBlocks.TRACK_BUFFER_WIDE.get();
            } else if (TrackMaterialData.of(track.getMaterial()).trackType == CRTrackMaterials.CRTrackType.MONORAIL) {
                overrideBlock = CRBlocks.TRACK_BUFFER_MONO.get();
                placedPos = context.getClickedFace() == Direction.DOWN ? pos.below() : pos.above();
                placeDirection = context.getClickedFace();
            }
            
            // Create Fly's TrackTargetingBehaviour reads TargetTrack with BlockPos.CODEC
            teTag.store("TargetTrack", BlockPos.CODEC, pos.subtract(placedPos));
            Block placedBlock = overrideBlock != null ? overrideBlock : getBlock();
            stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.<BlockEntityType<?>>of(((IBE<?>) placedBlock).getBlockEntityType(), teTag));
            
            TrackShape shape = state.getValue(TrackBlock.SHAPE);
            boolean diagonal = shape == TrackShape.PD || shape == TrackShape.ND;
            
            InteractionResult useOn = place(BufferBlockPlaceContext.at(
                    new BlockPlaceContext(context), placedPos, placeDirection,
                    Direction.fromAxisAndDirection(axis, nearestTrackAxis.getSecond()),
                    overrideBlock, diagonal
            ));
            
            if (oldData == null)
                stack.remove(DataComponents.BLOCK_ENTITY_DATA);
            else
                stack.set(DataComponents.BLOCK_ENTITY_DATA, oldData);
            
            return useOn;
        }
        
        return InteractionResult.PASS;
    }
    
    @Environment(EnvType.CLIENT)
    // PORT-NOTE(26.1): no longer an override; called by MixinCurvedTrackInteraction
    public boolean useOnCurve(TrackBlockOutline.BezierPointSelection selection, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        
        if (player != null) {
            
            player.sendOverlayMessage(CreateLang.translateDirect("track_target.invalid")
                    .withStyle(ChatFormatting.RED));
            AllSoundEvents.DENY.play(level, player, player.position(), .5f, 1);
            return false;
        }
        return false;
    }
}