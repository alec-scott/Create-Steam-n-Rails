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

package com.railwayteam.railways.mixin.conductor_possession;

import com.llamalad7.mixinextras.sugar.Local;
import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.content.conductor.ConductorPossessionController;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Lets entities get sent to the client even though they're not in range of the player
 *
 * Confirmed working with Security Craft
 */
@Mixin(value = ChunkMap.TrackedEntity.class, priority = 1200)
public abstract class TrackedEntityMixin {
	@Shadow
	@Final
	Entity entity;
	/**
	 * Sends entities in range of a camera that is currently being viewed, as well as conductor entities, to the client.
	 *
	 * <p>PORT-NOTE(26.1): 26.1's updatePlayer keeps visibility in the named local visibleToPlayer, so the camera range
	 * check and the override are one modification of it (1.20 used positional local capture and a JUMP ordinal).</p>
	 */
	@ModifyVariable(method = "updatePlayer", at = @At("STORE"), name = "visibleToPlayer")
	private boolean railways$securitycraft$modifyFlag(boolean visible, @Local(argsOnly = true) ServerPlayer player,
															 @Local(name = "visibleRange") double viewDistance) {
		if (entity instanceof ConductorEntity)
			return true;
		if (!visible && ConductorPossessionController.isPossessingConductor(player)) {
			Vec3 relativePosToCamera = player.getCamera().position().subtract(entity.position());
			if (relativePosToCamera.x >= -viewDistance && relativePosToCamera.x <= viewDistance && relativePosToCamera.z >= -viewDistance && relativePosToCamera.z <= viewDistance)
				return true;
		}
		return visible;
	}
}
