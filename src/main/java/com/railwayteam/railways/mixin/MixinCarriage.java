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

package com.railwayteam.railways.mixin;

import com.railwayteam.railways.port.minecraft.NbtCompat;

import com.railwayteam.railways.port.create.track.TrackMaterialData;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import com.railwayteam.railways.mixin_interfaces.IExtraTrainData;
import com.railwayteam.railways.port.minecraft.DynamicNbtCompat;
import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.mixin_interfaces.CarriageBogeyUtils;
import com.railwayteam.railways.mixin_interfaces.ICarriageBufferDistanceTracker;
import com.railwayteam.railways.mixin_interfaces.ICarriageConductors;
import com.railwayteam.railways.registry.CRTrackMaterials;
import com.railwayteam.railways.util.MixinVariables;
import com.zurrtum.create.content.trains.entity.Carriage;
import com.zurrtum.create.content.trains.entity.CarriageBogey;
import com.zurrtum.create.content.trains.entity.CarriageContraption;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.entity.TravellingPoint;
import com.zurrtum.create.content.trains.graph.DimensionPalette;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(value = Carriage.class, remap = false)
public abstract class MixinCarriage implements ICarriageConductors, ICarriageBufferDistanceTracker, IExtraTrainData {

    @Shadow public Train train;

    @Shadow public abstract CarriageBogey leadingBogey();

    @Shadow public abstract CarriageBogey trailingBogey();

    @Shadow public abstract TravellingPoint getLeadingPoint();

    @Shadow public abstract TravellingPoint getTrailingPoint();

    private final List<UUID> railways$controllingConductors = new ArrayList<>();

    @Override
    public List<UUID> railways$getControllingConductors() {
        return railways$controllingConductors;
    }

    @Unique
    private @Nullable Integer railways$leadingBufferDistance = null;
    @Unique
    private @Nullable Integer railways$trailingBufferDistance = null;

    @Override
    public @Nullable Integer railways$getLeadingDistance() {
        return railways$leadingBufferDistance;
    }

    @Override
    public @Nullable Integer railways$getTrailingDistance() {
        return railways$trailingBufferDistance;
    }

    @Override
    public void railways$setLeadingDistance(int distance) {
        railways$leadingBufferDistance = distance;
    }

    @Override
    public void railways$setTrailingDistance(int distance) {
        railways$trailingBufferDistance = distance;
    }

