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

package com.railwayteam.railways.content.switches;

import com.railwayteam.railways.content.switches.TrackSwitchBlock.SwitchState;
import com.railwayteam.railways.mixin_interfaces.ISwitchDisabledEdge;
import com.railwayteam.railways.registry.CREdgePointTypes;
import com.zurrtum.create.Create;
import com.zurrtum.create.content.trains.graph.DimensionPalette;
import com.zurrtum.create.content.trains.graph.EdgePointType;
import com.zurrtum.create.content.trains.graph.TrackEdge;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.graph.TrackNode;
import com.zurrtum.create.content.trains.graph.TrackNodeLocation;
import com.zurrtum.create.content.trains.signal.SignalPropagator;
import com.zurrtum.create.content.trains.signal.SingleBlockEntityEdgePoint;
import com.zurrtum.create.catnip.data.Couple;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/*
done: actually disable edges that aren't the active target
in Navigation.java we need to hook into this: - actually don't anymore
```java
if (!validTypes.contains(target.getValue().getTrackMaterial().trackType))
    continue;
```

and in Train.java we need to hook into this:
```java
blocked |= carriage.blocked || carriage.isOnIncompatibleTrack();
```
actually hooked into Carriage#isOnIncompatibleTrack


done: don't actually block, just remove it from the list of things we can go toward
to do this:
hook into {@link TrackEdge#canTravelTo }
 */
@SuppressWarnings("DuplicatedCode")
public class TrackSwitch extends SingleBlockEntityEdgePoint {
    private TrackNodeLocation switchPoint;
    private final List<TrackNodeLocation> exits = new ArrayList<>();

    private static int selectionPriorityTicker = 0;

    @ApiStatus.Internal
    public static int getSelectionPriority() {
        selectionPriorityTicker++;
        return selectionPriorityTicker;
    }

    public TrackSwitch() {
    }

    private @Nullable TrackNodeLocation straightExit;
    private @Nullable TrackNodeLocation leftExit;
    private @Nullable TrackNodeLocation rightExit;

    private @NotNull SwitchState switchState = SwitchState.NORMAL;
    private boolean automatic;
    private boolean locked;
    private boolean autoTrainsSwitch;

    public boolean shouldAutoTrainsSwitch() {
        return autoTrainsSwitch;
    }

