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

package com.railwayteam.railways.content.conductor.toolbox;

import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.multiloader.C2SPacket;
import com.zurrtum.create.content.equipment.toolbox.ToolboxHandler;
import com.zurrtum.create.AllSynchedDatas;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class MountedToolboxDisposeAllPacket implements C2SPacket {

	private final int toolboxCarrierId;

	public MountedToolboxDisposeAllPacket(ConductorEntity toolboxCarrier) {
		this.toolboxCarrierId = toolboxCarrier.getId();
	}

	public MountedToolboxDisposeAllPacket(FriendlyByteBuf buffer) {
		toolboxCarrierId = buffer.readInt();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(toolboxCarrierId);
	}

	@Override
	public void handle(ServerPlayer player) {
		Level world = player.level();
		if (world.getEntity(toolboxCarrierId) instanceof ConductorEntity conductorEntity) {

			double maxRange = ToolboxHandler.getMaxRange(player);
			if (player.distanceToSqr(conductorEntity) > maxRange
					* maxRange)
				return;

			MountedToolbox toolbox = conductorEntity.getToolbox();
			if (toolbox == null)
				return;
			boolean sendData = doDisposal(toolbox, player, conductorEntity);

			if (sendData)
				ToolboxHandler.syncData(player, AllSynchedDatas.TOOLBOX.get(player));
		}
	}

	/** PORT-NOTE(fabric): mirrors Create Fly's own dispose-all (AllHandle.onToolboxDisposeAll), keyed by conductor. */
	public static boolean doDisposal(MountedToolbox toolbox, ServerPlayer player, ConductorEntity conductor) {
		CompoundTag compound = AllSynchedDatas.TOOLBOX.get(player);
		MutableBoolean sendData = new MutableBoolean(false);
		Inventory playerInventory = player.getInventory();
		toolbox.inventory.inLimitedMode(inventory -> {
			for (int i = 0; i < 36; i++) {
				if (compound.getCompound(String.valueOf(i)).flatMap(nbt -> nbt.read("EntityUUID", UUIDUtil.CODEC))
					.map(uuid -> uuid.equals(conductor.getUUID())).orElse(false)) {
					ToolboxHandler.unequip(player, i, true);
					sendData.setTrue();
				}

				ItemStack itemStack = playerInventory.getItem(i);
				int count = itemStack.getCount();
				if (count == 0)
					continue;
				int insert = toolbox.inventory.insertExist(itemStack, count);
				if (insert == count)
					playerInventory.setItem(i, ItemStack.EMPTY);
				else
					itemStack.setCount(count - insert);
			}
		});
		return sendData.booleanValue();
	}
}
