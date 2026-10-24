/*
 * Steam 'n' Rails
 * Copyright (c) 2026 The Railways Team
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
package com.railwayteam.railways.fabric.client;

import com.railwayteam.railways.content.animated_flywheel.FlywheelMovementBehaviour;
import com.railwayteam.railways.content.animated_flywheel.client.FlywheelMovementRender;
import com.railwayteam.railways.content.smokestack.SmokeStackMovementBehaviour;
import com.railwayteam.railways.content.smokestack.client.SmokeStackMovementRender;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Attaches client movement renderers to Railways' movement behaviours.
 *
 * <p>PORT-NOTE(fabric): Create Fly keeps contraption rendering out of {@link MovementBehaviour}; client code sets
 * {@code attachRender} (as its {@code AllMovementRenders} does). Call once during client init.</p>
 */
public final class CRMovementRenders {
    private CRMovementRenders() {}

    public static void register() {
        BuiltInRegistries.BLOCK.forEach(block -> {
            MovementBehaviour behaviour = MovementBehaviour.REGISTRY.get(block);
            if (behaviour == null || behaviour.attachRender != null)
                return;
            if (behaviour instanceof FlywheelMovementBehaviour)
                behaviour.attachRender = new FlywheelMovementRender();
            else if (behaviour instanceof SmokeStackMovementBehaviour)
                behaviour.attachRender = new SmokeStackMovementRender();
        });
    }
}
