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

package com.railwayteam.railways.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.railwayteam.railways.registry.CRBogeyStyles;
import com.zurrtum.create.api.behaviour.EntityBehaviour;
import com.zurrtum.create.content.trains.entity.Carriage;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.client.foundation.entity.behaviour.CarriageParticleBehaviour;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// PORT-NOTE(26.1): Create Fly's CarriageParticles is the CarriageParticleBehaviour entity behaviour
@Mixin(CarriageParticleBehaviour.class)
public class MixinCarriageParticles {
    // PORT-NOTE(26.1): the behaviour is built in the entity constructor, before the carriage is bound, so the bogey
    // style is read when particles are first spawned (null until then)
    @Unique private Boolean railways$isHandcar;

    @WrapWithCondition(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
    ))
    private boolean railways$skipParticlesForHandcar(Level instance, ParticleOptions particleData, double x, double y, double z,
                                                double xSpeed, double ySpeed, double zSpeed, @Local(ordinal = 1) boolean spark) {
        if (railways$isHandcar == null) {
            Carriage carriage = ((CarriageContraptionEntity) ((EntityBehaviour<?>) (Object) this).entity).getCarriage();
            if (carriage == null)
                return true;
            railways$isHandcar = carriage.bogeys.both(b -> b == null || b.getStyle() == CRBogeyStyles.HANDCAR);
        }
        return spark || !railways$isHandcar;
    }
}
