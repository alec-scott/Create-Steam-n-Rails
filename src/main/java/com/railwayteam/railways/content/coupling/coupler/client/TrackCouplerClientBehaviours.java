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
package com.railwayteam.railways.content.coupling.coupler.client;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.config.CRConfigs;
import com.railwayteam.railways.content.coupling.coupler.TrackCouplerBlockEntity;
import com.railwayteam.railways.content.coupling.coupler.TrackCouplerBlockEntity.ClientInfo;
import com.railwayteam.railways.content.coupling.coupler.TrackCouplerBlockEntity.OperationMode;
import com.railwayteam.railways.util.client.MinRespectingScrollValueBox;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.catnip.lang.Lang;
import com.zurrtum.create.client.catnip.lang.LangBuilder;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Client behaviours of the track coupler: the edge spacing value box and the goggle tooltip.
 *
 * <p>PORT-NOTE(fabric): upstream kept these on the block entity. Create Fly's value boxes and goggle API are client
 * only, so they're client behaviours added to each coupler via {@code BlockEntityBehaviour.CLIENT_REGISTRY}.</p>
 */
public final class TrackCouplerClientBehaviours {
    private TrackCouplerClientBehaviours() {}

    public static MinRespectingScrollValueBox<TrackCouplerBlockEntity> edgeSpacing(TrackCouplerBlockEntity be) {
        MinRespectingScrollValueBox<TrackCouplerBlockEntity> box = new MinRespectingScrollValueBox<>(
            Component.translatable("railways.coupler.edge_spacing"), be, new ValueBoxTransform(true)) {
            {
                needsWrench = true;
            }
        };
        box.withFormatter(i -> i + "m");
        return box;
    }

    private static class ValueBoxTransform extends CenteredSideValueBoxTransform {
        ValueBoxTransform(boolean vertical) {
            super((state, d) -> d.getAxis().isVertical() == vertical);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16);
        }
    }

    public static class Tooltip extends TooltipBehaviour<TrackCouplerBlockEntity> implements IHaveGoggleInformation {
        public Tooltip(TrackCouplerBlockEntity be) {
            super(be);
        }

        private static LangBuilder b() {
            return Lang.builder(Railways.MOD_ID);
        }

        @Override
        public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
            ClientInfo clientInfo = blockEntity.getClientInfoOrNull();
            b().translate("tooltip.coupler.header").forGoggles(tooltip);
            b().translate("tooltip.coupler.mode")
                .style(ChatFormatting.YELLOW)
                .forGoggles(tooltip);
            b().translate("coupler.mode." + blockEntity.getAllowedOperationMode().getSerializedName())
                .style(ChatFormatting.YELLOW)
                .forGoggles(tooltip);

            String train1 = clientInfo == null ? "None" : clientInfo.trainName1;
            String train2 = clientInfo == null ? "None" : clientInfo.trainName2;
            OperationMode operationMode = clientInfo == null ? OperationMode.NONE : clientInfo.mode;
            b().translate("tooltip.coupler.train1", train1)
                .style(ChatFormatting.GOLD)
                .forGoggles(tooltip);
            b().translate("tooltip.coupler.train2", train2)
                .style(ChatFormatting.GOLD)
                .forGoggles(tooltip);

            b().translate("tooltip.coupler.action." + operationMode.name().toLowerCase(Locale.ROOT))
                .style(ChatFormatting.GREEN)
                .forGoggles(tooltip);
            if (clientInfo != null) {
                if (clientInfo.error != null) {
                    b().add(clientInfo.error)
                        .style(ChatFormatting.DARK_RED)
                        .forGoggles(tooltip);
                }
                if (clientInfo.error2 != null && CRConfigs.client().showExtendedCouplerDebug.get()) {
                    b().add(clientInfo.error2)
                        .style(ChatFormatting.DARK_PURPLE)
                        .forGoggles(tooltip);
                }
            }
            return true;
        }
    }
}
