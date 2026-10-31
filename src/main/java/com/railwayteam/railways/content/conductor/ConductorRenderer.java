/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2026 The Railways Team
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

package com.railwayteam.railways.content.conductor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.conductor.toolbox.MountedToolbox;
import com.railwayteam.railways.mixin.client.AccessorLivingEntityRenderer;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.AllItemTags;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PORT-NOTE(26.1): entity renderers extract a render state and submit it later. Everything the model and layers need
 * is extracted into {@link ConductorRenderState}; the humanoid fields (crouching, swimming, elytra, equipment) are
 * filled by {@link HumanoidMobRenderer#extractHumanoidRenderState}. This stays a {@link MobRenderer} because
 * {@link HumanoidMobRenderer} would add head and held item layers the conductor never had.
 */
public class ConductorRenderer extends MobRenderer<ConductorEntity, ConductorRenderState, ConductorEntityModel> {
  public static final Identifier TEXTURE = Railways.asResource("textures/entity/conductor.png");

  public ConductorRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, new ConductorEntityModel(ctx.bakeLayer(ConductorEntityModel.LAYER_LOCATION)), 0.2f);
    this.addLayer(new HumanoidArmorLayer<>(this,
      ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, ctx.getModelSet(), ConductorEntityModel::new),
      ctx.getEquipmentRenderer()
    ));
    this.addLayer(new ConductorSecondaryHeadLayer(this, ctx.getModelSet(), ctx.getPlayerSkinRenderCache()));
    this.addLayer(new ConductorToolboxLayer(this));
    this.addLayer(new ConductorFlagLayer(this));
    this.addLayer(new ConductorRemoteLayer(this));
    this.addLayer(new ConductorElytraLayer(this, ctx.getModelSet(), ctx.getEquipmentRenderer()));
  }

  private static Identifier ensurePng(Identifier loc) {
    if (loc.getPath().endsWith(".png")) return loc;
    return Identifier.fromNamespaceAndPath(loc.getNamespace(), loc.getPath() + ".png");
  }

  public static Identifier getTextureLocation(ItemStack headItem, @Nullable Component customName) {
    String name = headItem.getHoverName().getString();
    if (name.startsWith("[sus]"))
      name = name.substring(5);
    if (!headItem.isEmpty() && headItem.getItem() instanceof ConductorCapItem && CRBlockPartials.CUSTOM_CONDUCTOR_SKINS.containsKey(name)) {
      return ensurePng(CRBlockPartials.CUSTOM_CONDUCTOR_SKINS.get(name));
    }
    if (customName != null && CRBlockPartials.CUSTOM_CONDUCTOR_SKINS_FOR_NAME.containsKey(customName.getString())) {
      return ensurePng(CRBlockPartials.CUSTOM_CONDUCTOR_SKINS_FOR_NAME.get(customName.getString()));
    }
    return TEXTURE;
  }

  @Override
  public @NotNull Identifier getTextureLocation(@NotNull ConductorRenderState state) {
    return state.texture;
  }

  @Override
  public @NotNull ConductorRenderState createRenderState() {
    return new ConductorRenderState();
  }

  @Override
  public void extractRenderState(@NotNull ConductorEntity conductor, @NotNull ConductorRenderState state, float partialTicks) {
    super.extractRenderState(conductor, state, partialTicks);
    HumanoidMobRenderer.extractHumanoidRenderState(conductor, state, partialTicks, this.itemModelResolver);

    state.texture = getTextureLocation(conductor.getItemBySlot(EquipmentSlot.HEAD), conductor.getCustomName());
    state.color = conductor.getColor();
    state.job = conductor.getJob();
    state.holdingSchedules = conductor.isHoldingSchedulesClient();

    extractWornHead(conductor.getSecondaryHeadStack(), state.secondaryHead,
      conductor, this.itemModelResolver);
    state.secondaryHead.wornHeadAnimationPos = state.walkAnimationPos;

    ItemStack toolboxStack = conductor.getToolboxDisplayStack();
    MountedToolbox toolbox = conductor.getToolbox();
    state.carryingToolbox = toolbox != null && conductor.isCarryingToolbox() && toolboxStack.is(AllItemTags.TOOLBOXES)
      && toolboxStack.getItem() instanceof BlockItem;
    if (state.carryingToolbox) {
      state.toolboxColor = toolbox.getColor();
      state.toolboxBlockState = ((BlockItem) toolboxStack.getItem()).getBlock().defaultBlockState();
      state.toolboxLidAngle = toolbox.lid.getValue(partialTicks);
      state.toolboxDrawerOffset = toolbox.drawers.getValue(partialTicks);
    }

    state.visualBaseState = null;
    state.visualBaseRenderer = null;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  protected void setupRotations(@NotNull ConductorRenderState state, @NotNull PoseStack poseStack, float bodyRot, float scale) {
    if (state.visualBaseState != null && state.visualBaseRenderer != null) {
      ((AccessorLivingEntityRenderer) state.visualBaseRenderer).callSetupRotations(state.visualBaseState, poseStack, bodyRot, scale);
      return;
    }
    super.setupRotations(state, poseStack, bodyRot, scale);
  }

  /** Mirrors how vanilla extracts the worn head item, for the conductor's secondary head slot. */
  private static void extractWornHead(ItemStack stack, LivingEntityRenderState head, ConductorEntity conductor,
                                      ItemModelResolver itemModelResolver) {
    head.headItem.clear();
    head.wornHeadType = null;
    head.wornHeadProfile = null;
    if (stack.isEmpty())
      return;
    if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skull) {
      head.wornHeadType = skull.getType();
      head.wornHeadProfile = stack.get(DataComponents.PROFILE);
    } else if (!HumanoidArmorLayer.shouldRender(stack, EquipmentSlot.HEAD)) {
      itemModelResolver.updateForLiving(head.headItem, stack, ItemDisplayContext.HEAD, conductor);
    }
  }
}
