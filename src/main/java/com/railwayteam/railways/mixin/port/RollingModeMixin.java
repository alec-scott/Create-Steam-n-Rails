package com.railwayteam.railways.mixin.port;

import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import com.zurrtum.create.content.contraptions.actors.roller.RollerBlockEntity.RollingMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Adds the roller's {@code TRACK_REPLACE} mode (ordinal 3, used by {@code MixinRollerMovementBehaviour}).
 *
 * <p>PORT-NOTE(fabric): replaces the Fabric-ASM enum builder (no 26.1 build). Create Fly keeps the mode's icon in a
 * separate client enum, extended by {@code mixin/port/client/RollingModeIconMixin}.</p>
 */
@Mixin(RollingMode.class)
public class RollingModeMixin {
	@Shadow
	@Final
	@Mutable
	private static RollingMode[] $VALUES;

	static {
		$VALUES = ArrayUtils.add($VALUES, railways$create("TRACK_REPLACE", $VALUES.length));
	}

	@Invoker("<init>")
	private static RollingMode railways$create(String name, int ordinal) {
		throw new IllegalStateException("Unreachable");
	}
}
