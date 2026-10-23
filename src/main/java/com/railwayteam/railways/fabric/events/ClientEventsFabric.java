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
package com.railwayteam.railways.fabric.events;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.base.reload.ClientResourceReloadListener;
import com.railwayteam.railways.events.ClientEvents;
import com.railwayteam.railways.registry.CRParticleTypes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ClientEventsFabric {
	public static void init() {
		ClientTickEvents.START_CLIENT_TICK.register(ClientEvents::onClientTickStart);
		ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTickEnd);
		// Key input: mixin/port/client/KeyboardHandlerMixin (was Porting Lib's KeyInputCallback)
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((mc, level) -> ClientEvents.onClientWorldLoad(level));
		CRParticleTypes.registerFactories();
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Railways.asResource("client_events"),
			(ResourceManagerReloadListener) resourceManager -> ClientEvents.onTagsUpdated());
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Railways.asResource("client_reload_listener"),
			ClientResourceReloadListener.INSTANCE);
	}
}
