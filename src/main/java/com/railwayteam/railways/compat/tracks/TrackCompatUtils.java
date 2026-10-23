/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2025 The Railways Team
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

package com.railwayteam.railways.compat.tracks;

import com.railwayteam.railways.port.create.track.TrackMaterialData;
import com.railwayteam.railways.port.create.track.TrackMaterialFactory;

import com.zurrtum.create.AllBlockTags;

import com.google.common.collect.ImmutableSet;
import com.railwayteam.railways.content.custom_tracks.CustomTrackBlockStateGenerator;
import com.railwayteam.railways.content.custom_tracks.gen_template.OutputPrefixer;
import com.railwayteam.railways.content.custom_tracks.gen_template.TextureMaps;
import com.railwayteam.railways.content.custom_tracks.gen_template.TrackGenTemplate;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.compat.Mods;
import com.railwayteam.railways.config.CRConfigs;
import com.railwayteam.railways.mixin.AccessorTrackMaterialFactory;
import com.railwayteam.railways.multiloader.CommonTags;
import com.railwayteam.railways.registry.CRTrackMaterials;
import com.zurrtum.create.content.trains.track.*;
import com.railwayteam.railways.port.create.foundation.data.CreateRegistrate;
import com.railwayteam.railways.port.create.foundation.data.SharedProperties;
import com.railwayteam.railways.port.registrate.providers.DataGenContext;
import com.railwayteam.railways.port.registrate.providers.RegistrateBlockstateProvider;
import com.railwayteam.railways.port.registrate.util.entry.BlockEntry;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullBiConsumer;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;


public abstract class TrackCompatUtils {

    public static final Set<String> TRACK_COMPAT_MODS = ImmutableSet.of(
            "hexcasting",
            "byg", // Oh The Biomes You'll Go,
            "blue_skies",
            "twilightforest",
            "biomesoplenty",
            "natures_spirit",
            "create_dd", // Dreams 'n' Desires
            "quark",
            "tfc" // TerraFirmaCraft
    );

    public static boolean anyLoaded() {
        if (GenericTrackCompat.isDataGen() || CRConfigs.common().registerMissingTracks.get())
            return true;
        for (String mod : TRACK_COMPAT_MODS) {
            if (Mods.valueOf(mod.toUpperCase(Locale.ROOT)).isLoaded)
                return true;
        }
        return false;
    }

    @ApiStatus.Internal
    public static boolean mixinSkipLootLoading(Identifier resourceLocation) {
        if (resourceLocation.getNamespace().equals(Railways.MOD_ID)) {
            for (String compatMod : TRACK_COMPAT_MODS) {
                if (resourceLocation.getPath().startsWith("blocks/track_"+compatMod)) {
                    return !GenericTrackCompat.get(compatMod).shouldRegisterMissing();
                }
            }
        }
        return false;
    }

    private static final CreateRegistrate REGISTRATE = Railways.registrate();

    public static BlockEntry<TrackBlock> makeTrack(TrackMaterial material) {
        return makeTrack(material, CustomTrackBlockStateGenerator.create(
            OutputPrefixer.COMPAT,
            TrackGenTemplate.DEFAULT,
            TextureMaps.STANDARD.map
        )::generate);
    }

    public static BlockEntry<TrackBlock> makeTrack(TrackMaterial material, boolean hideInCreativeTabs) {
        return makeTrack(material, CustomTrackBlockStateGenerator.create(
            OutputPrefixer.COMPAT,
            TrackGenTemplate.DEFAULT,
            TextureMaps.STANDARD.map
        )::generate, (t) -> {}, (p) -> p, hideInCreativeTabs);
    }

    public static BlockEntry<TrackBlock> makeTrack(TrackMaterial material, NonNullBiConsumer<DataGenContext<Block, TrackBlock>, RegistrateBlockstateProvider> blockstateGen) {
        return makeTrack(material, blockstateGen, (t) -> {});
    }

    public static BlockEntry<TrackBlock> makeTrack(TrackMaterial material, NonNullBiConsumer<DataGenContext<Block, TrackBlock>, RegistrateBlockstateProvider> blockstateGen, NonNullConsumer<? super TrackBlock> onRegister) {
        return makeTrack(material, blockstateGen, onRegister, (p) -> p);
    }

    public static BlockEntry<TrackBlock> makeTrack(TrackMaterial material, NonNullBiConsumer<DataGenContext<Block, TrackBlock>, RegistrateBlockstateProvider> blockstateGen, Function<BlockBehaviour.Properties, BlockBehaviour.Properties> collectProperties) {
        return makeTrack(material, blockstateGen, (t) -> {}, collectProperties);
    }

    public static BlockEntry<TrackBlock> makeTrack(TrackMaterial material, NonNullBiConsumer<DataGenContext<Block, TrackBlock>, RegistrateBlockstateProvider> blockstateGen, NonNullConsumer<? super TrackBlock> onRegister, Function<BlockBehaviour.Properties, BlockBehaviour.Properties> collectProperties) {
        return makeTrack(material, blockstateGen, onRegister, collectProperties, false);
    }

    public static BlockEntry<TrackBlock> makeTrack(TrackMaterial material, NonNullBiConsumer<DataGenContext<Block, TrackBlock>, RegistrateBlockstateProvider> blockstateGen, NonNullConsumer<? super TrackBlock> onRegister, Function<BlockBehaviour.Properties, BlockBehaviour.Properties> collectProperties, boolean hideInCreativeTabs) {
        String owningMod = material.getId().getNamespace();
        String name = "track_" + owningMod + "_" + TrackMaterialData.of(material).resourceName();

        // PORT-NOTE(fabric): the optional tracks / girdable / pickaxe tags were datagen only (CRTagGen); the generated tag JSON has them.

        return REGISTRATE.block(name, p -> TrackMaterialData.of(material).createBlock(p))
            .initialProperties(SharedProperties::stone)
            .properties(p -> collectProperties.apply(p)
                .mapColor(MapColor.METAL)
                .strength(0.8F)
                .sound(SoundType.METAL)
                .noOcclusion())
            .blockstate(blockstateGen)
            .lang(TrackMaterialData.of(material).langName + " Train Track")
            .onRegister(onRegister)
            .onRegister(CRTrackMaterials::addToBlockEntityType)
            .item(TrackBlockItem::new)
            .removeTab(hideInCreativeTabs ? null : CreativeModeTabs.SEARCH)
            .model((c, p) -> p.generated(c, Identifier.fromNamespaceAndPath(owningMod, "item/track/track_"+TrackMaterialData.of(material).resourceName())))
            .build()
            .register();
    }

    public static TrackMaterial buildCompatModels(GenericTrackCompat trackCompat, TrackMaterialFactory factory) {
        String namespace = ((AccessorTrackMaterialFactory)factory).getId().getNamespace();
        String path = ((AccessorTrackMaterialFactory)factory).getId().getPath();
        String prefix = "block/track/compat/" + namespace + "/" + path + "/";

        TrackMaterialFactory materialFactory = factory.customModels(
                () -> () -> PartialModel.of(Railways.asResource(prefix + "tie")),
                () -> () -> PartialModel.of(Railways.asResource(prefix + "segment_left")),
                () -> () -> PartialModel.of(Railways.asResource(prefix + "segment_right"))
        );

        String customLang = trackCompat.getLang(path);
        if (!path.equals(customLang))
            materialFactory.lang(customLang);

        return materialFactory.build();
    }
}
