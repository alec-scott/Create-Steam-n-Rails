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
package com.railwayteam.railways.registry.fabric;

import net.minecraft.core.component.DataComponentMap;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.annotation.multiloader.ImplClass;
import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.content.palettes.painting.PaintFluid;
import com.railwayteam.railways.registry.CRDataComponents;
import com.railwayteam.railways.registry.CRFluids;
import com.zurrtum.create.client.AllFluidConfigs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.network.chat.Component;

@ImplClass
public class CRFluidsImpl {
    public static void registerAttributes() {
        PaintFluidVariantAttributeHandler handler = new PaintFluidVariantAttributeHandler();
        FluidVariantAttributes.register(CRFluids.PAINT, handler);
        FluidVariantAttributes.register(CRFluids.PAINT.getFlowing(), handler);
    }

    /**
     * PORT-NOTE(fabric): upstream swapped in a texture per paint color. Create Fly renders fluids from one model per
     * fluid ({@link AllFluidConfigs}), so paint uses the white paint texture tinted with the color's diffuse color.
     */
    @Environment(EnvType.CLIENT)
    public static void initRendering() {
        AllFluidConfigs.MODEL.put(CRFluids.PAINT, new FluidModel.Unbaked(
            new Material(Railways.asResource("fluid/paint_still/white")),
            new Material(Railways.asResource("fluid/paint_flow/white")), null, null));
        AllFluidConfigs.tint(CRFluids.PAINT, (fluid, components) -> {
            PalettesColor color = components.get(DataComponentMap.EMPTY, CRDataComponents.PAINT_COLOR);
            PalettesColor paint = color == null ? PalettesColor.NETHERITE : color;
            return 0xFF000000 | paint.getDiffuseColor();
        });
    }

    public static class PaintFluidVariantAttributeHandler implements FluidVariantAttributeHandler {
        @Override
        public Component getName(FluidVariant fluidVariant) {
            return Component.translatable(PaintFluid.getColor(fluidVariant)
                .map(PalettesColor::getPaintNameId)
                .orElse("fluid.railways.paint"));
        }
    }
}
