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

package com.railwayteam.railways.mixin;

import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.registry.CRTrackMaterials;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TrackMaterial.class, remap = false)
public class MixinTrackMaterial {
    /**
     * DataFixer for Modded Compat Cherry Tracks.
     * If it finds a modded cherry track material it'll replace it {@link CRTrackMaterials#CHERRY}
     * Which is from 1.20 Vanilla.
     */
    // PORT-NOTE(26.1): Create Fly decodes materials through fromId (its codecs xmap it), so both remaps run there.
    @ModifyVariable(method = "fromId", at = @At("HEAD"), argsOnly = true)
    private static Identifier railways$remapLegacyIds(Identifier id) {
        return railways$deserializeLegacyTracks(railways$updateCherryCompatTracks(id));
    }

    @Unique
    private static Identifier railways$updateCherryCompatTracks(Identifier original) {
        return switch (original.toString()) {
            case "biomesoplenty:cherry", "byg:cherry", "blue_skies:cherry"
                    -> CRTrackMaterials.CHERRY.getId();
            case "biomesoplenty:cherry_wide", "byg:cherry_wide", "blue_skies:cherry_wide"
                    -> CRTrackMaterials.getWide(CRTrackMaterials.CHERRY).getId();
            case "biomesoplenty:cherry_narrow", "byg:cherry_narrow", "blue_skies:cherry_narrow"
                    -> CRTrackMaterials.getNarrow(CRTrackMaterials.CHERRY).getId();
            default -> original;
        };
    }
    
    // Properly deserialize pre-resourcelocation tracks
    @Unique
    private static Identifier railways$deserializeLegacyTracks(Identifier original) {
        if (original.getNamespace().equals("minecraft")) {
            Identifier alternate = Railways.asResource(original.getPath());
            if (TrackMaterial.ALL.containsKey(alternate))
                return alternate;
        }
        return original;
    }
}
