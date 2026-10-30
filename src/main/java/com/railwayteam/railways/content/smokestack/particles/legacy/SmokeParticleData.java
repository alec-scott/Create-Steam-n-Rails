/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
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

package com.railwayteam.railways.content.smokestack.particles.legacy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.railwayteam.railways.registry.CRParticleTypes;
import com.railwayteam.railways.port.create.foundation.particle.ICustomParticleDataWithSprite;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class SmokeParticleData implements ParticleOptions, ICustomParticleDataWithSprite<SmokeParticleData> {

	public static final MapCodec<SmokeParticleData> CODEC = RecordCodecBuilder.mapCodec(i -> i
		.group(Codec.BOOL.fieldOf("stationary")
			.forGetter(p -> p.stationary),
			Codec.FLOAT.fieldOf("red")
				.forGetter(p -> p.red),
			Codec.FLOAT.fieldOf("green")
				.forGetter(p -> p.green),
			Codec.FLOAT.fieldOf("blue")
				.forGetter(p -> p.blue))
		.apply(i, SmokeParticleData::new));

	// PORT-NOTE(26.1): replaces the 1.20 Deserializer/writeToNetwork pair (commands use the codec now).
	public static final StreamCodec<RegistryFriendlyByteBuf, SmokeParticleData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, p -> p.stationary,
		ByteBufCodecs.FLOAT, p -> p.red,
		ByteBufCodecs.FLOAT, p -> p.green,
		ByteBufCodecs.FLOAT, p -> p.blue,
		SmokeParticleData::new);

	boolean stationary;
	float red;
	float green;
	float blue;

	public SmokeParticleData() {
		this(false);
	}

	public SmokeParticleData(float red, float green, float blue) {
		this(false, red, green, blue);
	}

	public SmokeParticleData(boolean stationary) {
		this(stationary, stationary ? 0.3f : 0.1f);
	}

	public SmokeParticleData(boolean stationary, float brightness) {
		this(stationary, brightness, brightness, brightness);
	}

	public SmokeParticleData(boolean stationary, float red, float green, float blue) {
		this.stationary = stationary;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	@Override
	public ParticleType<?> getType() {
		return CRParticleTypes.SMOKE.get();
	}

	@Override
	public StreamCodec<? super RegistryFriendlyByteBuf, SmokeParticleData> getStreamCodec() {
		return STREAM_CODEC;
	}

	@Override
	public MapCodec<SmokeParticleData> getCodec(ParticleType<SmokeParticleData> type) {
		return CODEC;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public ParticleProviderRegistry.PendingParticleProvider<SmokeParticleData> getMetaFactory() {
		return SmokeParticle.Factory::new;
	}
}