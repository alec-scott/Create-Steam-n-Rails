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
import com.railwayteam.railways.mixin.AccessorBlockEntity;
import com.railwayteam.railways.mixin.AccessorToolboxBlockEntity;
import com.railwayteam.railways.util.CRValueIO;
import com.railwayteam.railways.util.packet.PacketSender;
import com.zurrtum.create.content.equipment.toolbox.ToolboxBlock;
import com.zurrtum.create.content.equipment.toolbox.ToolboxBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A toolbox carried by a conductor.
 *
 * <p>PORT-NOTE(fabric): Create Fly has its own menu system; this opens a Railways {@code MenuType} whose client screen
 * finds the toolbox through the conductor (see {@link MountedToolboxScreen#create}). Item data comes from the toolbox
 * components (1.20.5) instead of NBT.</p>
 */
public class MountedToolbox extends ToolboxBlockEntity {
  protected final ConductorEntity parent;

  public MountedToolbox(ConductorEntity parent, DyeColor dyeColor) {
    super(parent.blockPosition(), ToolboxBlock.getColorBlock(dyeColor).defaultBlockState());
    this.parent = parent;
    setLevel(parent.level());
    setLazyTickRate(10);
  }

  public void readFromItem(ItemStack stack) {
    applyImplicitComponents(stack);
    Component name = stack.get(DataComponents.CUSTOM_NAME);
    if (name != null)
      setCustomName(name);
  }

  public ConductorEntity getParent() {
    return parent;
  }

  @Override
  public void tick() {
    // keep saved block pos updated for updateOpenCount and tickAudio
    ((AccessorBlockEntity) this).setWorldPosition(parent.blockPosition());
    super.tick();
  }

  @Override
  public void read(ValueInput compound, boolean clientPacket) {
    super.read(compound, clientPacket);
    compound.getInt("Color").ifPresent(colorId -> {
      DyeColor color = DyeColor.byId(colorId);
      // change the color by setting the stored state and updating the color provider
      BlockState state = ToolboxBlock.getColorBlock(color).defaultBlockState();
      setBlockState(state);
    });
  }

  @Override
  public void write(ValueOutput compound, boolean clientPacket) {
    super.write(compound, clientPacket);
    compound.putInt("Color", getColor().getId());
  }

  public List<Player> getConnectedPlayers() {
    Map<Integer, WeakHashMap<Player, Integer>> connectedPlayers = ((AccessorToolboxBlockEntity) this).getConnectedPlayers();
    Set<Player> players = new HashSet<>();
    for (Map.Entry<Integer, WeakHashMap<Player, Integer>> entry : connectedPlayers.entrySet()) {
       players.addAll(entry.getValue().keySet());
    }
    return players.stream().toList();
  }

  @Override
  public void sendData() {
    if (level == null || level.isClientSide())
      return;
    CompoundTag nbt = CRValueIO.save(level.registryAccess(), out -> this.write(out, true));
    PacketSender.syncMountedToolboxNBT(this.parent, nbt);
  }

  @Override
  public void setChanged() {
    // override and do nothing, this isn't in-world
  }

  public static MountedToolbox read(ConductorEntity parent, CompoundTag compound) {
    MountedToolbox holder = new MountedToolbox(parent, DyeColor.BROWN);
    holder.read(CRValueIO.input(parent.level().registryAccess(), compound), false);
    return holder;
  }

  public static MountedToolbox read(ConductorEntity parent, ValueInput input) {
    MountedToolbox holder = new MountedToolbox(parent, DyeColor.BROWN);
    holder.read(input, false);
    return holder;
  }

  @Override
  public MountedToolboxContainer createMenu(int id, @NotNull Inventory inv, @NotNull Player player, RegistryFriendlyByteBuf extraData) {
    sendToMenu(extraData);
    return new MountedToolboxContainer(id, inv, this);
  }

  public ItemStack getDisplayStack() {
    ItemStack stack = new ItemStack(ToolboxBlock.getColorBlock(getColor()));
    if (hasCustomName())
      stack.set(DataComponents.CUSTOM_NAME, getCustomName());
    return stack;
  }

  public ItemStack getCloneItemStack() {
    ItemStack stack = getDisplayStack();
    stack.applyComponents(collectComponents());
    return stack;
  }

  @Override
  public void sendToMenu(RegistryFriendlyByteBuf buffer) {
    buffer.writeVarInt(parent.getId());
    buffer.writeNbt(getUpdateTag(buffer.registryAccess()));
  }

  public static void openMenu(ServerPlayer player, MountedToolbox toolbox) {
    toolbox.openHandledScreen(player);
  }
}
