package com.railwayteam.railways.port.create.foundation.data;

import com.railwayteam.railways.port.registrate.providers.DataGenContext;
import com.railwayteam.railways.port.registrate.providers.RegistrateBlockstateProvider;
import com.railwayteam.railways.port.registrate.providers.RegistrateItemModelProvider;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullBiConsumer;
import com.railwayteam.railways.port.porting_lib.models.generators.ModelFile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Function;

public final class AssetLookup {
    private AssetLookup() {
    }

    public static <T extends Block> Function<BlockState, ModelFile> partialBaseModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
        return state -> prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block"));
    }

    public static <T extends Block> Function<BlockState, ModelFile> forPowered(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
        return state -> prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block" + (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED) ? "_powered" : "")));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> itemModelWithPartials() {
        return (ctx, prov) -> { };
    }
}
