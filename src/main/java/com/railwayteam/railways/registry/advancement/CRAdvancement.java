/*
 * Steam 'n' Rails
 * Copyright (c) 2023-2025 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.railwayteam.railways.registry.advancement;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.registry.CRAdvancements;
import com.railwayteam.railways.registry.CRTriggers;
import com.railwayteam.railways.port.registrate.util.entry.ItemProviderEntry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A Railways advancement.
 *
 * <p>PORT-NOTE(fabric): upstream also built the advancement JSON here (datagen). Datagen is stubbed on the 26.1
 * branch, so only the runtime half is kept: the built-in trigger, awarding, and the progress check. The builder methods
 * keep their signatures so {@code CRAdvancements} reads the same; the advancements themselves come from the committed
 * generated JSON.</p>
 */
public class CRAdvancement {

	static final String LANG = "advancement." + Railways.MOD_ID + ".";
	static final String SECRET_SUFFIX = "\n§7(Hidden Advancement)";

	private SimpleRailwaysTrigger builtinTrigger;
	private CRAdvancement parent;

	private final String id;
	private String title;
	private String description;

	public CRAdvancement(String id, UnaryOperator<Builder> b) {
		this.id = id;

		Builder t = new Builder();
		b.apply(t);

		if (!t.externalTrigger)
			builtinTrigger = CRTriggers.addSimple(id + "_builtin");

		if (t.type == TaskType.SECRET)
			description += SECRET_SUFFIX;

		CRAdvancements.ENTRIES.add(this);
	}

	private String titleKey() {
		return LANG + id;
	}

	private String descriptionKey() {
		return titleKey() + ".desc";
	}

	public boolean isAlreadyAwardedTo(Player player) {
		if (!(player instanceof ServerPlayer sp))
			return true;
		AdvancementHolder advancement = sp.level().getServer()
			.getAdvancements()
			.get(Railways.asResource(id));
		if (advancement == null)
			return true;
		return sp.getAdvancements()
			.getOrStartProgress(advancement)
			.isDone();
	}

	public void awardTo(Player player) {
		if (!(player instanceof ServerPlayer sp))
			return;
		if (builtinTrigger == null)
			throw new UnsupportedOperationException(
				"Advancement " + id + " uses external Triggers, it cannot be awarded directly");
		builtinTrigger.trigger(sp);
	}

	@ApiStatus.Internal
	public void provideLang(BiConsumer<String, String> consumer) {
		consumer.accept(titleKey(), title);
		consumer.accept(descriptionKey(), description);
	}

	@ApiStatus.Internal
	public enum TaskType {
		SILENT, NORMAL, NOISY, EXPERT, SECRET
	}

	@ApiStatus.Internal
	public class Builder {

		private TaskType type = TaskType.NORMAL;
		private boolean externalTrigger;

		public Builder special(TaskType type) {
			this.type = type;
			return this;
		}

		public Builder after(CRAdvancement other) {
			CRAdvancement.this.parent = other;
			return this;
		}

		public Builder icon(ItemProviderEntry<?, ?> item) {
			return this;
		}

		public Builder icon(ItemLike item) {
			return this;
		}

		public Builder icon(ItemStack stack) {
			return this;
		}

		public Builder icon(Supplier<ItemStack> stackSupplier) {
			return this;
		}

		public Builder title(String title) {
			CRAdvancement.this.title = title;
			return this;
		}

		public Builder description(String description) {
			CRAdvancement.this.description = description;
			return this;
		}

		public Builder whenBlockPlaced(Block block) {
			return externalTrigger();
		}

		public Builder whenIconCollected() {
			return externalTrigger();
		}

		public Builder whenItemCollected(ItemProviderEntry<?, ?> item) {
			return externalTrigger();
		}

		public Builder whenItemCollected(ItemLike itemProvider) {
			return externalTrigger();
		}

		public Builder whenItemCollected(TagKey<Item> tag) {
			return externalTrigger();
		}

		public Builder awardedForFree() {
			return externalTrigger();
		}

		/** The advancement's criteria are vanilla triggers defined in its JSON. */
		public Builder externalTrigger() {
			externalTrigger = true;
			return this;
		}
	}
}
