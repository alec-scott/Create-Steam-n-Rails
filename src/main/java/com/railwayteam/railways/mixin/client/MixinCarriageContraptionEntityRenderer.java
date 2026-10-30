/*
 * Steam 'n' Rails
 * Copyright (c) 2025 The Railways Team
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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.railwayteam.railways.content.custom_bogeys.renderer.unified.BogeyDisplay;
import com.railwayteam.railways.content.custom_bogeys.renderer.unified.BogeyDisplayHolder;
import com.zurrtum.create.client.AllBogeyStyleRenders;
import com.zurrtum.create.client.content.trains.bogey.SizeRenderer;
import com.zurrtum.create.client.content.trains.entity.CarriageContraptionEntityRenderer;
import com.zurrtum.create.client.content.trains.entity.CarriageContraptionEntityRenderer.CarriageBogeyRenderState;
import com.zurrtum.create.client.content.trains.entity.CarriageContraptionEntityRenderer.CarriageContraptionState;
import com.zurrtum.create.content.trains.bogey.BogeySize;
import com.zurrtum.create.content.trains.entity.CarriageBogey;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.world.level.CardinalLighting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

// PORT-NOTE(26.1): bogeys are rendered from extracted state; displays get the entity just before extraction.
@Mixin(CarriageContraptionEntityRenderer.class)
public class MixinCarriageContraptionEntityRenderer {
    @WrapOperation(method = "extractRenderState(Lcom/zurrtum/create/content/trains/entity/CarriageContraptionEntity;Lcom/zurrtum/create/client/content/trains/entity/CarriageContraptionEntityRenderer$CarriageContraptionState;F)V",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/content/trains/entity/CarriageContraptionEntityRenderer$CarriageBogeyRenderState;create(Lcom/zurrtum/create/content/trains/entity/CarriageBogey;FFIFFILnet/minecraft/world/level/CardinalLighting;F)Lcom/zurrtum/create/client/content/trains/entity/CarriageContraptionEntityRenderer$CarriageBogeyRenderState;"),
        remap = false)
    private CarriageBogeyRenderState updateEntity(CarriageBogey bogey, float viewXRot, float viewYRot, int bogeySpacing,
                                                  float yaw, float pitch, int light, CardinalLighting cardinalLighting,
                                                  float tickProgress, Operation<CarriageBogeyRenderState> original,
                                                  CarriageContraptionEntity entity, CarriageContraptionState state,
                                                  float outerTickProgress) {
        Map<BogeySize, SizeRenderer> renderers = AllBogeyStyleRenders.ALL.get(bogey.getStyle().id);
        SizeRenderer renderer = renderers == null ? null : renderers.get(bogey.getSize());
        if (renderer != null && renderer.renderer() instanceof BogeyDisplayHolder holder) {
            holder.runWithDisplay(display -> {
                if (display instanceof BogeyDisplay.EntityAware entityAware) {
                    entityAware.entityUpdate(entity);
                }
            });
        }
        return original.call(bogey, viewXRot, viewYRot, bogeySpacing, yaw, pitch, light, cardinalLighting, tickProgress);
    }
}
