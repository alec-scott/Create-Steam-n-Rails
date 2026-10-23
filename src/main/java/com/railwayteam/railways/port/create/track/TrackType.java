package com.railwayteam.railways.port.create.track;

import com.zurrtum.create.AllTrackMaterials;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Upstream Create's {@code TrackMaterial.TrackType}.
 *
 * <p>PORT-NOTE(fabric): Create Fly dropped track types and treats a material's id as its type (bogeys return an
 * {@link Identifier} from {@code getTrackType}, compared by reference with the edge material's id). Railways keeps
 * types: bogeys return {@link #id}, and {@code mixin/port/AbstractBogeyBlockMixin} / {@code MixinNavigation} make
 * Create Fly compare {@link TrackMaterialData#typeId(TrackMaterial)} instead of the material id.
 * {@link #STANDARD} reuses the Andesite material's id <i>instance</i>, because that's what Create Fly's standard bogey
 * returns.</p>
 */
public class TrackType {
	@FunctionalInterface
	public interface TrackBlockFactory {
		TrackBlock create(BlockBehaviour.Properties properties, TrackMaterial material);
	}

	public static final TrackType STANDARD = new TrackType(AllTrackMaterials.ANDESITE.getId(), TrackBlock::new);

	public final Identifier id;
	protected final TrackBlockFactory factory;

	public TrackType(Identifier id, TrackBlockFactory factory) {
		this.id = id;
		this.factory = factory;
	}
}
