package com.railwayteam.railways.mixin_interfaces;

import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;

import java.util.List;

/** Duck for Create Fly's {@code TrackRenderer.TrackRenderState}: track casing parts extracted for the frame. */
public interface ICasingRenderState {
    List<SuperByteBufferRenderState> railways$getCasingParts();
}
