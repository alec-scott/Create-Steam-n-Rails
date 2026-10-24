package com.railwayteam.railways.port.create;

import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Items that share another key's "hold shift" description (upstream {@code ItemDescription.useKey}).
 *
 * <p>PORT-NOTE(fabric): Create Fly's {@code ItemDescription} is client only, so registration code records the keys here
 * and {@code fabric/client/CRItemTooltips} hands them to Create Fly on the client.</p>
 */
public final class ItemDescriptionKeys {
	private static final Map<ItemLike, String> KEYS = new LinkedHashMap<>();

	private ItemDescriptionKeys() {
	}

	public static void useKey(ItemLike item, String key) {
		KEYS.put(item, key);
	}

	public static Map<ItemLike, String> all() {
		return KEYS;
	}
}
