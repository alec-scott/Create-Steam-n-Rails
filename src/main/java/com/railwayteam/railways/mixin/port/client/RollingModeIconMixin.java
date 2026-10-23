package com.railwayteam.railways.mixin.port.client;

import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import com.railwayteam.railways.registry.CRIcons;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.RollingModeScrollBehaviour.RollingModeIcon;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import com.zurrtum.create.content.contraptions.actors.roller.RollerBlockEntity.RollingMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client half of {@link com.railwayteam.railways.mixin.port.RollingModeMixin}: the scroll option and its icon. */
@Mixin(RollingModeIcon.class)
public class RollingModeIconMixin {
	@Shadow
	@Final
	@Mutable
	private static RollingModeIcon[] $VALUES;

	static {
		$VALUES = ArrayUtils.add($VALUES, railways$create("TRACK_REPLACE", $VALUES.length, CRIcons.I_SWAP_TRACKS));
	}

	@Invoker("<init>")
	private static RollingModeIcon railways$create(String name, int ordinal, AllIcons icon) {
		throw new IllegalStateException("Unreachable");
	}

	// Create Fly's exhaustive switch would throw for the added mode.
	@Inject(method = "from", at = @At("HEAD"), cancellable = true)
	private static void railways$trackReplace(RollingMode mode, CallbackInfoReturnable<RollingModeIcon> cir) {
		if (mode.ordinal() >= 3)
			cir.setReturnValue($VALUES[mode.ordinal()]);
	}
}
