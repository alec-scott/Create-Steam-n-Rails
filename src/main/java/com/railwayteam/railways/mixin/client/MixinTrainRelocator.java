/*
 * Steam 'n' Rails
 * Copyright (c) 2026 The Railways Team
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

package com.railwayteam.railways.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.railwayteam.railways.config.CRConfigs;
import com.railwayteam.railways.content.shadow_realm.ShadowRealm;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.infrastructure.packet.c2s.TrainRelocationPacket;
import com.zurrtum.create.client.content.trains.entity.TrainRelocatorClient;
import com.zurrtum.create.infrastructure.component.BezierTrackPointLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

// PORT-NOTE(26.1): retargeted to Create Fly's client-only TrainRelocatorClient
@Mixin(TrainRelocatorClient.class)
public class MixinTrainRelocator {
    @Shadow
    static UUID relocatingTrain;

    @Inject(method = "getRelocating", at = @At("HEAD"), cancellable = true)
    private static void getShadowRelocating(CallbackInfoReturnable<Train> cir) {
        if (ShadowRealm.MARKER.equals(relocatingTrain))
            cir.setReturnValue(ShadowRealm.clientShadowRestoringTrain);
    }

    @Inject(method = "clientTick", at = @At("HEAD"))
    private static void clearShadowRestoringTrain(CallbackInfo ci) {
        if (!ShadowRealm.MARKER.equals(relocatingTrain))
            ShadowRealm.clientShadowRestoringTrain = null;
    }

    @WrapOperation(method = "relocateClient", at = @At(value = "NEW", target = "(Ljava/util/UUID;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/Vec3;IZLcom/zurrtum/create/infrastructure/component/BezierTrackPointLocation;)Lcom/zurrtum/create/infrastructure/packet/c2s/TrainRelocationPacket;"))
    private static TrainRelocationPacket relocateShadowTrain(UUID trainId, BlockPos pos, Vec3 lookAngle, int entityId, boolean direction, BezierTrackPointLocation hoveredBezier, Operation<TrainRelocationPacket> original) {
        if (ShadowRealm.MARKER.equals(trainId) && ShadowRealm.clientShadowRestoringTrain != null) {
            trainId = ShadowRealm.clientShadowRestoringTrain.id;
        }
        return original.call(trainId, pos, lookAngle, entityId, direction, hoveredBezier);
    }

    @WrapOperation(method = {"clientTick", "onClicked"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean unrestrictRange(Vec3 instance, Position pos, double distance, Operation<Boolean> original,
                                           @Local(name = "player") LocalPlayer player) {
        if (ShadowRealm.MARKER.equals(relocatingTrain) || (player.isCreative() && CRConfigs.server().unlimitedCreativeRelocation.get()))
            return true;

        return original.call(instance, pos, distance);
    }
}
