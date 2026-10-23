package com.railwayteam.railways.port.create;

import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.registry.CreateRegistryKeys;

import net.minecraft.world.level.block.Block;
import com.railwayteam.railways.port.registrate.builders.BlockBuilder;
import com.railwayteam.railways.port.registrate.util.entry.RegistryEntry;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullUnaryOperator;

/**
 * Create's {@code DisplaySource.displaySource(...)} block-builder transform.
 *
 * <p>PORT-NOTE(fabric): Create Fly maps blocks to display sources in {@link DisplaySource#BY_BLOCK}; the block is added
 * once both it and the display source are registered.</p>
 */
public final class DisplaySourceTransforms {
	private DisplaySourceTransforms() {
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> displaySource(RegistryEntry<DisplaySource, ? extends DisplaySource> source) {
		return builder -> builder.onRegisterAfter(CreateRegistryKeys.DISPLAY_SOURCE, block -> DisplaySource.BY_BLOCK.add(block, source.get()));
	}
}
