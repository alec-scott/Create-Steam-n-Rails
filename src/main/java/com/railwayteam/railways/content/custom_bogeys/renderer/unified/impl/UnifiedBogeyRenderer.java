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

package com.railwayteam.railways.content.custom_bogeys.renderer.unified.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.content.custom_bogeys.renderer.unified.BogeyDisplay;
import com.railwayteam.railways.content.custom_bogeys.renderer.unified.BogeyDisplayHolder;
import com.zurrtum.create.catnip.data.Couple;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.trains.bogey.BogeyBlockEntityRenderer.BogeyRenderState;
import com.zurrtum.create.client.content.trains.bogey.BogeyRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * PORT-NOTE(26.1): Create Fly's bogey renderers extract a {@link BogeyRenderState} that is submitted later, instead of
 * drawing into a buffer. Elements are laid out relative to the bogey (identity base pose) and each one is captured as a
 * {@link SuperByteBufferRenderState} carrying its own transform.
 */
@ApiStatus.Internal
public class UnifiedBogeyRenderer implements BogeyRenderer, BogeyDisplayHolder {
    private final Couple<Renderer> renderers;
    private final @Nullable BogeyRenderer customRenderer;

    public UnifiedBogeyRenderer(BogeyDisplay.Factory factory) {
        this.renderers = Couple.createWithContext(inContraption -> Renderer.create(factory, inContraption));
        this.customRenderer = factory.createCustomRenderer();
    }

    @Override
    public void runWithDisplay(Consumer<BogeyDisplay> consumer) {
        consumer.accept(renderers.getFirst().display);
        consumer.accept(renderers.getSecond().display);
        if (customRenderer instanceof BogeyDisplayHolder customDisplayHolder) {
            customDisplayHolder.runWithDisplay(consumer);
        }
    }

    @Override
    public BogeyRenderState getRenderData(CompoundTag bogeyData, float wheelAngle, float tickProgress, int light,
                                          @Nullable CardinalLighting cardinalLighting, boolean inContraption) {
        final BlockState air = Blocks.AIR.defaultBlockState();
        final Renderer renderer = renderers.get(inContraption);

        // prepare
        for (RenderedElement element : renderer.allElements) {
            element.pose.identity();
        }

        // update
        renderer.display.update(bogeyData, wheelAngle);

        // extract
        List<SuperByteBufferRenderState> parts = new ArrayList<>();
        for (RenderedElement.Single element : renderer.singleElements) {
            parts.add(extract(CachedBuffers.partial(element.model(), air), element.element(), light, cardinalLighting));
        }

        for (RenderedElement.Multiple elements : renderer.multipleElements) {
            for (RenderedElement element : elements.elements()) {
                parts.add(extract(CachedBuffers.partial(elements.model(), air), element, light, cardinalLighting));
            }
        }

        for (RenderedElement.Scrolling element : renderer.scrollingElements) {
            float spriteSize = element.entry.getTarget().getV1() - element.entry.getTarget().getV0();
            float scrollV = element.shiftV;
            scrollV = scrollV - Mth.floor(scrollV);
            scrollV = scrollV * spriteSize * 0.5f;
            parts.add(extract(CachedBuffers.partial(element.model, air).shiftUVScrolling(element.entry, scrollV),
                element.element, light, cardinalLighting));
        }

        BogeyRenderState custom = customRenderer == null ? null
            : customRenderer.getRenderData(bogeyData, wheelAngle, tickProgress, light, cardinalLighting, inContraption);
        return new UnifiedBogeyRenderState(parts, custom);
    }

    private static SuperByteBufferRenderState extract(SuperByteBuffer sbb, RenderedElement element, int light,
                                                      @Nullable CardinalLighting cardinalLighting) {
        return sbb.mulPose(element.pose)
            .mulNormal(new Matrix3f(element.pose))
            .cardinalLighting(cardinalLighting)
            .light(light)
            .extractRenderState();
    }

    private record UnifiedBogeyRenderState(List<SuperByteBufferRenderState> parts,
                                           @Nullable BogeyRenderState custom) implements BogeyRenderState {
        @Override
        public void submit(PoseStack matrices, SubmitNodeCollector queue) {
            for (SuperByteBufferRenderState part : parts) {
                part.submit(matrices, queue);
            }
            if (custom != null) {
                custom.submit(matrices, queue);
            }
        }
    }

    private record Renderer(
        BogeyDisplay display,
        List<RenderedElement.Single> singleElements,
        List<RenderedElement.Multiple> multipleElements,
        List<RenderedElement.Scrolling> scrollingElements,
        List<RenderedElement> allElements
    ) {
        private static Renderer create(BogeyDisplay.Factory factory, boolean inContraption) {
            ArrayList<RenderedElement.Single> singleElements = new ArrayList<>();
            ArrayList<RenderedElement.Multiple> multipleElements = new ArrayList<>();
            ArrayList<RenderedElement.Scrolling> scrollingElements = new ArrayList<>();
            var prov = new RenderedElementProvider(singleElements, multipleElements, scrollingElements);

            BogeyDisplay display = factory.create(prov, inContraption);
            prov.freeze();

            List<RenderedElement> allElements = new ArrayList<>();
            singleElements.forEach(s -> allElements.add(s.element()));
            multipleElements.forEach(m -> Collections.addAll(allElements, m.elements()));
            scrollingElements.forEach(s -> allElements.add(s.element));

            return new Renderer(display, singleElements, multipleElements, scrollingElements, allElements);
        }
    }
}
