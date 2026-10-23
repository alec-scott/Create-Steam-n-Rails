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
package com.railwayteam.railways.content.switches.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.switches.TrackSwitch;
import com.railwayteam.railways.content.switches.TrackSwitchBlockEntity;
import com.railwayteam.railways.content.switches.TrackSwitchBlockEntity.AutoMode;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.railwayteam.railways.registry.CRIcons;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.catnip.lang.Lang;
import com.zurrtum.create.client.catnip.lang.LangBuilder;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

/**
 * Client behaviours of the track switch: the automatic switch's mode box and the goggle tooltip.
 *
 * <p>PORT-NOTE(fabric): upstream kept these on the block entity; Create Fly's value boxes and goggle API are client
 * only (see {@code CRBlockEntityBehaviours}).</p>
 */
public final class TrackSwitchClientBehaviours {
    private TrackSwitchClientBehaviours() {}

    public enum AutoModeIcon implements INamedIconOptions {
        MANUAL_ONLY(CRIcons.I_SWITCH_MANUAL),
        AUTO(CRIcons.I_SWITCH_AUTO);

        private final String translationKey;
        private final AllIcons icon;

        AutoModeIcon(AllIcons icon) {
            this.icon = icon;
            this.translationKey = "railways.switch.auto_mode." + name().toLowerCase(Locale.ROOT);
        }

        static AutoModeIcon from(AutoMode mode) {
            return values()[mode.ordinal()];
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    /** Only active on automatic switches (the block entity only has the server option then). */
    public static ScrollOptionBehaviour<AutoMode> autoMode(TrackSwitchBlockEntity be) {
        ScrollOptionBehaviour<AutoMode> box = new ScrollOptionBehaviour<>(AutoModeIcon.class, AutoModeIcon::from,
            Component.translatable("railways.switch.auto_mode"), be, new AutoModeSlot()) {
            {
                needsWrench = true;
            }
        };
        box.onlyActiveWhen(be::isAutomatic);
        return box;
    }

    private static class AutoModeSlot extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(BlockState blockState) {
            Vec3 base = new Vec3(12 / 16.0, 4.5 / 16.0, 4 / 16.0);
            return VecHelper.rotateCentered(base, AngleHelper.horizontalAngle(blockState.getValue(FACING)), Direction.Axis.Y);
        }

        @Override
        public void rotate(BlockState blockState, PoseStack ms) {
            TransformStack.of(ms)
                .rotateY(AngleHelper.horizontalAngle(blockState.getValue(FACING)) - 90)
                .rotateX(90);
        }

        @Override
        public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
            Vec3 offset = getLocalOffset(state);
            if (offset == null)
                return false;
            return localHit.distanceTo(offset) < scale / 3;
        }
    }

    public static class Tooltip extends TooltipBehaviour<TrackSwitchBlockEntity> implements IHaveGoggleInformation {
        public Tooltip(TrackSwitchBlockEntity be) {
            super(be);
        }

        private static LangBuilder b() {
            return Lang.builder(Railways.MOD_ID);
        }

        @Override
        public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
            b().translate("tooltip.switch.header").forGoggles(tooltip);
            b().translate("tooltip.switch.state")
                .style(ChatFormatting.YELLOW)
                .forGoggles(tooltip);
            b().translate("switch.state." + blockEntity.getState().getSerializedName())
                .style(ChatFormatting.YELLOW)
                .forGoggles(tooltip);
            return true;
        }
    }

    /** The lever overlay for the switch's current exits and state (was {@code TrackSwitchBlockEntity.getOverlayModel}). */
    public static @Nullable PartialModel overlayModel(TrackSwitchBlockEntity be) {
        TrackSwitch sw = be.edgePoint.getEdgePoint();
        if (sw == null) {
            return null;
        }

        if (sw.hasStraightExit() && sw.hasLeftExit() && sw.hasRightExit()) {
            if (be.isNormal()) {
                return CRBlockPartials.SWITCH_3WAY_STRAIGHT;
            } else if (be.isReverseLeft()) {
                return CRBlockPartials.SWITCH_3WAY_LEFT;
            } else if (be.isReverseRight()) {
                return CRBlockPartials.SWITCH_3WAY_RIGHT;
            }
        } else if (sw.hasStraightExit() && sw.hasLeftExit()) {
            return be.isNormal() ? CRBlockPartials.SWITCH_LEFT_STRAIGHT
                : CRBlockPartials.SWITCH_LEFT_TURN;
        } else if (sw.hasStraightExit() && sw.hasRightExit()) {
            return be.isNormal() ? CRBlockPartials.SWITCH_RIGHT_STRAIGHT
                : CRBlockPartials.SWITCH_RIGHT_TURN;
        } else if (sw.hasLeftExit() && sw.hasRightExit()) {
            return be.isReverseLeft() ? CRBlockPartials.SWITCH_2WAY_LEFT
                : CRBlockPartials.SWITCH_2WAY_RIGHT;
        }

        return CRBlockPartials.SWITCH_NONE;
    }
}
