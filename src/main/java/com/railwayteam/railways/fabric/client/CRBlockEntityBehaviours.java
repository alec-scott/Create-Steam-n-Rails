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

import com.railwayteam.railways.content.coupling.coupler.client.TrackCouplerClientBehaviours;
import com.railwayteam.railways.content.switches.client.TrackSwitchClientBehaviours;
import com.railwayteam.railways.content.fuel.tank.client.FuelTankTooltip;
import com.railwayteam.railways.registry.fabric.CRBlockEntitiesImpl;
import com.railwayteam.railways.registry.CRBlockEntities;
import com.zurrtum.create.client.AllBlockEntityBehaviours;

import java.util.List;

/**
 * Client-only block entity behaviours (value boxes, goggle tooltips, ...).
 *
 * <p>PORT-NOTE(fabric): Create Fly keeps these out of the block entities; they're attached on the client through
 * {@code BlockEntityBehaviour.CLIENT_REGISTRY}. Call once during client init.</p>
 */
public final class CRBlockEntityBehaviours {
    private CRBlockEntityBehaviours() {}

    public static void register() {
        AllBlockEntityBehaviours.add(CRBlockEntities.TRACK_COUPLER.get(),
            TrackCouplerClientBehaviours::edgeSpacing, TrackCouplerClientBehaviours.Tooltip::new);
        for (var type : List.of(CRBlockEntities.ANDESITE_SWITCH.get(), CRBlockEntities.BRASS_SWITCH.get()))
            AllBlockEntityBehaviours.add(type, TrackSwitchClientBehaviours::autoMode, TrackSwitchClientBehaviours.Tooltip::new);
        AllBlockEntityBehaviours.add(CRBlockEntitiesImpl.FUEL_TANK.get(), FuelTankTooltip::new);
    }
}
