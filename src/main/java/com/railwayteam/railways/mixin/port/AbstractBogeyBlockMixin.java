package com.railwayteam.railways.mixin.port;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.railwayteam.railways.port.create.track.TrackMaterialData;
import com.zurrtum.create.content.trains.bogey.AbstractBogeyBlock;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create Fly compares the edge's material id with the bogey's track type; compare the material's track type instead
 * (see {@link com.railwayteam.railways.port.create.track.TrackType}).
 */
@Mixin(AbstractBogeyBlock.class)
public class AbstractBogeyBlockMixin {
	@WrapOperation(method = "isOnIncompatibleTrack", at = @At(value = "INVOKE",
		target = "Lcom/zurrtum/create/content/trains/track/TrackMaterial;getId()Lnet/minecraft/resources/Identifier;"))
	private Identifier railways$compareTrackType(TrackMaterial material, Operation<Identifier> original) {
		return TrackMaterialData.typeId(material);
	}
}
