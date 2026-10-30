/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2025 The Railways Team
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

package com.railwayteam.railways.registry;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.schedule.RedstoneLinkInstruction;
import com.railwayteam.railways.content.schedule.StationLoadedCondition;
import com.railwayteam.railways.content.schedule.WaypointDestinationInstruction;
import com.railwayteam.railways.content.schedule.client.RedstoneLinkInstructionRender;
import com.railwayteam.railways.content.schedule.client.StationLoadedConditionRender;
import com.zurrtum.create.client.AllScheduleRenders;
import com.zurrtum.create.client.content.trains.schedule.destination.DestinationInstructionRender;
import com.zurrtum.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.zurrtum.create.content.trains.schedule.destination.ScheduleInstruction;
import com.zurrtum.create.catnip.data.Pair;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

import static com.zurrtum.create.AllSchedules.CONDITION_TYPES;
import static com.zurrtum.create.AllSchedules.INSTRUCTION_TYPES;

public class CRSchedule {
    public static final Identifier REDSTONE_LINK = registerInstruction("redstone_link", RedstoneLinkInstruction::new);
    public static final Identifier WAYPOINT_DESTINATION = registerInstruction("waypoint_destination", WaypointDestinationInstruction::new);
    public static final Identifier LOADED = registerCondition("loaded", StationLoadedCondition::new);

    private static Identifier registerInstruction(String name, Function<Identifier, ? extends ScheduleInstruction> factory) {
        Identifier id = Railways.asResource(name);
        INSTRUCTION_TYPES.add(Pair.of(id, factory));
        return id;
    }

    private static Identifier registerCondition(String name, Function<Identifier, ? extends ScheduleWaitCondition> factory) {
        Identifier id = Railways.asResource(name);
        CONDITION_TYPES.add(Pair.of(id, factory));
        return id;
    }

    /** PORT-NOTE(26.1): schedule UI is client side in Create Fly ({@link AllScheduleRenders}); call during client init. */
    @Environment(EnvType.CLIENT)
    public static void registerRenders() {
        AllScheduleRenders.ALL.put(REDSTONE_LINK, new RedstoneLinkInstructionRender());
        AllScheduleRenders.ALL.put(WAYPOINT_DESTINATION, new DestinationInstructionRender());
        AllScheduleRenders.ALL.put(LOADED, new StationLoadedConditionRender());
    }

    public static void register() {}
}
