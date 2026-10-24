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
import com.zurrtum.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Inventory;
import com.zurrtum.create.AllSynchedDatas;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MountedToolboxEquipPacket implements C2SPacket {

	private Integer toolboxCarrierId = null;
	private final int slot;
	private final int hotbarSlot;

	public MountedToolboxEquipPacket(ConductorEntity toolboxCarrier, int slot, int hotbarSlot) {
		this.toolboxCarrierId = toolboxCarrier.getId();
		this.slot = slot;
		this.hotbarSlot = hotbarSlot;
	}

	public MountedToolboxEquipPacket(FriendlyByteBuf buffer) {
		if (buffer.readBoolean())
			toolboxCarrierId = buffer.readInt();
		slot = buffer.readVarInt();
		hotbarSlot = buffer.readVarInt();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(toolboxCarrierId != null);
		if (toolboxCarrierId != null)
			buffer.writeInt(toolboxCarrierId);
		buffer.writeVarInt(slot);
		buffer.writeVarInt(hotbarSlot);
	}

	@Override
	public void handle(ServerPlayer player) {
		Level world = player.level();

		if (toolboxCarrierId == null) {
			ToolboxHandler.unequip(player, hotbarSlot, false);
			ToolboxHandler.syncData(player, AllSynchedDatas.TOOLBOX.get(player));
			return;
		}

		Entity entity = world.getEntity(toolboxCarrierId);

		double maxRange = ToolboxHandler.getMaxRange(player);
		if (player.distanceToSqr(entity) > maxRange
				* maxRange)
			return;
		if (!(entity instanceof ConductorEntity conductorEntity))
			return;

		ToolboxHandler.unequip(player, hotbarSlot, false);

		if (slot < 0 || slot >= 8) {
			ToolboxHandler.syncData(player, AllSynchedDatas.TOOLBOX.get(player));
			return;
		}

		MountedToolbox toolbox = conductorEntity.getToolbox();
		if (toolbox == null)
			return;

		Inventory playerInventory = player.getInventory();
		ItemStack held = playerInventory.getItem(hotbarSlot);
		if (!held.isEmpty() && !ToolboxInventory.canItemsShareCompartment(held, toolbox.inventory.filters.get(slot))) {
			// PORT-NOTE(fabric): same as Create Fly's own toolbox equip (AllHandle.onToolboxEquip)
			toolbox.inventory.inLimitedMode(inventory -> {
				int count = held.getCount();
				int insert = inventory.insertExist(held);
				if (insert != count) {
					count -= insert;
					insert = playerInventory.insert(held, count, Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE);
				}
				if (insert == count)
					playerInventory.setItem(hotbarSlot, ItemStack.EMPTY);
				else
					held.setCount(count - insert);
			});
		}

		CompoundTag compound = AllSynchedDatas.TOOLBOX.get(player);
		String key = String.valueOf(hotbarSlot);

		CompoundTag data = new CompoundTag();
		data.putInt("Slot", slot);
		data.store("EntityUUID", UUIDUtil.CODEC, conductorEntity.getUUID());
		data.store("Pos", BlockPos.CODEC, new BlockPos(0, 1000, 0));
		compound.put(key, data);

		toolbox.connectPlayer(slot, player, hotbarSlot);
		ToolboxHandler.syncData(player, compound);
	}
}
