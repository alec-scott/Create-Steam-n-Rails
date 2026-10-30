/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2026 The Railways Team
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

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.client.gui.components.Tooltip;
import com.railwayteam.railways.util.packet.StationLimitPacket;
import com.google.common.collect.ImmutableList;
import com.railwayteam.railways.mixin_interfaces.ILimited;
import com.railwayteam.railways.registry.CRPackets;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.entity.TrainIconType;
import com.zurrtum.create.client.content.trains.station.AbstractStationScreen;
import com.zurrtum.create.content.trains.station.GlobalStation;
import com.zurrtum.create.content.trains.station.StationBlockEntity;
import com.zurrtum.create.client.content.trains.station.StationScreen;
import com.zurrtum.create.infrastructure.packet.c2s.TrainEditPacket;
import com.zurrtum.create.client.foundation.gui.widget.ScrollInput;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = StationScreen.class, remap = false)
public abstract class MixinStationScreen extends AbstractStationScreen {
    @Shadow private EditBox trainNameBox;
    private Checkbox limitEnableCheckbox;
    private List<Identifier> iconTypes;
    private ScrollInput iconTypeScroll;

    private MixinStationScreen(StationBlockEntity te, GlobalStation station) {
        super(te, station);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/content/trains/station/StationScreen;tickTrainDisplay()V"), remap = true)
    private void initCheckbox(CallbackInfo ci) {
        int x = guiLeft;
        int y = guiTop;
        // PORT-NOTE(26.1): checkboxes are built (the hover tooltip is now a widget tooltip)
        limitEnableCheckbox = Checkbox.builder(Component.translatable("railways.station.train_limit"), font)
            .pos(x + background.getWidth() - 98, y + background.getHeight() - 26)
            .selected(station != null && ((ILimited) station).isLimitEnabled())
            .onValueChange((checkbox, selected) -> CRPackets.PACKETS.send(new StationLimitPacket(blockEntity.getBlockPos(), selected)))
            .tooltip(Tooltip.create(Component.translatable("railways.station.train_limit.tooltip.1")
                .append(CommonComponents.NEW_LINE).append(Component.translatable("railways.station.train_limit.tooltip.2"))))
            .build();
        addRenderableWidget(limitEnableCheckbox);

        iconTypes = TrainIconType.ALL.keySet()
                .stream()
                .toList();
        iconTypeScroll = new ScrollInput(x + 4, y + 17, 184, 14).titled(CreateLang.translateDirect("station.icon_type"));
        iconTypeScroll.withRange(0, iconTypes.size());
        iconTypeScroll.withStepFunction(ctx -> -iconTypeScroll.standardStep()
                .apply(ctx));
        iconTypeScroll.calling(s -> {
            Train train = displayedTrain.get();
            if (train != null) {
                train.icon = TrainIconType.byId(iconTypes.get(s));
                minecraft.player.connection.send(new TrainEditPacket(train.id, trainNameBox.getValue(), train.icon.id(), train.mapColorIndex));
            }
        });
        iconTypeScroll.active = false;
    }

    @Inject(method = "tickTrainDisplay", at = @At("HEAD"))
    private void tickIconScroll(CallbackInfo ci) {
        Train train = displayedTrain.get();

        if (train == null) {
            if (iconTypeScroll.active) {
                iconTypeScroll.active = false;
                removeWidget(iconTypeScroll);
            }

            Train imminentTrain = getImminent();

            if (imminentTrain != null) {
                iconTypeScroll.active = true;
                iconTypeScroll.setState(iconTypes.indexOf(imminentTrain.icon.id()));
                addRenderableWidget(iconTypeScroll);
            }
        }
    }
}