    @WrapOperation(method = "updateConductors", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/trains/entity/CarriageContraptionEntity;checkConductors()Lcom/zurrtum/create/catnip/data/Couple;"))
    private Couple<Boolean> addControllingConductors(CarriageContraptionEntity instance, Operation<Couple<Boolean>> original) {
        railways$controllingConductors.clear();
        if (instance.getContraption() instanceof CarriageContraption cc) {
            for (Entity passenger : instance.getPassengers()) {
                if (!(passenger instanceof ConductorEntity))
                    continue;
                BlockPos seatOf = cc.getSeatOf(passenger.getUUID());
                if (seatOf == null)
                    continue;
                Couple<Boolean> validSides = cc.conductorSeats.get(seatOf);
                if (validSides == null)
                    continue;
                if (validSides.getFirst() || validSides.getSecond())
                    railways$controllingConductors.add(passenger.getUUID());
            }
        }
        return original.call(instance);
    }

    // PORT-NOTE(26.1): Create Fly saves carriages with encode/decode on DynamicOps (write/read are unused), so the
    // extra fields are added to the encoded map and read back from it.
    @WrapOperation(method = "encode", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/RecordBuilder;build(Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
    private static DataResult<Object> railways$encodeExtraData(RecordBuilder<Object> map, Object empty, Operation<DataResult<Object>> original,
                                                               @Local(argsOnly = true) Carriage input, @Local(argsOnly = true) DynamicOps<Object> ops) {
        CompoundTag tag = new CompoundTag();
        ((IExtraTrainData) input).railways$writeExtraData(tag);
        DynamicNbtCompat.addAll(map, ops, tag);
        return original.call(map, empty);
    }

    @Inject(method = "decode", at = @At("RETURN"))
    private static void railways$decodeExtraData(DynamicOps<Object> ops, Object input, TrackGraph graph, DimensionPalette dimensions,
                                                 CallbackInfoReturnable<Carriage> cir) {
        ((IExtraTrainData) cir.getReturnValue()).railways$readExtraData(
            DynamicNbtCompat.read(ops, input, "ControllingConductors", "LeadingBufferDistance", "TrailingBufferDistance"));
    }

    @Override
    public void railways$writeExtraData(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (UUID uuid : railways$controllingConductors) {
            CompoundTag uuidTag = new CompoundTag();
            NbtCompat.putUUID(uuidTag, "UUID", uuid);
            listTag.add(uuidTag);
        }
        tag.put("ControllingConductors", listTag);

        if (railways$leadingBufferDistance != null)
            tag.putInt("LeadingBufferDistance", railways$leadingBufferDistance);
        if (railways$trailingBufferDistance != null)
            tag.putInt("TrailingBufferDistance", railways$trailingBufferDistance);
    }

    @Override
    public void railways$readExtraData(CompoundTag tag) {
        Carriage carriage = (Carriage) (Object) this;
        List<UUID> controllingConductors = ((ICarriageConductors) carriage).railways$getControllingConductors();
        controllingConductors.clear();
        if (tag.contains("ControllingConductors")) {
            ListTag listTag = tag.getListOrEmpty("ControllingConductors");
            for (Tag item : listTag) {
                if (item instanceof CompoundTag uuidTag && NbtCompat.hasUUID(uuidTag, "UUID")) {
                    controllingConductors.add(NbtCompat.getUUID(uuidTag, "UUID"));
                }
            }
        }

        if (tag.contains("LeadingBufferDistance"))
            ((ICarriageBufferDistanceTracker) carriage).railways$setLeadingDistance(tag.getIntOr("LeadingBufferDistance", 0));

        if (tag.contains("TrailingBufferDistance"))
            ((ICarriageBufferDistanceTracker) carriage).railways$setTrailingDistance(tag.getIntOr("TrailingBufferDistance", 0));
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void markTravelStart(Level level, TrackGraph graph, double distance, TravellingPoint toFollowForward,
                                 TravellingPoint toFollowBackward, int type, CallbackInfoReturnable<Double> cir) {
        if (train.navigation.isActive()) // only do automatic stuff when automatically operated
            MixinVariables.trackEdgeCarriageTravelling = true;
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void markTravelEnd(Level level, TrackGraph graph, double distance, TravellingPoint toFollowForward,
                               TravellingPoint toFollowBackward, int type, CallbackInfoReturnable<Double> cir) {
        MixinVariables.trackEdgeCarriageTravelling = false;
    }

    @ModifyExpressionValue(method = "isOnIncompatibleTrack", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/trains/bogey/AbstractBogeyBlock;isOnIncompatibleTrack(Lcom/zurrtum/create/content/trains/entity/Carriage;Z)Z", ordinal = 0))
    private boolean allowUniversalTrackLeading(boolean original) {
        return railways$isIncompatible(original, true);
    }

    @ModifyExpressionValue(method = "isOnIncompatibleTrack", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/trains/bogey/AbstractBogeyBlock;isOnIncompatibleTrack(Lcom/zurrtum/create/content/trains/entity/Carriage;Z)Z", ordinal = 1))
    private boolean allowUniversalTrackTrailing(boolean original) {
        return railways$isIncompatible(original, false);
    }

    @Unique
    private boolean railways$isIncompatible(boolean original, boolean leading) {
        CarriageBogey bogey = leading ? leadingBogey() : trailingBogey();
        TravellingPoint point = leading ? getLeadingPoint() : getTrailingPoint();
        if (point.edge == null)
            return false;
        if (TrackMaterialData.of(point.edge.getTrackMaterial()).trackType == CRTrackMaterials.CRTrackType.UNIVERSAL)
            return false;
        if (CarriageBogeyUtils.getType(bogey).getTrackType(bogey.getStyle()) == CRTrackMaterials.CRTrackType.UNIVERSAL.id)
            return false;
        return original;
    }

    @Inject(method = "manageEntities", at = @At("HEAD"), cancellable = true)
    private void allowTravellingWithoutLevel(Level level, CallbackInfo ci) {
        if (level == null) ci.cancel();
    }
}
