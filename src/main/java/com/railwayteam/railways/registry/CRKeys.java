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

package com.railwayteam.railways.registry;

import com.mojang.blaze3d.platform.InputConstants;
import com.railwayteam.railways.Railways;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.Window;
import com.zurrtum.create.client.AllKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public enum CRKeys {
    BOGEY_MENU("bogey_menu", GLFW.GLFW_KEY_LEFT_ALT),
    CYCLE_MENU("cycle_menu", GLFW.GLFW_KEY_LEFT_ALT);

    private KeyMapping keybind;
    private final String description;
    private final int key;
    private final boolean modifiable;

    // PORT-NOTE(26.1): key mappings take a registered category (lang key key.category.railways.keys)
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Railways.asResource("keys"));

    // Needed to make keys not appear to conflict between Steam 'n' Rails & Create
    public static final Set<KeyMapping> NON_CONFLICTING_KEYMAPPINGS = new HashSet<>();

    CRKeys(String description, int defaultKey) {
        this.description = Railways.MOD_ID + ".keyinfo." + description;
        this.key = defaultKey;
        this.modifiable = !description.isEmpty();
    }

    public static void register() {
        // PORT-NOTE(fabric): was AllKeysMixin; Create Fly lists its key mappings in AllKeys.ALL
        NON_CONFLICTING_KEYMAPPINGS.addAll(AllKeys.ALL);
        for (CRKeys key : values()) {
            if (!key.modifiable)
                continue;
            key.keybind = new KeyMapping(key.description, key.key, CATEGORY);
            NON_CONFLICTING_KEYMAPPINGS.add(key.keybind);
            registerKeyBinding(key.keybind);
        }
    }

    public static void fixBinds() {
        Window window = Minecraft.getInstance().getWindow();
        for (CRKeys key : values()) {
            if (key.keybind == null || key.keybind.isUnbound())
                continue;
            key.keybind.setDown(InputConstants.isKeyDown(window, key.getBoundCode()));
        }
    }

    public KeyMapping getKeybind() {
        return keybind;
    }

    public boolean isPressed() {
        if (!modifiable)
            return isKeyDown(key);
        return keybind.isDown();
    }

    public String getBoundKey() {
        return keybind.getTranslatedKeyMessage()
                .getString()
                .toUpperCase();
    }

    public int getBoundCode() {
        return getBoundCode(keybind);
    }

    public static boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }

    public static boolean isMouseButtonDown(int button) {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), button) == 1;
    }

    public static boolean ctrlDown() {
        return Minecraft.getInstance().hasControlDown();
    }

    public static boolean shiftDown() {
        return Minecraft.getInstance().hasShiftDown();
    }

    public static boolean altDown() {
        return Minecraft.getInstance().hasAltDown();
    }

    private static void registerKeyBinding(KeyMapping keyMapping) {
        com.railwayteam.railways.registry.fabric.CRKeysImpl.registerKeyBinding(keyMapping);
    }

    private static int getBoundCode(KeyMapping keyMapping) {
        return com.railwayteam.railways.registry.fabric.CRKeysImpl.getBoundCode(keyMapping);
    }
}
