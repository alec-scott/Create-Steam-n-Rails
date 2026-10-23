/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2025 The Railways Team
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
package com.railwayteam.railways.registry;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.smokestack.particles.chimneypush.ChimneyPushParticleData;
import com.railwayteam.railways.content.smokestack.particles.legacy.SmokeParticleData;
import com.railwayteam.railways.content.smokestack.particles.puffs.PuffSmokeParticleData;
import com.railwayteam.railways.port.create.foundation.particle.ICustomParticleData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Locale;
import java.util.function.Supplier;

public enum CRParticleTypes {
	SMOKE(SmokeParticleData::new),
	SMOKE_PUFF_SMALL(PuffSmokeParticleData.Small::new),
	SMOKE_PUFF_MEDIUM(PuffSmokeParticleData.Medium::new),
	CHIMNEYPUSH_SMALL(ChimneyPushParticleData.Small::new),
	CHIMNEYPUSH_MEDIUM(ChimneyPushParticleData.Medium::new),
	;

	private final ParticleEntry<?> entry;

	<D extends ParticleOptions> CRParticleTypes(Supplier<? extends ICustomParticleData<D>> typeFactory) {
		String name = name().toLowerCase(Locale.ROOT);
		entry = new ParticleEntry<>(name, typeFactory);
	}

	public ParticleType<?> get() {
		return entry.object;
	}

	public String parameter() {
		return entry.name;
	}

	/** Registers the particle types (PORT-NOTE(fabric): was Porting Lib's {@code LazyRegistrar}). */
	public static void init() {
		for (CRParticleTypes particle : values())
			Registry.register(BuiltInRegistries.PARTICLE_TYPE, Railways.asResource(particle.entry.name), particle.entry.object);
	}

	/** PORT-NOTE(fabric): providers go through Fabric's {@code ParticleProviderRegistry}; call once during client init. */
	@Environment(EnvType.CLIENT)
	public static void registerFactories() {
		for (CRParticleTypes particle : values())
			particle.entry.registerFactory();
	}

	private static class ParticleEntry<D extends ParticleOptions> {
		private final String name;
		private final Supplier<? extends ICustomParticleData<D>> typeFactory;
		private final ParticleType<D> object;

		public ParticleEntry(String name, Supplier<? extends ICustomParticleData<D>> typeFactory) {
			this.name = name;
			this.typeFactory = typeFactory;
			object = this.typeFactory.get().createType();
		}

		@Environment(EnvType.CLIENT)
		public void registerFactory() {
			typeFactory.get().register(object);
		}
	}
}
