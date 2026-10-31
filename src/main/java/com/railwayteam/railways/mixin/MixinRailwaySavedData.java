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
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.railwayteam.railways.mixin_interfaces.IShadowTrain;
import com.railwayteam.railways.mixin_interfaces.RailwaySavedDataDuck;
import com.zurrtum.create.content.trains.RailwaySavedData;
import com.zurrtum.create.content.trains.entity.Train;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(RailwaySavedData.class)
public class MixinRailwaySavedData implements RailwaySavedDataDuck {
    @Shadow private Map<UUID, Train> trains;

    @Unique
    private Map<UUID, Train> railways$shadowTrains = new HashMap<>();

    @Unique
    private Map<Identifier, UUID> railways$shadowKeys = new HashMap<>();

    @Override
    public Map<UUID, Train> railway$getShadowTrains() {
        return railways$shadowTrains;
    }

    @Override
    public Map<Identifier, UUID> railways$getShadowKeys() {
        return railways$shadowKeys;
    }

    // PORT-NOTE(26.1): Create Fly's RailwaySavedData is a codec (static save/load on DynamicOps); trains are decoded in
    // lambda$load$2 and saved from the third Map.values() call in save.
    @WrapOperation(method = "lambda$load$2", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object railways$deleteEmptyTrains(Map<Object, Object> instance, Object key, Object value, Operation<Object> original) {
        if (value instanceof Train train && train.carriages.isEmpty()) // delete trains with 0 carriages
            return null;
        return original.call(instance, key, value);
    }

    @Inject(method = "load(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", at = @At("RETURN"))
    private static void loadShadowTrains(DynamicOps<Object> ops, Object input, CallbackInfoReturnable<DataResult<Pair<RailwaySavedData, Object>>> cir) {
        RailwaySavedData sd = cir.getReturnValue().result().map(Pair::getFirst).orElse(null);
        if (sd == null) return;

        MixinRailwaySavedData self = (MixinRailwaySavedData) (Object) sd;
        self.railways$shadowTrains = new HashMap<>();
        self.railways$shadowKeys = new HashMap<>();

        var iter = self.trains.values().iterator();
        while (iter.hasNext()) {
            Train train = iter.next();
            Identifier shadowKey = ((IShadowTrain) train).railways$getShadowKey();
            if (shadowKey != null) {
                iter.remove();
                self.railways$shadowTrains.put(train.id, train);
                self.railways$shadowKeys.put(shadowKey, train.id);
            }
        }
    }

    @WrapOperation(
        method = "save",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;values()Ljava/util/Collection;",
            ordinal = 2 // trains
        )
    )
    private static Collection<Object> saveShadowTrains(Map<Object, Object> instance, Operation<Collection<Object>> original,
                                                       @Local(argsOnly = true) RailwaySavedData input) {
        List<Object> all = new ArrayList<>(original.call(instance));
        all.addAll(((RailwaySavedDataDuck) input).railway$getShadowTrains().values());
        return all;
    }
}
