package com.railwayteam.railways.mixin.client;

import com.railwayteam.railways.mixin_interfaces.IExtraRenderParts;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.trains.track.TrackRenderer.TrackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

/** PORT-NOTE(26.1): holds the casing parts {@link MixinTrackRenderer} extracts. */
@Mixin(value = TrackRenderState.class, remap = false)
public class MixinTrackRenderState implements IExtraRenderParts {
    @Unique
    private final List<SuperByteBufferRenderState> railways$casingParts = new ArrayList<>();

    @Override
    public List<SuperByteBufferRenderState> railways$getExtraParts() {
        return railways$casingParts;
    }
}
