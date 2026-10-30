package com.railwayteam.railways.mixin.client;

import com.railwayteam.railways.util.CustomTrackOverlayRendering;
import com.llamalad7.mixinextras.sugar.Local;
import com.railwayteam.railways.content.custom_tracks.monorail.MonorailTrackBlock;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.zurrtum.create.client.content.trains.track.StandardTrackBlockRenderer;
import com.zurrtum.create.client.content.trains.track.StandardTrackBlockRenderer.StandardTrackBlockRenderState;
import com.zurrtum.create.client.content.trains.track.TrackBlockRenderState;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.flywheel.lib.transform.Affine;
import com.zurrtum.create.content.trains.station.StationBlockEntity;
import com.zurrtum.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.zurrtum.create.infrastructure.component.BezierTrackPointLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shifts Create's track overlays for monorails and cased tracks, and hides them on invisible phantom tracks.
 *
 * <p>PORT-NOTE(26.1): replaces {@code MixinTrackBlockClient} ({@code TrackBlock.prepareTrackOverlay} moved here in
 * Create Fly). The shift is a pure translation before the overlay's rotation, so it is applied up front: on the
 * affine for flywheel visuals, and on the render state's leading offset otherwise. The monorail's assembly overlay (was
 * {@code MonorailTrackBlock.prepareAssemblyOverlay}) is swapped and raised here too.</p>
 */
@Mixin(value = StandardTrackBlockRenderer.class, remap = false)
public class MixinStandardTrackBlockRenderer {
    @Inject(method = "prepareTrackOverlay", at = @At("HEAD"), cancellable = true)
    private <Self extends Affine<Self>> void railways$shiftOverlay(Affine<Self> affine, BlockGetter world, BlockPos pos,
                                                                   BlockState state, BezierTrackPointLocation bezierPoint,
                                                                   AxisDirection direction, RenderedTrackOverlayType type,
                                                                   CallbackInfo ci) {
        Vec3 shift = CustomTrackOverlayRendering.overlayShift(world, pos, state, bezierPoint);
        if (shift == null) {
            affine.scale(0);
            ci.cancel();
        } else if (shift != Vec3.ZERO) {
            affine.translate(shift);
        }
    }

    @Inject(method = "getRenderState", at = @At("RETURN"), cancellable = true)
    private void railways$shiftRenderState(Level world, Vec3 offset, BlockState trackState, BlockPos pos,
                                           AxisDirection direction, BezierTrackPointLocation bezier,
                                           RenderedTrackOverlayType type, float scale,
                                           CallbackInfoReturnable<TrackBlockRenderState> cir) {
        if (!(cir.getReturnValue() instanceof StandardTrackBlockRenderState state))
            return;
        Vec3 shift = CustomTrackOverlayRendering.overlayShift(world, pos, trackState, bezier);
        if (shift == null)
            cir.setReturnValue(null);
        else
            state.offset = state.offset.add(shift);
    }

    @ModifyVariable(method = "getAssemblyRenderState", at = @At("HEAD"), argsOnly = true)
    private Vec3 railways$raiseMonorailAssembly(Vec3 offset, StationBlockEntity be, Vec3 offsetArg, Level world, BlockPos pos,
                                                BlockState state) {
        return state.getBlock() instanceof MonorailTrackBlock ? offset.add(0, 14 / 16f, 0) : offset;
    }

    @ModifyArg(method = "getAssemblyRenderState", at = @At(value = "INVOKE",
        target = "Lcom/zurrtum/create/client/catnip/render/CachedBuffers;partial(Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lcom/zurrtum/create/client/catnip/render/SuperByteBuffer;"),
        index = 0)
    private PartialModel railways$monorailAssemblyModel(PartialModel model, @Local(argsOnly = true) BlockState state) {
        return state.getBlock() instanceof MonorailTrackBlock ? CRBlockPartials.MONORAIL_TRACK_ASSEMBLING_OVERLAY : model;
    }
}
