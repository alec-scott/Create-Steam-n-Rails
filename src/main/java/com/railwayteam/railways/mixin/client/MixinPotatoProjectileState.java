package com.railwayteam.railways.mixin.client;

import com.railwayteam.railways.mixin_interfaces.IExtraRenderParts;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.equipment.potatoCannon.PotatoProjectileRenderer.PotatoProjectileState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

/** PORT-NOTE(26.1): holds the paint blob {@link MixinPotatoProjectileRenderer} extracts. */
@Mixin(value = PotatoProjectileState.class, remap = false)
public class MixinPotatoProjectileState implements IExtraRenderParts {
    @Unique
    private final List<SuperByteBufferRenderState> railways$extraParts = new ArrayList<>();

    @Override
    public List<SuperByteBufferRenderState> railways$getExtraParts() {
        return railways$extraParts;
    }
}
