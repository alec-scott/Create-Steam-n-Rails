package com.railwayteam.railways.content.schedule.client;

import com.railwayteam.railways.content.schedule.StationLoadedCondition;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.content.trains.schedule.IScheduleInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** PORT-NOTE(26.1): the client half of {@link StationLoadedCondition}. */
public class StationLoadedConditionRender implements IScheduleInput<StationLoadedCondition> {
    @Override
    public Pair<ItemStack, Component> getSummary(StationLoadedCondition input) {
        return Pair.of(ItemStack.EMPTY, Component.translatable("railways.schedule.condition.loaded"));
    }
}
