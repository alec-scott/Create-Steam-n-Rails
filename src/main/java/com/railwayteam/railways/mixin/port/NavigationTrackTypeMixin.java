package com.railwayteam.railways.mixin.port;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.railwayteam.railways.port.create.track.TrackMaterialData;
import com.zurrtum.create.content.trains.entity.Navigation;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create Fly's pathfinding checks the edge's material id against the bogeys' valid track types; check the material's
 * track type instead (see {@link com.railwayteam.railways.port.create.track.TrackType}).
 */
@Mixin(Navigation.class)
public class NavigationTrackTypeMixin {
	@WrapOperation(method = "search(DDZLjava/util/ArrayList;Lcom/zurrtum/create/content/trains/entity/Navigation$StationTest;)V",
		at = @At(value = "INVOKE",
			target = "Lcom/zurrtum/create/content/trains/track/TrackMaterial;getId()Lnet/minecraft/resources/Identifier;"))
	private Identifier railways$compareTrackType(TrackMaterial material, Operation<Identifier> original) {
		return TrackMaterialData.typeId(material);
	}
}