    void setAutoTrainsSwitch(boolean autoTrainsSwitch) {
        this.autoTrainsSwitch = autoTrainsSwitch;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public boolean isLocked() {
        return locked;
    }

    void setLocked(boolean locked) {
        this.locked = locked;
    }

    private @Nullable TrackNodeLocation getExit(SwitchState state) {
        return switch (state) {
            case NORMAL -> straightExit;
            case REVERSE_RIGHT -> rightExit;
            case REVERSE_LEFT -> leftExit;
        };
    }

    private Map<SwitchState, Optional<TrackNodeLocation>> getExistingExits() {
        return Map.of(
                SwitchState.NORMAL, Optional.ofNullable(straightExit),
                SwitchState.REVERSE_LEFT, Optional.ofNullable(leftExit),
                SwitchState.REVERSE_RIGHT, Optional.ofNullable(rightExit)
        );
    }

    /**
     * @param loc location to approach from
     * @return (if found) state where `loc` can be traversed
     */
    @ApiStatus.Internal
    public @Nullable SwitchState getTargetState(TrackNodeLocation loc) {
        for (Map.Entry<SwitchState, Optional<TrackNodeLocation>> entry : getExistingExits().entrySet()) {
            if (entry.getValue().isPresent() && entry.getValue().get().equals(loc)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public EdgePointType<?> getType() {
        return CREdgePointTypes.SWITCH;
    }

    @Override
    public boolean canCoexistWith(EdgePointType<?> otherType, boolean front) {
        return otherType == EdgePointType.SIGNAL;
    }

    @Override
    public void blockEntityAdded(BlockEntity tile, boolean front) {
        super.blockEntityAdded(tile, front);

        if (tile instanceof TrackSwitchBlockEntity te) {
            te.calculateExits(this);
            automatic = te.isAutomatic();
            locked = te.isLocked();
        }

        notifyTrains(tile.getLevel());
    }

    @Override
    public void onRemoved(MinecraftServer server, TrackGraph graph) {
        exits.clear();
        sortExits();
        removeFromAllGraphs(server);
    }

    private void notifyTrains(Level level) {
        TrackGraph graph = Create.RAILWAYS.sided(level).getGraph(edgeLocation.getFirst());
        if (graph == null)
            return;
        TrackEdge edge = graph.getConnection(edgeLocation.map(graph::locateNode));
        if (edge == null)
            return;
        SignalPropagator.notifyTrains(graph, edge);
    }

    void updateExits(TrackNodeLocation switchPoint, Collection<TrackNodeLocation> newExits) {
        this.switchPoint = switchPoint;

        if (edgeLocation == null) {
            exits.clear();
            return;
        }
        Vec3 forward = edgeLocation.getFirst().getLocation().vectorTo(edgeLocation.getSecond().getLocation());
        exits.clear();
        exits.addAll(newExits.stream()
                // Exit should be in the same direction switch is facing
                .filter(e -> forward.dot(switchPoint.getLocation().vectorTo(e.getLocation())) > 0)
                .toList());
        sortExits();
        ensureValidState();
    }

    private void sortExits() {
        Vec3 forward = edgeLocation.getFirst().getLocation().vectorTo(edgeLocation.getSecond().getLocation()).normalize();
        exits.sort(Comparator.comparing(e -> {
            // Relative exit directions -- 0 is straight on, negative for left, positive for right
            Vec3 exitDir = switchPoint.getLocation().vectorTo(e.getLocation());
            return forward.x * exitDir.z - forward.z * exitDir.x;
        }));

        if (exits.size() == 1) {
            leftExit = null;
            straightExit = exits.get(0);
            rightExit = null;
        } else if (exits.size() == 2) {
            Vec3 firstExitDir = switchPoint.getLocation().vectorTo(exits.get(0).getLocation()).normalize();
            Vec3 secondExitDir = switchPoint.getLocation().vectorTo(exits.get(1).getLocation()).normalize();

            double firstExitRelativeDir = forward.x * firstExitDir.z - forward.z * firstExitDir.x;
            double secondExitRelativeDir = forward.x * secondExitDir.z - forward.z * secondExitDir.x;

            double cutoff = 0.2;

            // Determine which exit is left/right/straight based on relative exit directions
            // 0.2 *should* be straight enough, maybe
            if (firstExitRelativeDir < 0 && secondExitRelativeDir <= cutoff) {
                //    / /         /
                // --+-'   or  --+---  = left, straight
                leftExit = exits.get(0);
                straightExit = exits.get(1);
                rightExit = null;
            } else if (firstExitRelativeDir >= -cutoff && secondExitRelativeDir > 0) {
                // --+-.       --+---
                //    \ \  or     \    = right, straight
                leftExit = null;
                straightExit = exits.get(0);
                rightExit = exits.get(1);
            } else {
                //    /
                // --<   = left, right
                //    \
                leftExit = exits.get(0);
                straightExit = null;
                rightExit = exits.get(1);
            }
        } else if (exits.size() == 3) {
            leftExit = exits.get(0);
            straightExit = exits.get(1);
            rightExit = exits.get(2);
        } else {
            leftExit = null;
            straightExit = null;
            rightExit = null;
        }
    }

    @Nullable TrackNodeLocation getSwitchPoint() {
        return switchPoint;
    }

    Collection<TrackNodeLocation> getExits() {
        return exits.stream().toList();
    }

    private boolean isStateValid(SwitchState state) {
        if (state == null)
            return false;
        return switch (state) {
            case NORMAL -> hasStraightExit();
            case REVERSE_RIGHT -> hasRightExit();
            case REVERSE_LEFT -> hasLeftExit();
        };
    }

    private SwitchState getValidSwitchState() {
        for (SwitchState state : SwitchState.values()) {
            if (isStateValid(state))
                return state;
        }
        return SwitchState.NORMAL;
    }

    void ensureValidState() {
        if (!isStateValid(switchState)) {
            switchState = getValidSwitchState();
        }
    }

    /**
     * Tries to set state, but fails if locked
     * @param state Which state to attempt to switch to
     * @return success or failure
     */
    public boolean trySetSwitchState(@NotNull SwitchState state) {
        if (isLocked())
            return false;
        return setSwitchState(state);
    }

    public boolean setSwitchState(@NotNull SwitchState state) {
        if (isStateValid(state) && switchState != state) {
            switchState = state;
            ticks = 10000; // force a tick
            forceTickClient = true;
            return true;
        }
        return false;
    }

    public @NotNull SwitchState getSwitchState() {
        return switchState;
    }

    public @Nullable TrackNodeLocation getSwitchTarget() {
        //ensureValidState();
        return switch (switchState) {
            case NORMAL -> straightExit;
            case REVERSE_RIGHT -> rightExit;
            case REVERSE_LEFT -> leftExit;
        };
    }

    public boolean hasStraightExit() {
        return straightExit != null;
    }

    public boolean hasLeftExit() {
        return leftExit != null;
    }

    public boolean hasRightExit() {
        return rightExit != null;
    }

    @Override
    public void write(ValueOutput view, DimensionPalette dimensions) {
        super.write(view, dimensions);
        switchPoint.write(view.child("SwitchPoint"), dimensions);
        ValueOutput.ValueOutputList exitList = view.childrenList("Exits");
        for (TrackNodeLocation exit : exits)
            exit.write(exitList.addChild(), dimensions);
        view.putString("SwitchState", switchState.getSerializedName());
        view.putBoolean("Automatic", automatic);
        view.putBoolean("Locked", locked);
        view.putBoolean("AutoTrainsSwitch", autoTrainsSwitch);
    }

    @Override
    public <T> DataResult<T> encode(DynamicOps<T> ops, T empty, DimensionPalette dimensions) {
        DataResult<T> prefix = super.encode(ops, empty, dimensions);
        RecordBuilder<T> map = ops.mapBuilder();
        map.add("SwitchPoint", TrackNodeLocation.encode(switchPoint, ops, empty, dimensions));
        ListBuilder<T> exitList = ops.listBuilder();
        for (TrackNodeLocation exit : exits)
            exitList.add(TrackNodeLocation.encode(exit, ops, empty, dimensions));
        map.add("Exits", exitList.build(empty));
        map.add("SwitchState", ops.createString(switchState.getSerializedName()));
        map.add("Automatic", ops.createBoolean(automatic));
        map.add("Locked", ops.createBoolean(locked));
        map.add("AutoTrainsSwitch", ops.createBoolean(autoTrainsSwitch));
        return map.build(prefix);
    }

    @Override
    public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.write(buffer, dimensions);
        buffer.writeInt(switchState.ordinal());
        buffer.writeBoolean(automatic);
        buffer.writeBoolean(locked);
        switchPoint.send(buffer, dimensions);
        buffer.writeCollection(exits, (buf, e) -> e.send(buf, dimensions));
    }

    @Override
    public void read(ValueInput view, boolean migration, DimensionPalette dimensions) {
        super.read(view, migration, dimensions);
        readSwitchState(view.getStringOr("SwitchState", ""));
        automatic = view.getBooleanOr("Automatic", false);
        locked = view.getBooleanOr("Locked", false);
        autoTrainsSwitch = view.getBooleanOr("AutoTrainsSwitch", false);
        List<TrackNodeLocation> exitList = new ArrayList<>();
        for (ValueInput exit : view.childrenListOrEmpty("Exits"))
            exitList.add(TrackNodeLocation.read(exit, dimensions));
        updateExits(TrackNodeLocation.read(view.childOrEmpty("SwitchPoint"), dimensions), exitList);
    }

    @Override
    public <T> void decode(DynamicOps<T> ops, T input, boolean migration, DimensionPalette dimensions) {
        super.decode(ops, input, migration, dimensions);
        MapLike<T> map = ops.getMap(input).getOrThrow();
        readSwitchState(ops.getStringValue(map.get("SwitchState")).result().orElse(""));
        automatic = ops.getBooleanValue(map.get("Automatic")).result().orElse(false);
        locked = ops.getBooleanValue(map.get("Locked")).result().orElse(false);
        autoTrainsSwitch = ops.getBooleanValue(map.get("AutoTrainsSwitch")).result().orElse(false);
        List<TrackNodeLocation> exitList = new ArrayList<>();
        ops.getStream(map.get("Exits")).result().ifPresent(stream -> stream.forEach(e -> exitList.add(TrackNodeLocation.decode(ops, e, dimensions))));
        updateExits(TrackNodeLocation.decode(ops, map.get("SwitchPoint"), dimensions), exitList);
    }

    private void readSwitchState(String exit) {
        try {
            switchState = SwitchState.valueOf(exit.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            switchState = getValidSwitchState();
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.read(buffer, dimensions);
        switchState = SwitchState.values()[buffer.readInt()];
        automatic = buffer.readBoolean();
        locked = buffer.readBoolean();
        updateExits(
                TrackNodeLocation.receive(buffer, dimensions),
                buffer.readList(buf -> TrackNodeLocation.receive(buf, dimensions))
        );
    }

    private int ticks = 0;
    boolean forceTickClient = false;

    boolean doForceTickClient() {
        if (forceTickClient) {
            forceTickClient = false;
            return true;
        }
        return false;
    }

    @Override
    public void tick(MinecraftServer server, TrackGraph graph, boolean preTrains) {
        super.tick(server, graph, preTrains);
        if (preTrains) {
            ticks++;
            if (ticks < 10) {
                return;
            }
            ticks = 0;
            updateEdges(graph);
            if (automatic)
                switchForEdges(graph);
        }
    }

    public void updateEdges(TrackGraph graph) {
        updateEdges(graph, false);
    }

    public void setEdgesActive(TrackGraph graph) {
        updateEdges(graph, true);
    }

    private void updateEdges(TrackGraph graph, boolean forceActive) {
        TrackNodeLocation from = switchPoint;
        for (TrackNodeLocation to : exits) {
            if (to != null) {
                TrackNode toNode = graph.locateNode(to);
                Map<TrackNode, TrackEdge> connections = graph.getConnectionsFrom(toNode);
                if (connections == null)
                    continue;
                TrackNode closestFromNode = null;
                TrackEdge closestEdge = null;
                double closestDistance = Double.MAX_VALUE;
                for (Map.Entry<TrackNode, TrackEdge> otherEnd : connections.entrySet()) {
                    double distance = otherEnd.getKey().getLocation().distSqr(from);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestEdge = otherEnd.getValue();
                        closestFromNode = otherEnd.getKey();
                    }
                }
                if (closestEdge != null) {
                    ((ISwitchDisabledEdge) closestEdge.getEdgeData()).setEnabled(forceActive || getSwitchTarget() == to);
                    ((ISwitchDisabledEdge) closestEdge.getEdgeData()).setAutomatic(!forceActive && automatic && !locked && autoTrainsSwitch);
                }
                if (closestFromNode != null) {
                    TrackEdge reverseEdge = graph.getConnection(Couple.create(closestFromNode, toNode));
                    if (reverseEdge != null) {
                        ((ISwitchDisabledEdge) reverseEdge.getEdgeData()).setEnabled(forceActive || getSwitchTarget() == to);
                        ((ISwitchDisabledEdge) reverseEdge.getEdgeData()).setAutomatic(!forceActive && automatic && !locked && autoTrainsSwitch);
                    }
                }
                // not sure if this enabled state will be synchronized to client... hmm...
            }
        }
    }

    private void switchForEdges(TrackGraph graph) {
        TrackNodeLocation from = switchPoint;
        TrackNodeLocation highestPriorityExit = null;
        int highestPriority = -100;
        for (TrackNodeLocation to : exits) {
            if (to != null) {
                TrackNode toNode = graph.locateNode(to);
                Map<TrackNode, TrackEdge> connections = graph.getConnectionsFrom(toNode);
                if (connections == null)
                    continue;
                TrackNode closestFromNode = null;
                TrackEdge closestEdge = null;
                double closestDistance = Double.MAX_VALUE;
                for (Map.Entry<TrackNode, TrackEdge> otherEnd : connections.entrySet()) {
                    double distance = otherEnd.getKey().getLocation().distSqr(from);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestEdge = otherEnd.getValue();
                        closestFromNode = otherEnd.getKey();
                    }
                }
                if (closestEdge != null) {
                    ISwitchDisabledEdge switchEdge = (ISwitchDisabledEdge) closestEdge.getEdgeData();
                    if (switchEdge.isAutomaticallySelected()) {
                        if (switchEdge.getAutomaticallySelectedPriority() > highestPriority) {
                            highestPriorityExit = to;
                            highestPriority = switchEdge.getAutomaticallySelectedPriority();
                        }
                        switchEdge.ackAutomaticSelection();
                    }
                }
                if (closestFromNode != null) {
                    TrackEdge reverseEdge = graph.getConnection(Couple.create(closestFromNode, toNode));
                    if (reverseEdge != null) {
                        ISwitchDisabledEdge reverseSwitchEdge = (ISwitchDisabledEdge) reverseEdge.getEdgeData();
                        if (reverseSwitchEdge.isAutomaticallySelected()) {
                            if (reverseSwitchEdge.getAutomaticallySelectedPriority() > highestPriority) {
                                highestPriorityExit = to;
                                highestPriority = reverseSwitchEdge.getAutomaticallySelectedPriority();
                            }
                            reverseSwitchEdge.ackAutomaticSelection();
                        }
                    }
                }
                // not sure if this enabled state will be synchronized to client... hmm...
            }
        }
        if (highestPriorityExit != null) {
            for (SwitchState state : SwitchState.values()) {
                if (highestPriorityExit == getExit(state)) {
                    setSwitchState(state);
                    break;
                }
            }
        }
    }
}
