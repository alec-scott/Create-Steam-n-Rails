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

package com.railwayteam.railways.content.schedule;

import com.railwayteam.railways.annotation.multiloader.MultiLoaderEvent;
import com.railwayteam.railways.mixin.AccessorScheduleRuntime;
import com.zurrtum.create.Create;
import com.zurrtum.create.content.redstone.link.IRedstoneLinkable;
import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.zurrtum.create.content.trains.entity.Carriage;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.graph.DiscoveredPath;
import com.zurrtum.create.content.trains.schedule.ScheduleRuntime;
import com.zurrtum.create.content.trains.schedule.destination.ScheduleInstruction;
import com.zurrtum.create.catnip.data.Couple;
import com.zurrtum.create.catnip.data.WorldAttached;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// PORT-NOTE(26.1): UI (summary, icons, editor) moved to the client RedstoneLinkInstructionRender, as in Create Fly.
public class RedstoneLinkInstruction extends ScheduleInstruction {
    private static final Codec<Couple<Frequency>> FREQUENCY_CODEC = Couple.codec(Frequency.CODEC);

    public static WorldAttached<List<CustomRedstoneActor>> customActors =
        new WorldAttached<>($ -> new ArrayList<>());

    @MultiLoaderEvent
    public static void tick(Level world) {
        List<CustomRedstoneActor> actors = customActors.get(world);
        for (Iterator<CustomRedstoneActor> actorIterator = actors.iterator(); actorIterator.hasNext();) {
            CustomRedstoneActor actor = actorIterator.next();
            actor.decrement();
            if (!actor.isAlive()) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, actor);
                actorIterator.remove();
            }
        }
    }

    public Couple<Frequency> freq;

    public RedstoneLinkInstruction(Identifier id) {
        super(id);
        freq = Couple.create(() -> RedstoneLinkNetworkHandler.Frequency.EMPTY);
        data.putInt("Power", 15);
    }

    @Override
    public boolean supportsConditions() {
        return false;
    }

    @Override
    protected void writeAdditional(ValueOutput view) {
        view.store("Frequency", FREQUENCY_CODEC, freq);
    }

    @Override
    protected void readAdditional(ValueInput view) {
        freq = view.read("Frequency", FREQUENCY_CODEC).orElseGet(() -> Couple.create(() -> RedstoneLinkNetworkHandler.Frequency.EMPTY));
    }

    @Override
    public @Nullable DiscoveredPath start(ScheduleRuntime runtime, Level level) {
        Train train = ((AccessorScheduleRuntime) runtime).getTrain();
        Carriage carriage = train.carriages.get(0);
        CustomRedstoneActor actor = new CustomRedstoneActor(carriage);
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, actor);
        customActors.get(level).add(actor);
        //Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, actor);

        runtime.state = ScheduleRuntime.State.PRE_TRANSIT;
        runtime.currentEntry++;
        return null;
    }

    private final class CustomRedstoneActor implements IRedstoneLinkable {
        private final Carriage carriage;
        private long ticks = 8;

        private CustomRedstoneActor(Carriage carriage) {
            this.carriage = carriage;
        }

        public void decrement() {
            ticks--;
        }

        @Override
        public int getTransmittedStrength() {
            return isAlive() ? intData("Power") : 0;
        }

        @Override
        public void setReceivedStrength(int power) {
        }

        @Override
        public boolean isListening() {
            return false;
        }

        @Override
        public boolean isAlive() {
            return ticks > 0;
        }

        @Override
        public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
            return freq;
        }

        @Override
        public BlockPos getLocation() {
            return BlockPos.containing(carriage.getLeadingPoint().getPosition(carriage.train.graph));
        }
    }
}
