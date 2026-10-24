package com.railwayteam.railways.port.create.track;

import com.railwayteam.railways.port.registrate.util.nullness.NonNullSupplier;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Upstream Create's {@code TrackMaterialFactory}, building a Create Fly {@link TrackMaterial} plus its
 * {@link TrackMaterialData}.
 *
 * <p>Models are recorded here and handed to Create Fly on the client by {@link TrackMaterialModels}. The
 * {@link PartialModel} types below are erased generics, so a dedicated server never loads the client class.</p>
 */
public class TrackMaterialFactory {
	static final Supplier<Optional<Ingredient>> DEFAULT_RAILS = TrackMaterialData::defaultRails;

	/** How a material's tie and rail segment models are found, by material. */
	static final Map<TrackMaterial, ModelSpec> MODELS = new IdentityHashMap<>();

	enum ModelKind { DEFAULT, STANDARD, CUSTOM }

	record ModelSpec(ModelKind kind, @Nullable Supplier<Supplier<PartialModel>> tie,
					 @Nullable Supplier<Supplier<PartialModel>> left, @Nullable Supplier<Supplier<PartialModel>> right) {
		static final ModelSpec DEFAULT = new ModelSpec(ModelKind.DEFAULT, null, null, null);
		static final ModelSpec STANDARD = new ModelSpec(ModelKind.STANDARD, null, null, null);
	}

	private final Identifier id;
	private String langName;
	private NonNullSupplier<NonNullSupplier<? extends TrackBlock>> trackBlock;
	private Supplier<Optional<Ingredient>> sleeperIngredient = Optional::empty;
	private Supplier<Optional<Ingredient>> railsIngredient = DEFAULT_RAILS;
	private Identifier particle;
	private TrackType trackType = TrackType.STANDARD;
	@Nullable
	private TrackType.TrackBlockFactory customFactory = null;
	private ModelSpec models = ModelSpec.DEFAULT;

	public TrackMaterialFactory(Identifier id) {
		this.id = id;
	}

	public Identifier getId() {
		return id;
	}

	public static TrackMaterialFactory make(Identifier id) {  // Convenience function for static import
		return new TrackMaterialFactory(id);
	}

	public TrackMaterialFactory lang(String langName) {
		this.langName = langName;
		return this;
	}

	public TrackMaterialFactory block(NonNullSupplier<NonNullSupplier<? extends TrackBlock>> trackBlock) {
		this.trackBlock = trackBlock;
		return this;
	}

	public TrackMaterialFactory defaultModels() {
		this.models = ModelSpec.DEFAULT;
		return this;
	}

	public TrackMaterialFactory sleeper(Ingredient sleeperIngredient) {
		this.sleeperIngredient = () -> Optional.of(sleeperIngredient);
		return this;
	}

	public TrackMaterialFactory sleeper(ItemLike... items) {
		this.sleeperIngredient = () -> Optional.of(Ingredient.of(items));
		return this;
	}

	/**
	 * A sleeper item that may not exist (another mod's slab), resolved when first needed.
	 *
	 * <p>PORT-NOTE(fabric): replaces Railways' {@code SoftIngredient}; 26.1 ingredients are final and can't hold a
	 * missing item.</p>
	 */
	public TrackMaterialFactory softSleeper(Identifier item) {
		this.sleeperIngredient = () -> BuiltInRegistries.ITEM.getOptional(item).map(Ingredient::of);
		return this;
	}

	public TrackMaterialFactory rails(Ingredient railsIngredient) {
		this.railsIngredient = () -> Optional.of(railsIngredient);
		return this;
	}

	public TrackMaterialFactory rails(ItemLike... items) {
		this.railsIngredient = () -> Optional.of(Ingredient.of(items));
		return this;
	}

	public TrackMaterialFactory noRecipeGen() {
		this.railsIngredient = Optional::empty;
		this.sleeperIngredient = Optional::empty;
		return this;
	}

	public TrackMaterialFactory particle(Identifier particle) {
		this.particle = particle;
		return this;
	}

	public TrackMaterialFactory trackType(TrackType trackType) {
		this.trackType = trackType;
		return this;
	}

	/** Models at {@code <namespace>:block/track/<path>/{tie,segment_left,segment_right}}. */
	public TrackMaterialFactory standardModels() {
		this.models = ModelSpec.STANDARD;
		return this;
	}

	public TrackMaterialFactory customModels(Supplier<Supplier<PartialModel>> tieModel,
											 Supplier<Supplier<PartialModel>> leftSegmentModel,
											 Supplier<Supplier<PartialModel>> rightSegmentModel) {
		this.models = new ModelSpec(ModelKind.CUSTOM, tieModel, leftSegmentModel, rightSegmentModel);
		return this;
	}

	public TrackMaterialFactory customBlockFactory(TrackType.TrackBlockFactory factory) {
		this.customFactory = factory;
		return this;
	}

	public TrackMaterial build() {
		if (trackBlock == null || langName == null || particle == null)
			throw new IllegalStateException("Track material " + id + " is missing its block, lang name or particle");
		NonNullSupplier<NonNullSupplier<? extends TrackBlock>> block = trackBlock;
		TrackMaterial material = new TrackMaterial(id, () -> block.get().get());
		TrackMaterialData.register(new TrackMaterialData(material, langName, particle, trackType, sleeperIngredient,
			railsIngredient, customFactory));
		MODELS.put(material, models);
		return material;
	}
}
