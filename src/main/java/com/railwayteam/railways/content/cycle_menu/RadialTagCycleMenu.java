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

package com.railwayteam.railways.content.cycle_menu;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.railwayteam.railways.content.bogey_menu.handler.BogeyMenuEventsHandler;
import com.railwayteam.railways.mixin.client.AccessorToolboxHandlerClient;
import com.railwayteam.railways.registry.CRKeys;
import com.railwayteam.railways.util.client.ClientUtils;
import com.zurrtum.create.client.foundation.gui.AllGuiTextures;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.gui.AbstractSimiScreen;
import com.zurrtum.create.client.catnip.gui.element.GuiGameElement;
import com.zurrtum.create.catnip.math.AngleHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RadialTagCycleMenu extends AbstractSimiScreen {

    private int ticksOpen;
    private int hoveredSlot;
    private boolean scrollMode;
    private int scrollSlot = 0;
    private final TagKey<Item> tag;
    private final List<Item> cycle;
    private final @Nullable DataComponentPatch stackTag;
    private final int slotCount;

    RadialTagCycleMenu(TagKey<Item> tag, List<Item> cycle, @Nullable DataComponentPatch stackTag) {
        hoveredSlot = -1;
        this.cycle = cycle;
        this.tag = tag;
        this.stackTag = stackTag;
        this.slotCount = Math.max(8, cycle.size());
    }

    @SuppressWarnings({"IntegerDivisionInFloatingPointContext", "DuplicatedCode"})
    @Override
    protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        float fade = Mth.clamp((ticksOpen + AnimationTickHolder.getPartialTicks()) / 10f, 1 / 512f, 1);

        // PORT-NOTE(26.1): mirrors Create Fly's RadialToolboxMenu (2D GUI poses, render state GUI, input events)
        Matrix3x2fStack ms = graphics.pose();

        hoveredSlot = -1;
        Window window = Minecraft.getInstance().getWindow();
        float hoveredX = mouseX - window.getGuiScaledWidth() / 2;
        float hoveredY = mouseY - window.getGuiScaledHeight() / 2;

        double interSlotAngle = 360. / slotCount;

        float distance = hoveredX * hoveredX + hoveredY * hoveredY;
        if (distance > 25 && distance < 10000) {
            double offset = 90 - 1.5 * interSlotAngle;
            hoveredSlot = (int) Math.floor(
                (Mth.floor((AngleHelper.deg(Mth.atan2(hoveredY, hoveredX)) + 360 + 180 - offset)) % 360)
                    / interSlotAngle
            );
        }
        if (scrollMode && distance > 150)
            scrollMode = false;

        ms.pushMatrix();
        ms.translate(width / 2, height / 2);
        Component tip = null;
        Identifier tagLoc = tag.location();
        Component title = Component.translatable("tag.item." + tagLoc.getNamespace() + "." + tagLoc.getPath().replace('/', '.'));

        /*
        core rendering
         */

        for (int slot = 0; slot < slotCount; slot++) {
            ms.pushMatrix();
            double radius = -(5 * slotCount) + (10 * (1 - fade) * (1 - fade));
            double angle = slot * interSlotAngle - interSlotAngle;
            ms.rotate(Mth.DEG_TO_RAD * (float) angle);
            ms.translate(0, (float) radius);
            ms.rotate(Mth.DEG_TO_RAD * (float) -angle);
            ms.translate(-12, -12);

            boolean selected = (slot == (scrollMode ? scrollSlot : hoveredSlot));

            if (slot < cycle.size()) {
                ItemStack stack = new ItemStack(cycle.get(slot));
                if (stackTag != null) {
                    stack.applyComponents(stackTag);
                }

                if (minecraft != null) {

                    AllGuiTextures.TOOLBELT_SLOT.render(graphics, 0, 0);
                    graphics.item(stack, 3, 3);


                    if (selected) {
                        AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, -1, -1);
                        tip = Component.empty().append(stack.getHoverName())
                            .withStyle(ChatFormatting.GOLD);
                    }
                }
            } else {
                AllGuiTextures.TOOLBELT_EMPTY_SLOT.render(graphics, 0, 0);
            }

            ms.popMatrix();
        }

        /*
        end core rendering
         */

        {
            int i1 = (int) (fade * 255.0F);
            if (i1 > 255)
                i1 = 255;

            if (i1 > 8) {
                ms.pushMatrix();
                ms.translate(0, -80);
                drawComponent(graphics, title, i1);
            }
        }


        ms.popMatrix();

        if (tip != null) {
            int i1 = (int) (fade * 255.0F);
            if (i1 > 255)
                i1 = 255;

            if (i1 > 8) {
                ms.pushMatrix();
                ms.translate((float) (width / 2), (float) (height - 68));
                drawComponent(graphics, tip, i1);
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void drawComponent(GuiGraphicsExtractor graphics, Component title, int i1) {
        Matrix3x2fStack ms = graphics.pose();

        int k1 = 16777215;
        int k = i1 << 24 & -16777216;
        int l = font.width(title);
        graphics.text(font, title, Math.round(-l / 2f), -4, k1 | k, false);
        ms.popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int a = ((int) (0x50 * Math.min(1, (ticksOpen + AnimationTickHolder.getPartialTicks()) / 20f))) << 24;
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x101010 | a, 0x101010 | a);
    }

    @Override
    public void tick() {
        ticksOpen++;
        super.tick();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int selected = scrollMode ? scrollSlot : hoveredSlot;
        int button = click.button();

        if (button == 0) {
            if (selected >= 0 && selected < cycle.size()) {
                TagCycleHandlerClient.select(cycle.get(selected));
                onClose();
                BogeyMenuEventsHandler.COOLDOWN = 2;
                TagCycleHandlerClient.COOLDOWN = 2;
                AccessorToolboxHandlerClient.setCOOLDOWN(2);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        Window window = Minecraft.getInstance().getWindow();
        double hoveredX = mouseX - window.getGuiScaledWidth() / 2;
        double hoveredY = mouseY - window.getGuiScaledHeight() / 2;
        double distance = hoveredX * hoveredX + hoveredY * hoveredY;
        if (distance <= 150) {
            scrollMode = true;
            scrollSlot = (((int) (scrollSlot - delta)) + slotCount) % slotCount;
            for (int i = 0; i < 10; i++) {

                if (scrollSlot < cycle.size())
                    break;

                scrollSlot -= Mth.sign(delta);
                scrollSlot = (scrollSlot + slotCount) % slotCount;
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        InputConstants.Key mouseKey = InputConstants.getKey(input);
        if (ClientUtils.isActiveAndMatches(CRKeys.CYCLE_MENU.getKeybind(), mouseKey)) {
            onClose();
            return true;
        }
        return super.keyReleased(input);
    }
}
