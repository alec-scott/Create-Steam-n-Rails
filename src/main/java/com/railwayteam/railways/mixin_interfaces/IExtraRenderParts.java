package com.railwayteam.railways.mixin_interfaces;

import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;

import java.util.List;

/**
 * Duck for Create Fly block entity render states that Railways adds parts to (track casings, virtual couplers):
 * parts extracted for the frame, submitted after the original renderer's.
 */
public interface IExtraRenderParts {
    List<SuperByteBufferRenderState> railways$getExtraParts();
}
