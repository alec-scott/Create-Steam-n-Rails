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
package com.railwayteam.railways.fabric.client;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.port.registrate.util.entry.RegistryEntry;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.foundation.item.ItemDescription;
import com.zurrtum.create.client.foundation.item.KineticStats;
import com.zurrtum.create.client.foundation.item.TooltipModifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Item description tooltips (the "hold shift" summaries).
 *
 * <p>PORT-NOTE(fabric): upstream set {@code REGISTRATE.setTooltipModifierFactory(...)} in common code. Create Fly's
 * tooltip classes are client-only, so every Railways item is registered here from the client entrypoint instead
 * (as in the CBC port).</p>
 */
public class CRItemTooltips {
	public static void register() {
		for (RegistryEntry<Item, Item> entry : Railways.registrate().<Item, Item>getAll(Registries.ITEM)) {
			Item item = entry.get();
			if (TooltipModifier.REGISTRY.get(item) != null)
				continue;
			TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
				.andThen(TooltipModifier.mapNull(KineticStats.create(item))));
		}
	}
}
