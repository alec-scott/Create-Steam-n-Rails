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
package com.railwayteam.railways.content.animated_flywheel.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.content.animated_flywheel.FlywheelMovementBehaviour;
import com.railwayteam.railways.mixin_interfaces.ICarriageFlywheel;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderBehaviour;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderState;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.foundation.virtualWorld.VirtualRenderWorld;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.kinetics.flywheel.FlywheelBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Sets the carriage flywheel's interpolated angle each frame.
 *
 * <p>PORT-NOTE(fabric): was {@code FlywheelMovementBehaviour.renderInContraption}; Create Fly attaches client
 * movement rendering separately. Draws nothing itself (the flywheel block entity renders).</p>
 */
public class FlywheelMovementRender implements MovementRenderBehaviour {
    @Override
    public MovementRenderState getRenderState(Vec3 camera, Font font, MovementContext context, VirtualRenderWorld renderWorld,
                                              PoseStack.Pose transform, Matrix4f worldMatrix) {
        if (!(context.contraption.getOrCreateClientContraptionLazy().getBlockEntity(context.localPos) instanceof FlywheelBlockEntity flywheelBlockEntity))
            return null;
        if (!(context.temporaryData instanceof FlywheelMovementBehaviour.TemporaryData temporaryData))
            return null;

        ((ICarriageFlywheel) flywheelBlockEntity).railways$setAngle(Mth.lerp(
            AnimationTickHolder.getPartialTicks(),
            temporaryData.wheelAngle0,
            temporaryData.wheelAngle1
        ));
        return null;
    }
}
