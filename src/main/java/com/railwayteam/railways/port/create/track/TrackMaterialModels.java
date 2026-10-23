package com.railwayteam.railways.port.create.track;

import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.AllTrackMaterialModels;
import com.zurrtum.create.client.AllTrackMaterialModels.TrackModelHolder;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Client side of {@link TrackMaterialFactory}: gives Create Fly a {@link TrackModelHolder} for each material built
 * with the factory. Call once during client init, after the materials are registered.
 */
public final class TrackMaterialModels {
	private static final TrackModelHolder DEFAULT = new TrackModelHolder(AllPartialModels.TRACK_TIE,
		AllPartialModels.TRACK_SEGMENT_LEFT, AllPartialModels.TRACK_SEGMENT_RIGHT);

	private TrackMaterialModels() {}

	public static void register() {
		for (Map.Entry<TrackMaterial, TrackMaterialFactory.ModelSpec> entry : TrackMaterialFactory.MODELS.entrySet()) {
			TrackMaterial material = entry.getKey();
			if (material.modelHolder != null)
				continue;
			AllTrackMaterialModels.register(material, holderFor(material.getId(), entry.getValue()));
		}
	}

	private static TrackModelHolder holderFor(Identifier id, TrackMaterialFactory.ModelSpec spec) {
		return switch (spec.kind()) {
			case DEFAULT -> DEFAULT;
			case STANDARD -> {
				String prefix = "block/track/" + id.getPath() + "/";
				yield new TrackModelHolder(
					PartialModel.of(Identifier.fromNamespaceAndPath(id.getNamespace(), prefix + "tie")),
					PartialModel.of(Identifier.fromNamespaceAndPath(id.getNamespace(), prefix + "segment_left")),
					PartialModel.of(Identifier.fromNamespaceAndPath(id.getNamespace(), prefix + "segment_right")));
			}
			case CUSTOM -> new TrackModelHolder(spec.tie().get().get(), spec.left().get().get(), spec.right().get().get());
		};
	}
}
