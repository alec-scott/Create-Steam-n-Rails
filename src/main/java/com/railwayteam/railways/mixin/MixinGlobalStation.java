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

package com.railwayteam.railways.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import com.railwayteam.railways.mixin_interfaces.ILimitedGlobalStation;
import com.railwayteam.railways.port.minecraft.DynamicNbtCompat;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.graph.DimensionPalette;
import com.zurrtum.create.content.trains.station.GlobalStation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jetbrains.annotations.Nullable;

@Mixin(value = GlobalStation.class, remap = false)
public abstract class MixinGlobalStation implements ILimitedGlobalStation {
    @Shadow @Nullable public abstract Train getNearestTrain();

    @Shadow @Nullable public abstract Train getImminentTrain();

    private boolean limitEnabled;

    @Override
    public boolean isStationEnabled() {
        return !limitEnabled || (getNearestTrain()) == null;
    }

    @Override
    public Train getDisablingTrain() {
        if (!limitEnabled)
            return null;
        return (getNearestTrain());
    }

    @Override
    public Train orDisablingTrain(Train before, Train except) {
        if (before == null || before == except)
            before = getDisablingTrain();
        return before;
    }

    @Override
    public void setLimitEnabled(boolean limitEnabled) {
        this.limitEnabled = limitEnabled;
    }

    @Override
    public boolean isLimitEnabled() {
        return limitEnabled;
    }

    // PORT-NOTE(26.1): saved data goes through encode/decode on DynamicOps; read/write use ValueInput/ValueOutput
    @Inject(method = "read(Lnet/minecraft/world/level/storage/ValueInput;ZLcom/zurrtum/create/content/trains/graph/DimensionPalette;)V", at = @At("TAIL"))
    private void readLimit(ValueInput nbt, boolean migration, DimensionPalette dimensions, CallbackInfo ci) {
        limitEnabled = nbt.getBooleanOr("LimitEnabled", false);
    }

    @Inject(method = "decode", at = @At("TAIL"))
    private void decodeLimit(DynamicOps<Object> ops, Object input, boolean migration, DimensionPalette dimensions, CallbackInfo ci) {
        limitEnabled = DynamicNbtCompat.read(ops, input, "LimitEnabled").getBooleanOr("LimitEnabled", false);
    }

    @WrapOperation(method = "encode", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/RecordBuilder;build(Lcom/mojang/serialization/DataResult;)Lcom/mojang/serialization/DataResult;"))
    private DataResult<Object> encodeLimit(RecordBuilder<Object> map, DataResult<Object> prefix, Operation<DataResult<Object>> original,
                                           @Local(argsOnly = true) DynamicOps<Object> ops) {
        map.add("LimitEnabled", ops.createBoolean(limitEnabled));
        return original.call(map, prefix);
    }

    @Inject(method = "read(Lnet/minecraft/network/FriendlyByteBuf;Lcom/zurrtum/create/content/trains/graph/DimensionPalette;)V", at = @At("TAIL"), remap = true)
    private void readNetLimit(FriendlyByteBuf buffer, DimensionPalette dimensions, CallbackInfo ci) {
        limitEnabled = buffer.readBoolean();
    }

    @Inject(method = "write(Lnet/minecraft/world/level/storage/ValueOutput;Lcom/zurrtum/create/content/trains/graph/DimensionPalette;)V", at = @At("TAIL"))
    private void writeLimit(ValueOutput nbt, DimensionPalette dimensions, CallbackInfo ci) {
        nbt.putBoolean("LimitEnabled", limitEnabled);
    }

    @Inject(method = "write(Lnet/minecraft/network/FriendlyByteBuf;Lcom/zurrtum/create/content/trains/graph/DimensionPalette;)V", at = @At("TAIL"), remap = true)
    private void writeNetLimit(FriendlyByteBuf buffer, DimensionPalette dimensions, CallbackInfo ci) {
        buffer.writeBoolean(limitEnabled);
    }
}
