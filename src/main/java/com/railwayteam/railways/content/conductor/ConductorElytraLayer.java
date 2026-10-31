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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import org.jetbrains.annotations.NotNull;

public class ConductorElytraLayer extends WingsLayer<ConductorRenderState, ConductorEntityModel> {
    public ConductorElytraLayer(RenderLayerParent<ConductorRenderState, ConductorEntityModel> renderer, EntityModelSet loader,
                                EquipmentLayerRenderer equipmentRenderer) {
        super(renderer, loader, equipmentRenderer);
    }

    @Override
    public void submit(@NotNull PoseStack matrixStack, @NotNull SubmitNodeCollector queue, int packedLight,
                       @NotNull ConductorRenderState state, float yRot, float xRot) {
        matrixStack.pushPose();
        matrixStack.scale(0.7f, 0.7f, 0.7f);
        matrixStack.translate(0, 16 / 16.0, 0);
        super.submit(matrixStack, queue, packedLight, state, yRot, xRot);
        matrixStack.popPose();
    }
}
