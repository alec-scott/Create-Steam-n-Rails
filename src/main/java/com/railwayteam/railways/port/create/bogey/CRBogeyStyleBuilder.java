package com.railwayteam.railways.port.create.bogey;

import com.zurrtum.create.client.AllBogeyStyleRenders;
import com.zurrtum.create.client.content.trains.bogey.SizeRenderer;
import com.zurrtum.create.content.trains.bogey.AbstractBogeyBlock;
import com.zurrtum.create.content.trains.bogey.BogeySize;
import com.zurrtum.create.content.trains.bogey.BogeyStyle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Upstream Create's bogey style builder: sizes take a block <em>supplier</em> and a client renderer.
 *
 * <p>PORT-NOTE(fabric): Create Fly's {@link BogeyStyle.Builder#size(BogeySize, AbstractBogeyBlock)} wants the block
 * instance, but Railways' bogey blocks read their default style from {@code CRBogeyStyles} in their constructors, so
 * the blocks don't exist yet when the styles are built. {@link BogeyStyle} keeps the builder's {@code sizes} map, so
 * blocks are put into it by {@link #resolveBlocks()} once Registrate has registered them. Renderers go to Create Fly's
 * client-side {@link AllBogeyStyleRenders} in {@link #registerRenderers()}.</p>
 */
public class CRBogeyStyleBuilder extends BogeyStyle.Builder {
    private static final List<CRBogeyStyleBuilder> BUILT = new ArrayList<>();

    private final Map<BogeySize, Supplier<? extends AbstractBogeyBlock<?>>> blocks = new LinkedHashMap<>();
    private final List<Supplier<Supplier<SizeRenderer>>> renderers = new ArrayList<>();
    private BogeyStyle style;

    public CRBogeyStyleBuilder(Identifier id, Identifier cycleGroup) {
        super(id, cycleGroup);
    }

    public CRBogeyStyleBuilder size(BogeySize size, Supplier<? extends AbstractBogeyBlock<?>> block,
                                    Supplier<Supplier<SizeRenderer>> renderer) {
        blocks.put(size, block);
        renderers.add(renderer);
        return this;
    }

    @Override
    public CRBogeyStyleBuilder displayName(Component displayName) {
        super.displayName(displayName);
        return this;
    }

    @Override
    public CRBogeyStyleBuilder soundEvent(Supplier<SoundEvent> soundEvent) {
        super.soundEvent(soundEvent);
        return this;
    }

    @Override
    public CRBogeyStyleBuilder contactParticle(ParticleOptions contactParticle) {
        super.contactParticle(contactParticle);
        return this;
    }

    @Override
    public CRBogeyStyleBuilder smokeParticle(ParticleOptions smokeParticle) {
        super.smokeParticle(smokeParticle);
        return this;
    }

    @Override
    public CRBogeyStyleBuilder defaultData(CompoundTag defaultData) {
        super.defaultData(defaultData);
        return this;
    }

    @Override
    public BogeyStyle build() {
        style = super.build();
        BUILT.add(this);
        return style;
    }

    /** Call once all blocks are registered. */
    public static void resolveBlocks() {
        for (CRBogeyStyleBuilder builder : BUILT) {
            builder.blocks.forEach((size, block) -> builder.sizes.put(size, block.get()));
        }
    }

    /** Client only, after {@link #resolveBlocks()}. */
    @SuppressWarnings("unchecked")
    public static void registerRenderers() {
        for (CRBogeyStyleBuilder builder : BUILT) {
            Supplier<SizeRenderer>[] sizeRenderers = builder.renderers.stream()
                .map(Supplier::get)
                .toArray(Supplier[]::new);
            AllBogeyStyleRenders.register(builder.style, sizeRenderers);
        }
    }
}
