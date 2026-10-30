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

package com.railwayteam.railways.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.railwayteam.railways.config.CRConfigs;
import com.railwayteam.railways.content.shadow_realm.ShadowRealm;
import com.railwayteam.railways.content.shadow_realm.ShadowRealm.RestorationTarget;
import com.zurrtum.create.AllHandle;
import com.zurrtum.create.infrastructure.packet.c2s.TrainRelocationPacket;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// earlier priority to bypass OPAC protections, which freak out about there being no entity associated with the relocation
// PORT-NOTE(26.1): Create Fly handles the packet in AllHandle.onTrainRelocation (was the packet's handle lambda)
@Mixin(value = AllHandle.class, priority = 500)
public class TrainRelocationPacketMixin {
    @WrapOperation(method = "onTrainRelocation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z", ordinal = 1))
    private static boolean unrestrictRange(Vec3 instance, Position pos, double distance, Operation<Boolean> original,
                                           ServerGamePacketListenerImpl listener, TrainRelocationPacket packet) {
        if (listener.player.isCreative() && CRConfigs.server().unlimitedCreativeRelocation.get())
            return true;

        return original.call(instance, pos, distance);
    }

    @Inject(method = "onTrainRelocation", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private static void relocateShadowTrain(ServerGamePacketListenerImpl listener, TrainRelocationPacket packet, CallbackInfo ci) {
        ServerPlayer sender = listener.player;
        RestorationTarget target = new RestorationTarget(sender.level(), packet.pos(), packet.hoveredBezier(), packet.direction(), packet.lookAngle());
        ShadowRealm.handleTrainRelocationPacket(sender, packet.trainId(), target, ci);
    }
}
