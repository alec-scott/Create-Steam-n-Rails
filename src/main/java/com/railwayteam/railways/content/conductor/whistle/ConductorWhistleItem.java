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

package com.railwayteam.railways.content.conductor.whistle;

import net.minecraft.world.entity.EquipmentSlot;
import com.railwayteam.railways.util.CRValueIO;
import com.zurrtum.create.AllSchedules;
import com.railwayteam.railways.registry.CRBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.core.component.DataComponents;
import com.railwayteam.railways.util.ItemUtils;
import com.railwayteam.railways.port.minecraft.NbtCompat;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import com.railwayteam.railways.config.CRConfigs;
import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.mixin.AccessorCarriage;
import com.railwayteam.railways.mixin.AccessorScheduleRuntime;
import com.railwayteam.railways.mixin_interfaces.ICarriageConductors;
import com.railwayteam.railways.registry.CRBlocks;
import com.railwayteam.railways.registry.CREntities;
import com.railwayteam.railways.registry.CRSounds;
import com.railwayteam.railways.util.TextUtils;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.Create;
import com.zurrtum.create.content.kinetics.deployer.DeployerFakePlayer;
import com.zurrtum.create.content.trains.GlobalRailwayManager;
import com.zurrtum.create.content.trains.entity.Carriage;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.graph.EdgePointType;
import com.zurrtum.create.content.trains.schedule.Schedule;
import com.zurrtum.create.content.trains.schedule.ScheduleEntry;
import com.zurrtum.create.content.trains.schedule.condition.ScheduledDelay;
import com.zurrtum.create.content.trains.schedule.destination.DestinationInstruction;
import com.zurrtum.create.content.trains.station.StationBlock;
import com.zurrtum.create.content.trains.station.StationBlockEntity;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.client.content.trains.track.TrackBlockOutline;
import com.zurrtum.create.content.trains.track.TrackTargetingBlockItem;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConductorWhistleItem extends TrackTargetingBlockItem {

    public static final String SPECIAL_MARKER = "<ConductorFlag>";

    public ConductorWhistleItem(Block block, Item.Properties properties) {
        super(block, properties, EdgePointType.STATION);
    }
    // PORT-NOTE(26.1): curve placement (1.20 useOnCurve returned false, "not worth the effort") is refused by
    // MixinCurvedTrackInteraction

    private static InteractionResult fail(Player player, String message) {
        player.sendOverlayMessage(Component.translatable("railways.whistle.failure."+message).withStyle(ChatFormatting.RED));
        player.sendSystemMessage(Component.translatable("railways.whistle.failure."+message).withStyle(ChatFormatting.RED));
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        CompoundTag tag = ItemUtils.getCustomData(stack);
        if (tag != null && NbtCompat.hasUUID(tag, "SelectedTrain") && NbtCompat.hasUUID(tag, "SelectedConductor")) {
            UUID trainId = NbtCompat.getUUID(tag, "SelectedTrain");
            UUID conductorId = NbtCompat.getUUID(tag, "SelectedConductor");
            String trainName = "NOT FOUND";
            GlobalRailwayManager railways = Create.RAILWAYS.sided(null); // tooltips are client side
            if (railways != null && railways.trains.containsKey(trainId))
                trainName = railways.trains.get(trainId).name.getString();

            if (CRConfigs.client().modernWhistleTooltip.get()) {
                tooltip.accept(TextUtils.translateWithFormatting("railways.whistle.tool.bound2", conductorId.toString().substring(0, 5), trainName, trainId.toString().substring(0, 5)));
            } else {
                tooltip.accept(Component.translatable("railways.whistle.tool.bound").withStyle(ChatFormatting.DARK_GREEN));
                tooltip.accept(TextUtils.translateWithFormatting("railways.whistle.tool.conductor_id", conductorId.toString().substring(0, 5)));
                tooltip.accept(TextUtils.translateWithFormatting("railways.whistle.tool.train_id", trainName, trainId.toString().substring(0, 5)));
                tooltip.accept(Component.translatable("railways.whistle.tool.bound_usage"));
                tooltip.accept(Component.translatable("railways.whistle.tool.bound_auto_usage"));
                tooltip.accept(Component.translatable("railways.whistle.tool.bound_auto_clear"));
            }
        } else {
            if (CRConfigs.client().modernWhistleTooltip.get()) {
                tooltip.accept(TextUtils.translateWithFormatting("railways.whistle.tool.not_bound2"));
            } else {
                tooltip.accept(Component.translatable("railways.whistle.tool.not_bound").withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack pStack, @NotNull Player pPlayer,
                                                           @NotNull LivingEntity pInteractionTarget, @NotNull InteractionHand pUsedHand) {
        if (pPlayer.level.isClientSide())
            return InteractionResult.PASS;
        if (pInteractionTarget instanceof ConductorEntity conductor && conductor.getVehicle() instanceof CarriageContraptionEntity cce) {
            Train train = cce.getCarriage().train;
            if (train.owner == pPlayer.getUUID() || !CRConfigs.server().conductors.whistleRequiresOwning.get()) {
                CompoundTag stackTag = ItemUtils.getCustomData(pStack);
                NbtCompat.putUUID(stackTag, "SelectedTrain", train.id);
                NbtCompat.putUUID(stackTag, "SelectedConductor", conductor.getUUID());
                stackTag.putByte("SelectedColor", conductor.getEntityData().get(ConductorEntity.COLOR));
                pPlayer.sendOverlayMessage(Component.translatable("railways.whistle.set"));
                ItemUtils.setCustomData(pStack, stackTag);
                pPlayer.setItemInHand(pUsedHand, pStack);
                AllSoundEvents.PECULIAR_BELL_USE.play(pPlayer.level, null, conductor.getX(), conductor.getY(), conductor.getZ(), .5f, 1.1f);
                return InteractionResult.SUCCESS;
            } else {
                pPlayer.sendOverlayMessage(Component.translatable("railways.whistle.not_owner").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
        }
        return super.interactLivingEntity(pStack, pPlayer, pInteractionTarget, pUsedHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        ItemStack stack = pContext.getItemInHand();
        BlockPos pos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        BlockState state = level.getBlockState(pos);
        Player player = pContext.getPlayer();
        CompoundTag stackTag = ItemUtils.getCustomData(stack);
        if (!NbtCompat.hasUUID(stackTag, "SelectedTrain")) return InteractionResult.FAIL;
        UUID trainId = NbtCompat.getUUID(stackTag, "SelectedTrain");
        Train train = Create.RAILWAYS.trains.get(trainId);

        if (player == null || train == null)
            return InteractionResult.FAIL;

        if (player instanceof DeployerFakePlayer && state.getBlock() instanceof AirBlock && train.runtime.isAutoSchedule) {train.runtime.discardSchedule();}


        if (player.isSteppingCarefully() && stack.has(DataComponents.CUSTOM_DATA)) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;
            player.sendOverlayMessage(Component.translatable("railways.whistle.clear"));
            stack.remove(DataComponents.CUSTOM_DATA);
            AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1, .5f);
            return InteractionResult.SUCCESS;
        }

        if (state.getBlock() instanceof StationBlock || state.getBlock() instanceof ITrackBlock) {
            level.playSound(null, pos, CRSounds.CONDUCTOR_WHISTLE.get(), SoundSource.BLOCKS, 0.3f, 1f);
            if (level.isClientSide())
                return InteractionResult.SUCCESS;


            String stationName = "";

            if (!NbtCompat.hasUUID(stackTag, "SelectedTrain") || !NbtCompat.hasUUID(stackTag, "SelectedConductor"))
                return fail(player, "not_bound");

            UUID conductorId = NbtCompat.getUUID(stackTag, "SelectedConductor");


            if (!Create.RAILWAYS.trains.containsKey(trainId))
                return fail(player, "train_missing");


            boolean foundConductor = false;
            Carriage conductorCarriage = null;
            for (Carriage carriage : train.carriages) {
                if (((ICarriageConductors) carriage).railways$getControllingConductors().contains(conductorId)) {
                    foundConductor = true;
                    conductorCarriage = carriage;
                    break;
                }
            }

            if (!foundConductor)
                return fail(player, "conductor_missing");


            if (state.getBlock() instanceof ITrackBlock track) {
                Vec3 lookAngle = player.getLookAngle();
                boolean front = track.getNearestTrackAxis(level, pos, state, lookAngle)
                        .getSecond() == Direction.AxisDirection.POSITIVE;

                stackTag.put("SelectedPos", NbtCompat.writeBlockPos(pos));
                stackTag.putBoolean("SelectedDirection", front);
                stackTag.remove("Bezier");
                ItemUtils.setCustomData(stack, stackTag);

                EdgePointType<?> type = getType(stack);
                MutableObject<OverlapResult> result = new MutableObject<>(null);
                withGraphLocation(level, pos, front, null, type, (overlap, location) -> result.setValue(overlap));

                if (result.getValue().feedback != null) {
                    player.sendOverlayMessage(CreateLang.translateDirect(result.getValue().feedback)
                            .withStyle(ChatFormatting.RED));
                    AllSoundEvents.DENY.play(level, null, pos, .5f, 1);
                    return InteractionResult.FAIL;
                }

                Direction[] directions = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};
                Direction successDirection = null;
                for (Direction direction : directions) {
                    BlockPos placePos = pos.relative(direction);
                    Vec3 hitPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                            .add(direction.getStepX() * 0.5, direction.getStepY() * 0.5, direction.getStepZ() * 0.5);
                    BlockPlaceContext ctx = new BlockPlaceContext(
                            player, pContext.getHand(), stack, new BlockHitResult(hitPos, direction.getOpposite(), placePos, false)
                    );
                    if (level.getBlockState(placePos).canBeReplaced(ctx)) {
                        successDirection = direction;
                        break;
                    }
                }

                if (successDirection == null) {
                    stackTag.remove("SelectedPos");
                    stackTag.remove("SelectedDirection");
                    ItemUtils.setCustomData(stack, stackTag);
                    return fail(player, "no_space");
                }

                BlockPos placePos = pos.relative(successDirection);

                stationName = SPECIAL_MARKER + placePos.toShortString();

                BlockState placeState = CRBlocks.CONDUCTOR_WHISTLE_FLAG.getDefaultState();
                level.setBlock(placePos, placeState, 11);
                CompoundTag teTag = new CompoundTag();
                teTag.putString("Name", stationName);
                teTag.putByte("SelectedColor", stackTag.getByteOr("SelectedColor", (byte) 0));
                teTag.putBoolean("TargetDirection", stackTag.getBooleanOr("SelectedDirection", false));
                BlockPos selectedPos = NbtCompat.readBlockPos(stackTag.getCompoundOrEmpty("SelectedPos"));
                // PORT-NOTE(26.1): Create Fly's TrackTargetingBehaviour reads TargetTrack with BlockPos.CODEC, and block
                // entity data for placement is the BLOCK_ENTITY_DATA component (was the BlockEntityTag stack NBT)
                teTag.store("TargetTrack", BlockPos.CODEC, selectedPos.subtract(placePos));
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.<BlockEntityType<?>>of(CRBlockEntities.CONDUCTOR_WHISTLE_FLAG.get(), teTag));

                updateCustomBlockEntityTag(placePos, level, player, stack, placeState);
                stack.remove(DataComponents.BLOCK_ENTITY_DATA);
                stackTag.remove("SelectedPos");
                stackTag.remove("SelectedDirection");
                ItemUtils.setCustomData(stack, stackTag);

            }

            else if (level.getBlockEntity(pos) instanceof StationBlockEntity stationBe) {
                stationName = Objects.requireNonNull(stationBe.getStation()).name;
            }

            // have to own train if: non-auto schedule is in progress
            if (CRConfigs.server().conductors.whistleRequiresOwning.get() && train.runtime.getSchedule() != null && !train.runtime.completed && !train.runtime.isAutoSchedule && train.getOwner(level) != player) {
                stackTag.remove("SelectedPos");
                stackTag.remove("SelectedDirection");
                ItemUtils.setCustomData(stack, stackTag);
                return fail(player, "not_owner");
            }

            if (train.runtime.getSchedule() != null && !train.runtime.isAutoSchedule) {
                ItemStack scheduleStack = train.runtime.returnSchedule(level.registryAccess());
                if (!scheduleStack.isEmpty()) {
                    for (CompoundTag passengerTag : ((AccessorCarriage) conductorCarriage).getSerialisedPassengers().values()) {
                        if (passengerTag.contains("PlayerPassenger")) continue;
                        if (passengerTag.contains("id") && CREntities.CONDUCTOR.getId().equals(Identifier.parse(passengerTag.getStringOr("id", "")))) {
                            // It is a conductor
                            if (NbtCompat.hasUUID(passengerTag, "UUID") && NbtCompat.getUUID(passengerTag, "UUID").equals(conductorId)) {
                                // It is the targeted conductor
                                // Place the schedule in the conductor
                                ListTag schedulesList;
                                if (!passengerTag.contains("heldSchedules")) {
                                    schedulesList = new ListTag();
                                    passengerTag.put("heldSchedules", schedulesList);
                                } else {
                                    schedulesList = passengerTag.getListOrEmpty("heldSchedules");
                                }
                                schedulesList.add(CRValueIO.saveItem(level.registryAccess(), scheduleStack));
                                scheduleStack.setCount(0);
                                break;
                            }
                        }
                    }
                    if (!scheduleStack.isEmpty()) {
                        // Try if the conductor is simply loaded
                        conductorCarriage.forEachPresentEntity(cce -> {
                            if (!scheduleStack.isEmpty()) {
                                for (Entity passenger : cce.getPassengers()) {
                                    if (passenger instanceof ConductorEntity conductorEntity && passenger.getUUID().equals(conductorId)) {
                                        conductorEntity.addSchedule(scheduleStack);
                                        scheduleStack.setCount(0);
                                        break;
                                    }
                                }
                            }
                        });
                    }
                    if (!scheduleStack.isEmpty() && !player.addItem(scheduleStack)) // fallback, probably should never be called. Keep it *just in case*
                        player.drop(scheduleStack, false);
                }
            }


            player.sendOverlayMessage(Component.translatable("railways.whistle.success"));
            AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1, 1);

            Schedule schedule = new Schedule();
            ScheduleEntry entry = new ScheduleEntry();
            DestinationInstruction instruction = new DestinationInstruction(AllSchedules.DESTINATION);
            ScheduledDelay condition = new ScheduledDelay(AllSchedules.DELAY);
            condition.getData().putInt("Value", 0);
            instruction.getData().putString("Text", stationName);
            entry.instruction = instruction;
            if (entry.conditions.isEmpty())
                entry.conditions.add(new ArrayList<>());
            entry.conditions.get(0).add(condition);
            schedule.entries.add(entry);
            schedule.cyclic = false;
            train.runtime.discardSchedule();
            train.runtime.setSchedule(schedule, true);
            ((AccessorScheduleRuntime) train.runtime).setCooldown(10);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    // PORT-NOTE(26.1): 26.1 passes the equipment slot (null outside equipment slots) instead of slot id + selected
    public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel serverLevel, @NotNull Entity entity, @Nullable EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND
            && (serverLevel.getGameTime() + entity.hashCode()) % CRConfigs.server().conductors.whistleRebindRate.get() == 0) {
            CompoundTag tag = ItemUtils.getCustomData(stack);
            if (tag != null && NbtCompat.hasUUID(tag, "SelectedTrain") && NbtCompat.hasUUID(tag, "SelectedConductor")) {
                UUID trainId = NbtCompat.getUUID(tag, "SelectedTrain");
                UUID conductorId = NbtCompat.getUUID(tag, "SelectedConductor");

                if (serverLevel.getEntity(conductorId) instanceof ConductorEntity conductor) {
                    if (conductor.getVehicle() instanceof CarriageContraptionEntity cce && !trainId.equals(cce.trainId)) {
                        NbtCompat.putUUID(tag, "SelectedTrain", cce.trainId);
                        ItemUtils.setCustomData(stack, tag);
                    }
                }
            }
        }
    }
}
