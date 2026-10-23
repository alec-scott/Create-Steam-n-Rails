package com.railwayteam.railways.mixin.port;

import com.railwayteam.railways.mixin_interfaces.IHandcarTrain;
import com.zurrtum.create.content.trains.entity.Train;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Syncs the handcar flag with the train.
 *
 * <p>PORT-NOTE(fabric): upstream appended it to Create's {@code TrainPacket}. Create Fly's {@code AddTrainPacket} is
 * just {@link Train#STREAM_CODEC}, so the codec is wrapped once {@code Train} is initialised (before any packet codec
 * captures it).</p>
 */
@Mixin(Train.class)
public class TrainStreamCodecMixin {
	@Shadow
	@Final
	@Mutable
	public static StreamCodec<RegistryFriendlyByteBuf, Train> STREAM_CODEC;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void railways$syncHandcar(CallbackInfo ci) {
		StreamCodec<RegistryFriendlyByteBuf, Train> original = STREAM_CODEC;
		STREAM_CODEC = StreamCodec.of(
			(buf, train) -> {
				original.encode(buf, train);
				buf.writeBoolean(((IHandcarTrain) train).railways$isHandcar());
			},
			buf -> {
				Train train = original.decode(buf);
				((IHandcarTrain) train).railways$setHandcar(buf.readBoolean());
				return train;
			});
	}
}
