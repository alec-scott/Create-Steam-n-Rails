/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
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

import com.railwayteam.railways.content.switches.TrackSwitch;
import com.railwayteam.railways.content.switches.TrackSwitchDebugVisualizer;
import com.railwayteam.railways.content.train_debug.TravellingPointVisualizer;
import com.railwayteam.railways.registry.CREdgePointTypes;
import com.railwayteam.railways.util.Utils;
import com.zurrtum.create.Create;
import com.zurrtum.create.client.content.kinetics.KineticDebugger;
import com.zurrtum.create.client.content.trains.GlobalRailwayManagerClient;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PORT-NOTE(26.1): Create Fly ticks the client railway manager through the static {@code GlobalRailwayManagerClient.tick}. */
@Mixin(GlobalRailwayManagerClient.class)
public abstract class MixinGlobalRailwayManager {
    @Inject(method = "tick", at = @At("HEAD"))
    private static void showTrainDebug(Minecraft mc, CallbackInfo ci) {
        if (KineticDebugger.isActive() && Utils.isDevEnv())
            for (Train train : Create.RAILWAYS.trains.values())
                TravellingPointVisualizer.debugTrain(train);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private static void showSwitchDebug(Minecraft mc, CallbackInfo ci) {
        if (KineticDebugger.isF3DebugModeActive()) {
            for (TrackGraph graph : Create.RAILWAYS.trackNetworks.values()) {
                for (TrackSwitch sw: graph.getPoints(CREdgePointTypes.SWITCH)) {
                    TrackSwitchDebugVisualizer.visualizeSwitchExits(sw);
                }
            }
        }
    }
}
