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
package com.railwayteam.railways.util;

import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.world.entity.player.Player;

/**
 * A scroll value whose settings board starts at {@code min} instead of 0.
 *
 * <p>PORT-NOTE(fabric): Create Fly splits scroll values into this server half (stored on the block entity) and a client
 * half for the value box; see {@code util/client/MinRespectingScrollValueBox}.</p>
 */
public class MinRespectingScrollValueBehaviour extends ServerScrollValueBehaviour {
    public MinRespectingScrollValueBehaviour(SmartBlockEntity be) {
        super(be);
    }

    public int getMin() {
        return min;
    }

    @Override
    public void setValueSettings(Player player, ValueSettings vs, boolean ctrlDown) {
        super.setValueSettings(player, new ValueSettings(vs.row(), vs.value() + this.min), ctrlDown);
    }

    @Override
    public ValueSettings getValueSettings() {
        ValueSettings vs = super.getValueSettings();
        return new ValueSettings(vs.row(), vs.value() - min);
    }
}
