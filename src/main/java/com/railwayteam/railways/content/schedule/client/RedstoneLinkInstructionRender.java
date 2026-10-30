package com.railwayteam.railways.content.schedule.client;

import com.google.common.collect.ImmutableList;
import com.railwayteam.railways.content.schedule.RedstoneLinkInstruction;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.content.trains.schedule.IScheduleInput;
import com.zurrtum.create.client.foundation.gui.ModularGuiLineBuilder;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** PORT-NOTE(26.1): the client half of {@link RedstoneLinkInstruction} (was on the instruction itself in 1.20). */
public class RedstoneLinkInstructionRender implements IScheduleInput<RedstoneLinkInstruction> {
    @Override
    public int slotsTargeted() {
        return 2;
    }

    @Override
    public Pair<ItemStack, Component> getSummary(RedstoneLinkInstruction input) {
        return Pair.of(icon(), formatted(input));
    }

    private MutableComponent formatted(RedstoneLinkInstruction input) {
        return Component.translatable("railways.schedule.instruction.redstone_link.power", input.intData("Power"));
    }

    @Override
    public List<Component> getSecondLineTooltip(int slot) {
        return ImmutableList.of(CreateLang.translateDirect(slot == 0 ? "logistics.firstFrequency" : "logistics.secondFrequency")
            .withStyle(ChatFormatting.RED));
    }

    private ItemStack icon() {
        return AllItems.REDSTONE_LINK.getDefaultInstance();
    }

    @Override
    public ItemStack getSecondLineIcon() {
        return icon();
    }

    @Override
    public List<Component> getTitleAs(RedstoneLinkInstruction input, String type) {
        return ImmutableList.of(
            CreateLang.translateDirect("schedule.condition.redstone_link.frequency_powered"),
            Component.literal(" #1 ").withStyle(ChatFormatting.GRAY)
                .append(input.freq.getFirst()
                    .getStack()
                    .getHoverName()
                    .copy()
                    .withStyle(ChatFormatting.DARK_AQUA)),
            Component.literal(" #2 ").withStyle(ChatFormatting.GRAY)
                .append(input.freq.getSecond()
                    .getStack()
                    .getHoverName()
                    .copy()
                    .withStyle(ChatFormatting.DARK_AQUA)));
    }

    @Override
    public void setItem(RedstoneLinkInstruction input, int slot, ItemStack stack) {
        input.freq.set(slot == 0, Frequency.of(stack));
    }

    @Override
    public ItemStack getItem(RedstoneLinkInstruction input, int slot) {
        return input.freq.get(slot == 0)
            .getStack();
    }

    @Override
    public void initConfigurationWidgets(RedstoneLinkInstruction input, ModularGuiLineBuilder builder) {
        builder.addScrollInput(20, 101, (si, l) -> {
            si.withRange(1, 16)
                .withStepFunction(c -> c.shift ? 5 : 1)
                .titled(Component.translatable("railways.schedule.instruction.redstone_link.power_edit_box"));
            //l.withSuffix("%");
        }, "Power");
    }
}
