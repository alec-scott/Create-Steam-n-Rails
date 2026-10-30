/*
 * Steam 'n' Rails
 * Copyright (c) 2025 The Railways Team
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

import com.railwayteam.railways.Railways;
import com.zurrtum.create.client.foundation.block.connected.CTType;

/**
 * PORT-NOTE(26.1): Create Fly's {@link CTType} is a class whose texture index comes from a bit mask of connected
 * steps (was an enum implementing Create's {@code CTType} interface, with a {@code CTContext}).
 */
public class CRCTTypes extends CTType {
    public static final CRCTTypes VERTICAL_PINKMACHINE = new CRCTTypes("vertical_pinkmachine");

    private CRCTTypes(String name) {
        super(Railways.asResource(name), VERTICAL);
    }

    @Override
    public int getTextureIndex(int context) {
        boolean up = (context & UP_FLAG) != 0;
        boolean down = (context & DOWN_FLAG) != 0;
        return !up && !down
            ? 0 // single
            : !up
            ? 3 // top
            : !down
            ? 2 // bottom
            : 1; // middle
    }
}
