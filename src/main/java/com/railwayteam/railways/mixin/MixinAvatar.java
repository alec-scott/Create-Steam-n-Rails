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

import com.railwayteam.railways.content.conductor.ConductorEntity;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disguised players are conductor sized.
 *
 * <p>PORT-NOTE(26.1): player dimensions, eye height included, come from {@code Avatar.getDefaultDimensions}; 1.20 had
 * separate {@code Player.getDimensions} and {@code getStandingEyeHeight} hooks.</p>
 */
@Mixin(Avatar.class)
public abstract class MixinAvatar {
    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
    private void shrinkConductorPlayer(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof Player player) || !ConductorEntity.isPlayerDisguised(player))
            return;
        if (pose == Pose.SLEEPING || pose == Pose.FALL_FLYING || pose == Pose.SPIN_ATTACK || pose == Pose.SWIMMING || pose == Pose.DYING)
            return;
        EntityDimensions dimensions = cir.getReturnValue();
        // conductor eye height is 1.5 * 0.76, player eye height is 1.62
        cir.setReturnValue(dimensions.scale(1.0f, 1.5f / 1.8f)
            .withEyeHeight(dimensions.eyeHeight() * (1.5f * 0.76f / 1.62f)));
    }
}
