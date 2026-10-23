package com.railwayteam.railways.mixin.port.client;

import com.railwayteam.railways.events.ClientEvents;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PORT-NOTE(fabric): replaces Porting Lib's {@code KeyInputCallback} (fired after vanilla handles the key). */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("TAIL"))
	private void railways$onKeyInput(long window, int action, KeyEvent event, CallbackInfo ci) {
		ClientEvents.onKeyInput(event.key(), action != 0);
	}
}
