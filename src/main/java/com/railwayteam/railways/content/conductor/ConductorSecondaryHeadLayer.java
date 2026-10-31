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
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jetbrains.annotations.NotNull;

/**
 * Renders the conductor's secondary head item (heads, skulls and other non-armor items) on top of its cap.
 *
 * <p>PORT-NOTE(26.1): the 1.20 layer was a copy of vanilla's head layer reading from a different slot. The secondary
 * head is now extracted into its own {@link LivingEntityRenderState} ({@link ConductorRenderState#secondaryHead}) and
 * drawn by a vanilla {@link CustomHeadLayer}; that layer only reads the head part from its parent model, so the parent
 * is shared through an unchecked cast.</p>
 */
public class ConductorSecondaryHeadLayer extends RenderLayer<ConductorRenderState, ConductorEntityModel> {
    private final CustomHeadLayer<LivingEntityRenderState, ?> headLayer;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ConductorSecondaryHeadLayer(RenderLayerParent<ConductorRenderState, ConductorEntityModel> renderer,
                                       EntityModelSet modelSet, PlayerSkinRenderCache playerSkinRenderCache) {
        super(renderer);
        this.headLayer = new CustomHeadLayer<>((RenderLayerParent) renderer, modelSet, playerSkinRenderCache);
    }

    @Override
    public void submit(@NotNull PoseStack matrixStack, @NotNull SubmitNodeCollector queue, int packedLight,
                       @NotNull ConductorRenderState state, float yRot, float xRot) {
        LivingEntityRenderState head = state.secondaryHead;
        if (head.headItem.isEmpty() && head.wornHeadType == null)
            return;
        head.isBaby = state.isBaby;
        head.outlineColor = state.outlineColor;
        headLayer.submit(matrixStack, queue, packedLight, head, yRot, xRot);
    }
}
