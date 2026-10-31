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

import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.content.conductor.ConductorPossessionController;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This mixin makes sure that chunks near cameras are properly sent to the player viewing it, as well as fixing block updates
 * not getting sent to chunks loaded by cameras
 *
 * <p>PORT-NOTE(26.1): chunk tracking goes through {@link ChunkTrackingView}. Block updates are fixed by also treating
 * chunks in view of the possessed conductor as tracked (1.20 redirected the section position used by
 * {@code getPlayers}), and camera chunks are loaded and dropped with the view difference. The SecurityCraft camera check
 * is dropped with the rest of the 1.20 compat.</p>
 */
@Mixin(value = ChunkMap.class, priority = 1200)
public abstract class ChunkMapMixin {
	@Shadow
	protected abstract int getPlayerViewDistance(ServerPlayer player);

	@Shadow
	protected abstract void markChunkPendingToSend(ServerPlayer player, ChunkPos pos);

	@Shadow
	private static void dropChunk(ServerPlayer player, ChunkPos pos) {
		throw new AssertionError();
	}

	@Inject(method = "isChunkTracked", at = @At("RETURN"), cancellable = true)
	private void railways$trackCameraChunks(ServerPlayer player, int x, int z, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && ConductorPossessionController.isPossessingConductor(player)) {
			SectionPos cameraPos = SectionPos.of(player.getCamera());
			if (ChunkTrackingView.isWithinDistance(cameraPos.x(), cameraPos.z(), getPlayerViewDistance(player), x, z, false))
				cir.setReturnValue(true);
		}
	}

	/**
	 * Tracks chunks loaded by cameras to send them to the client, and tracks chunks around the player to properly update them
	 * when they stop viewing a camera
	 */
	@Inject(method = "move", at = @At(value = "TAIL"))
	private void railways$trackCameraLoadedChunks(ServerPlayer player, CallbackInfo callback) {
		int viewDistance = getPlayerViewDistance(player);
		if (player.getCamera() instanceof ConductorEntity camera) {
			if (!camera.hasSentChunks()) {
				SectionPos oldPos = camera.oldSectionPos;
				SectionPos pos = SectionPos.of(camera);
				camera.oldSectionPos = pos;

				// if we are updating from a previous position, only load / unload relevant chunks
				ChunkTrackingView oldView = oldPos == null ? ChunkTrackingView.EMPTY
					: ChunkTrackingView.of(new ChunkPos(oldPos.x(), oldPos.z()), viewDistance);
				ChunkTrackingView newView = ChunkTrackingView.of(new ChunkPos(pos.x(), pos.z()), viewDistance);
				ChunkTrackingView.difference(oldView, newView,
					chunkPos -> markChunkPendingToSend(player, chunkPos),
					chunkPos -> {
						if (!player.getChunkTrackingView().contains(chunkPos))
							dropChunk(player, chunkPos);
					});

				camera.setHasSentChunks(true);
			}
		}
		else if (ConductorEntity.hasRecentlyDismounted(player)) {
			SectionPos pos = player.getLastSectionPos();
			ChunkTrackingView.of(new ChunkPos(pos.x(), pos.z()), viewDistance)
				.forEach(chunkPos -> markChunkPendingToSend(player, chunkPos));
		}
	}
}
