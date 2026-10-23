package com.railwayteam.railways.port.create;

import com.railwayteam.railways.port.registrate.builders.BlockBuilder;
import com.railwayteam.railways.port.registrate.util.entry.RegistryEntry;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullConsumer;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullUnaryOperator;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.behaviour.display.DisplayTarget;
import com.zurrtum.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.api.registry.CreateRegistryKeys;
import net.minecraft.world.level.block.Block;

/**
 * Upstream Create's static block-builder helpers ({@code com.railwayteam.railways.port.create.CreateTransforms.movementBehaviour(...)},
 * {@code DisplaySource.displaySource(...)}, ...), which Create Fly dropped along with Registrate.
 *
 * <p>PORT-NOTE(fabric): they register into Create Fly's {@code SimpleRegistry}s ({@link MovementBehaviour#REGISTRY},
 * {@link DisplaySource#BY_BLOCK}, ...). Display sources/targets are attached once both the block and the source are
 * registered.</p>
 */
public final class CreateTransforms {
	private CreateTransforms() {
	}

	public static <B extends Block> NonNullConsumer<? super B> movementBehaviour(MovementBehaviour behaviour) {
		return block -> MovementBehaviour.REGISTRY.register(block, behaviour);
	}

	public static <B extends Block> NonNullConsumer<? super B> interactionBehaviour(MovingInteractionBehaviour behaviour) {
		return block -> MovingInteractionBehaviour.REGISTRY.register(block, behaviour);
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> displaySource(RegistryEntry<DisplaySource, ? extends DisplaySource> source) {
		return builder -> builder.onRegisterAfter(CreateRegistryKeys.DISPLAY_SOURCE, block -> DisplaySource.BY_BLOCK.add(block, source.get()));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> displayTarget(RegistryEntry<DisplayTarget, ? extends DisplayTarget> target) {
		return builder -> builder.onRegisterAfter(CreateRegistryKeys.DISPLAY_TARGET, block -> DisplayTarget.BY_BLOCK.register(block, target.get()));
	}
}
