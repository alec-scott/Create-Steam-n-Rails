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

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.util.CRValueIO;
import com.zurrtum.create.client.content.equipment.toolbox.ToolboxScreen;
import com.zurrtum.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.zurrtum.create.foundation.gui.menu.MenuType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MountedToolboxScreen extends ToolboxScreen {
  public MountedToolboxScreen(MountedToolboxContainer container, Inventory inv, Component title) {
    super(container, inv, title);
  }

  /** Create Fly screen factory: the opening data is the conductor's entity id and the toolbox data. */
  public static MountedToolboxScreen create(Minecraft mc, MenuType<ToolboxBlockEntity> type, int syncId, Inventory inventory,
                                            Component title, RegistryFriendlyByteBuf extraData) {
    int conductorId = extraData.readVarInt();
    if (mc.level == null || !(mc.level.getEntity(conductorId) instanceof ConductorEntity conductor)) {
      Railways.LOGGER.error("Conductor with ID not found: " + conductorId);
      return null;
    }
    MountedToolbox toolbox = conductor.getOrCreateToolboxHolder();
    toolbox.readClient(CRValueIO.input(extraData.registryAccess(), extraData.readNbt()));
    return type.create(MountedToolboxScreen::new, syncId, inventory, title, toolbox);
  }
}
