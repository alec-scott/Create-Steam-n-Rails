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

package com.railwayteam.railways.multiloader.fabric;

import com.railwayteam.railways.multiloader.C2SPacket;
import com.railwayteam.railways.multiloader.PacketSet;
import com.railwayteam.railways.multiloader.S2CPacket;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.List;
import java.util.function.Function;

/**
 * PORT-NOTE(fabric): 26.1 Fabric networking is payload based. Each direction is one {@link RawPayload} type carrying
 * the bytes Railways' {@link PacketSet} writes (packet index + body). Bodies are read and written through a
 * {@link RegistryFriendlyByteBuf}, so packets can encode item stacks.
 */
public class PacketSetImpl extends PacketSet {
	private final CustomPacketPayload.Type<RawPayload> c2sType;
	private final CustomPacketPayload.Type<RawPayload> s2cType;

	protected PacketSetImpl(String id, int version,
							List<Function<FriendlyByteBuf, S2CPacket>> s2cPackets,
							Object2IntMap<Class<? extends S2CPacket>> s2cTypes,
							List<Function<FriendlyByteBuf, C2SPacket>> c2sPackets,
							Object2IntMap<Class<? extends C2SPacket>> c2sTypes) {
		super(id, version, s2cPackets, s2cTypes, c2sPackets, c2sTypes);
		c2sType = new CustomPacketPayload.Type<>(c2sPacket);
		s2cType = new CustomPacketPayload.Type<>(s2cPacket);
		PayloadTypeRegistry.serverboundPlay().register(c2sType, RawPayload.codec(c2sType));
		PayloadTypeRegistry.clientboundPlay().register(s2cType, RawPayload.codec(s2cType));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void registerS2CListener() {
		ClientPlayNetworking.registerGlobalReceiver(s2cType, (payload, context) -> handleS2CPacket(context.client(),
			new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), context.player().registryAccess())));
	}

	@Override
	public void registerC2SListener() {
		ServerPlayNetworking.registerGlobalReceiver(c2sType, (payload, context) -> handleC2SPacket(context.player(),
			new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), context.player().registryAccess())));
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected void doSendC2S(RegistryFriendlyByteBuf buf) {
		ClientPlayNetworking.send(new RawPayload(c2sType, RawPayload.bytesOf(buf)));
	}

	void sendS2C(ServerPlayer player, byte[] data) {
		ServerPlayNetworking.send(player, new RawPayload(s2cType, data));
	}

	@Internal
	public static PacketSet create(String id, int version,
								   List<Function<FriendlyByteBuf, S2CPacket>> s2cPackets,
								   Object2IntMap<Class<? extends S2CPacket>> s2cTypes,
								   List<Function<FriendlyByteBuf, C2SPacket>> c2sPackets,
								   Object2IntMap<Class<? extends C2SPacket>> c2sTypes) {
		return new PacketSetImpl(id, version, s2cPackets, s2cTypes, c2sPackets, c2sTypes);
	}

	public record RawPayload(Type<RawPayload> type, byte[] data) implements CustomPacketPayload {
		static StreamCodec<RegistryFriendlyByteBuf, RawPayload> codec(Type<RawPayload> type) {
			return StreamCodec.of((buf, payload) -> buf.writeBytes(payload.data()),
				buf -> new RawPayload(type, bytesOf(buf)));
		}

		static byte[] bytesOf(FriendlyByteBuf buf) {
			byte[] bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes);
			return bytes;
		}
	}
}
