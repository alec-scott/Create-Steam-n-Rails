package com.railwayteam.railways.port.create.track;

import com.google.common.base.Suppliers;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.AllTrackMaterials;
import com.zurrtum.create.Create;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The parts of upstream Create's {@code TrackMaterial} that Create Fly dropped (lang name, particle, track type, recipe
 * ingredients, custom block factory), kept per material on the Railways side.
 *
 * <p>Materials made with {@link TrackMaterialFactory} register their data; Create's Andesite material is registered
 * here with upstream's values. Any other material (from another addon) gets derived defaults, and its track type is its
 * own id, which is exactly how Create Fly treats it.</p>
 */
public final class TrackMaterialData {
	private static final Map<TrackMaterial, TrackMaterialData> DATA = new IdentityHashMap<>();
	private static final Map<TrackMaterial, TrackMaterialData> DERIVED = new IdentityHashMap<>();

	public final TrackMaterial material;
	public final String langName;
	public final Identifier particle;
	public final TrackType trackType;
	private final Supplier<Optional<Ingredient>> sleeperIngredient;
	private final Supplier<Optional<Ingredient>> railsIngredient;
	@Nullable
	private final TrackType.TrackBlockFactory customFactory;

	TrackMaterialData(TrackMaterial material, String langName, Identifier particle, TrackType trackType,
					  Supplier<Optional<Ingredient>> sleeperIngredient, Supplier<Optional<Ingredient>> railsIngredient,
					  @Nullable TrackType.TrackBlockFactory customFactory) {
		this.material = material;
		this.langName = langName;
		this.particle = particle;
		this.trackType = trackType;
		this.sleeperIngredient = Suppliers.memoize(sleeperIngredient::get);
		this.railsIngredient = Suppliers.memoize(railsIngredient::get);
		this.customFactory = customFactory;
	}

	static void register(TrackMaterialData data) {
		DATA.put(data.material, data);
	}

	static {
		register(new TrackMaterialData(AllTrackMaterials.ANDESITE, "Andesite",
			Identifier.fromNamespaceAndPath(Create.MOD_ID, "block/palettes/stone_types/polished/andesite_cut_polished"), TrackType.STANDARD,
			Optional::empty, TrackMaterialFactory.DEFAULT_RAILS, null));
	}

	public static TrackMaterialData of(TrackMaterial material) {
		TrackMaterialData data = DATA.get(material);
		if (data != null)
			return data;
		synchronized (DERIVED) {
			return DERIVED.computeIfAbsent(material, TrackMaterialData::derive);
		}
	}

	private static TrackMaterialData derive(TrackMaterial material) {
		Identifier id = material.getId();
		String path = id.getPath().replace('_', ' ');
		String lang = path.isEmpty() ? path : path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
		TrackType ownType = new TrackType(id, TrackBlock::new);
		return new TrackMaterialData(material, lang, Identifier.withDefaultNamespace("block/stone"), ownType,
			Optional::empty, Optional::empty, null);
	}

	/** The id Create Fly should compare with a bogey's {@code getTrackType}. */
	public static Identifier typeId(TrackMaterial material) {
		return of(material).trackType.id;
	}

	/** Upstream Create's {@code TrackMaterial.deserialize}: blank (data from Create 0.5) or unknown ids give Andesite. */
	public static TrackMaterial deserialize(String serializedName) {
		if (serializedName.isBlank())
			return AllTrackMaterials.ANDESITE;
		Identifier id = Identifier.tryParse(serializedName);
		return id == null ? AllTrackMaterials.ANDESITE : TrackMaterial.fromId(id);
	}

	public Optional<Ingredient> sleeperIngredient() {
		return sleeperIngredient.get();
	}

	public Optional<Ingredient> railsIngredient() {
		return railsIngredient.get();
	}

	public TrackBlock createBlock(BlockBehaviour.Properties properties) {
		return (customFactory != null ? customFactory : trackType.factory).create(properties, material);
	}

	public String resourceName() {
		return material.getId().getPath();
	}

	public boolean isFromMod(String modId) {
		return material.getId().getNamespace().equals(modId);
	}

	public ItemStack asStack() {
		return asStack(1);
	}

	public ItemStack asStack(int count) {
		return new ItemStack(material.getBlock(), count);
	}

	public static List<TrackMaterial> allFromMod(String modId) {
		List<TrackMaterial> list = new ArrayList<>();
		for (TrackMaterial material : TrackMaterial.ALL.values()) {
			if (material.getId().getNamespace().equals(modId))
				list.add(material);
		}
		return list;
	}

	static Optional<Ingredient> defaultRails() {
		return Optional.of(Ingredient.of(Items.IRON_NUGGET, AllItems.ZINC_NUGGET));
	}
}
