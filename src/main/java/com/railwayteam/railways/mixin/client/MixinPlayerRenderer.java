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

package com.railwayteam.railways.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.compat.Mods;
import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.content.conductor.ConductorRenderState;
import com.railwayteam.railways.content.conductor.ConductorRenderer;
import com.railwayteam.railways.mixin_interfaces.IConductorDisguiseState;
import com.railwayteam.railways.registry.CREntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws players wearing a disguise as a conductor.
 *
 * <p>PORT-NOTE(26.1): rendering is split into extraction and submission. The disguise conductor is extracted alongside
 * the player into {@link IConductorDisguiseState}, with the player's pose, light and name copied over, and
 * {@link MixinLivingEntityRenderer} submits it in place of the player. First person hands no longer get the player, so
 * they check the local player. The 1.20 shadow shrink is not ported: shadows are extracted before this runs.</p>
 */
@Mixin(AvatarRenderer.class)
public abstract class MixinPlayerRenderer {
    @Unique
    private ConductorEntity railways$visualEntity;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL"))
    private void railways$extractDisguise(Avatar avatar, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        IConductorDisguiseState disguiseState = (IConductorDisguiseState) state;
        ConductorRenderer conductorRenderer;
        if (!(avatar instanceof AbstractClientPlayer player) || !ConductorEntity.isPlayerDisguised(player)
            || (player.isLocalPlayer() && Minecraft.getInstance().options.getCameraType().isFirstPerson() && Mods.FIGURA.isLoaded)
            || (conductorRenderer = railways$getConductorRenderer(player)) == null) {
            disguiseState.railways$setDisguise(null);
            return;
        }

        ConductorRenderState disguise = disguiseState.railways$getDisguise();
        if (disguise == null)
            disguise = conductorRenderer.createRenderState();
        conductorRenderer.extractRenderState(railways$visualEntity, disguise, partialTicks);

        disguise.isCrouching = state.isCrouching;
        disguise.swimAmount = state.swimAmount;
        disguise.isVisuallySwimming = state.isVisuallySwimming;
        disguise.isFallFlying = state.isFallFlying;
        disguise.isPassenger = state.isPassenger;
        disguise.lightCoords = state.lightCoords;
        disguise.outlineColor = state.outlineColor;
        disguise.distanceToCameraSq = state.distanceToCameraSq;
        disguise.nameTag = state.nameTag;
        disguise.nameTagAttachment = state.nameTagAttachment;
        disguise.scoreText = state.scoreText;
        disguise.visualBaseState = state;
        disguise.visualBaseRenderer = (LivingEntityRenderer<?, ?, ?>) (Object) this;
        disguiseState.railways$setDisguise(disguise);
    }

    @Unique
    private @Nullable ConductorRenderer railways$getConductorRenderer(AbstractClientPlayer player) {
        if (railways$visualEntity == null || railways$visualEntity.level() != player.level())
            railways$visualEntity = new ConductorEntity(CREntities.CONDUCTOR.get(), player.level());
        railways$transformVisualConductor(player, railways$visualEntity);
        railways$visualEntity.visualBaseEntity = player;
        return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(railways$visualEntity) instanceof ConductorRenderer renderer
            ? renderer : null;
    }

    @Unique
    private static void railways$transformVisualConductor(AbstractClientPlayer player, ConductorEntity conductor) {
        conductor.xo = player.xo;
        conductor.yo = player.yo;
        conductor.zo = player.zo;
        conductor.xOld = player.xOld;
        conductor.yOld = player.yOld;
        conductor.zOld = player.zOld;
        conductor.xRotO = player.xRotO;
        conductor.yRotO = player.yRotO;
        ((AccessorEntity) conductor).setXRot(player.getXRot());
        ((AccessorEntity) conductor).setYRot(player.getYRot());

        conductor.yHeadRot = player.yHeadRot;
        conductor.yBodyRot = player.yBodyRot;
        conductor.yBodyRotO = player.yBodyRotO;
        conductor.yHeadRotO = player.yHeadRotO;
        ((AccessorWalkAnimationState) conductor.walkAnimation).setPosition(player.walkAnimation.position());
        conductor.walkAnimation.setSpeed(player.walkAnimation.speed());
        ((AccessorWalkAnimationState) conductor.walkAnimation).setSpeedOld(((AccessorWalkAnimationState) player.walkAnimation).getSpeedOld());

        conductor.tickCount = player.tickCount;

        conductor.setOnGround(player.onGround());

        conductor.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD));
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() == Items.ELYTRA) {
            conductor.setItemSlot(EquipmentSlot.CHEST, chest);
        } else {
            conductor.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        }

        conductor.setSharedFlag(7, player.isFallFlying());
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void railways$renderRightHand(PoseStack poseStack, SubmitNodeCollector queue, int light, Identifier skinTexture,
                                          boolean sleeveVisible, CallbackInfo ci) {
        railways$renderHand(poseStack, queue, light, true, ci);
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void railways$renderLeftHand(PoseStack poseStack, SubmitNodeCollector queue, int light, Identifier skinTexture,
                                         boolean sleeveVisible, CallbackInfo ci) {
        railways$renderHand(poseStack, queue, light, false, ci);
    }

    @Unique
    private void railways$renderHand(PoseStack poseStack, SubmitNodeCollector queue, int light, boolean right, CallbackInfo ci) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || !ConductorEntity.isPlayerDisguised(player))
            return;
        ConductorRenderer conductorRenderer = railways$getConductorRenderer(player);
        if (conductorRenderer == null)
            return;
        ci.cancel();

        ModelPart arm = right ? conductorRenderer.getModel().rightArm : conductorRenderer.getModel().leftArm;
        arm.resetPose();
        poseStack.pushPose();
        poseStack.translate(0, -0.3, 0);
        Identifier texture = ConductorRenderer.getTextureLocation(railways$visualEntity.getItemBySlot(EquipmentSlot.HEAD),
            railways$visualEntity.getCustomName());
        queue.submitModelPart(arm, poseStack, RenderTypes.entitySolid(texture), light, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }
}
