package com.railwayteam.railways.port.create.foundation.data;

import com.railwayteam.railways.port.registrate.Registrate;
import com.railwayteam.railways.port.registrate.builders.Builder;
import com.railwayteam.railways.port.registrate.builders.NoConfigBuilder;
import com.railwayteam.railways.port.porting_lib.util.DeferredHolder;
import com.railwayteam.railways.port.registrate.util.entry.RegistryEntry;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullFunction;
import com.railwayteam.railways.port.registrate.util.nullness.NonNullSupplier;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.behaviour.display.DisplayTarget;
import com.zurrtum.create.api.registry.CreateRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public class CreateRegistrate extends Registrate {
    private static final Map<RegistryEntry<?, ?>, ResourceKey<CreativeModeTab>> TAB_LOOKUP = new IdentityHashMap<>();

    @Nullable
    private ResourceKey<CreativeModeTab> currentTab;

    public CreateRegistrate(String modId) {
        super(modId);
    }

    public static CreateRegistrate create(String modId) {
        return new CreateRegistrate(modId);
    }

    /** Upstream Create's tab tracking: entries registered after this call belong to {@code tab}. */
    public CreateRegistrate setCreativeTab(@Nullable ResourceKey<CreativeModeTab> tab) {
        this.currentTab = tab;
        return this;
    }

    public static boolean isInCreativeTab(RegistryEntry<?, ?> entry, ResourceKey<CreativeModeTab> tab) {
        return TAB_LOOKUP.get(entry) == tab;
    }

    @Override
    protected <R, T extends R> RegistryEntry<R, T> accept(String name, ResourceKey<? extends Registry<R>> type,
                                                         Builder<R, T, ?, ?> builder, NonNullSupplier<? extends T> creator,
                                                         NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
        RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);
        if (currentTab != null)
            TAB_LOOKUP.put(entry, currentTab);
        return entry;
    }

    public <T extends DisplaySource> NoConfigBuilder<DisplaySource, T, CreateRegistrate> displaySource(
            String name, NonNullSupplier<T> supplier) {
        return generic(self(), name, (net.minecraft.resources.ResourceKey) CreateRegistries.DISPLAY_SOURCE.key(), supplier);
    }

    public <T extends DisplayTarget> NoConfigBuilder<DisplayTarget, T, CreateRegistrate> displayTarget(
            String name, NonNullSupplier<T> supplier) {
        return generic(self(), name, (net.minecraft.resources.ResourceKey) CreateRegistries.DISPLAY_TARGET.key(), supplier);
    }

}
