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

package com.railwayteam.railways.content.custom_tracks.monorail.fabric;

import com.railwayteam.railways.annotation.multiloader.ImplClass;
import com.railwayteam.railways.content.custom_tracks.monorail.MonorailBlockStateGenerator;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackShape;
import com.railwayteam.railways.port.registrate.providers.DataGenContext;
import com.railwayteam.railways.port.registrate.providers.RegistrateBlockstateProvider;
import com.railwayteam.railways.port.porting_lib.models.generators.ModelFile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@ImplClass
public class MonorailBlockStateGeneratorImpl extends MonorailBlockStateGenerator {
    public static MonorailBlockStateGenerator create() {
        return new MonorailBlockStateGeneratorImpl();
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
        TrackShape value = state.getValue(TrackBlock.SHAPE);
        if (value == TrackShape.NONE)
            return prov.models()
                .getExistingFile(prov.mcLoc("block/air"));
        return prov.models()
            .getExistingFile(prov.modLoc("block/monorail/monorail/static_blocks/" + value.getModel()));
    }
}
