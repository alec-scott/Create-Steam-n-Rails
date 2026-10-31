/*
 * Steam 'n' Rails
 * Copyright (c) 2024 The Railways Team
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

package com.railwayteam.railways.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.railwayteam.railways.content.custom_tracks.phantom.PhantomSpriteManager;
import com.railwayteam.railways.mixin_interfaces.AnimatedTextureDuck;
import com.railwayteam.railways.mixin_interfaces.IPotentiallyInvisibleSpriteContents;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Phantom track sprites switch between their first (visible) and second (invisible) animation frame.
 *
 * <p>PORT-NOTE(26.1): sprite animations are drawn to the atlas on the GPU from a per-sprite
 * {@link SpriteContents.AnimationState} that advances a frame index each tick; 1.20 uploaded frames directly. The
 * frame is now pinned in {@code AnimationState.tick} and marked dirty when it changes.</p>
 */
@Mixin(SpriteContents.class)
public abstract class MixinSpriteContents implements IPotentiallyInvisibleSpriteContents {
    @Inject(method = "<init>(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)V",
        at = @At("RETURN"))
    private void railways$onInit(Identifier name, FrameSize frameSize, NativeImage originalImage,
                                 Optional<AnimationMetadataSection> animationInfo,
                                 List<MetadataSectionType.WithValue<?>> additionalMetadata,
                                 Optional<TextureMetadataSection> textureInfo, CallbackInfo ci) {
        if (PhantomSpriteManager.register((SpriteContents) (Object) this))
            railways$shouldDoInvisibility = true;
    }

    @Unique
    private boolean railways$visible = true;

    @Unique
    private boolean railways$shouldDoInvisibility = false;

    @Override
    public void railways$uploadFrame(boolean visible) {
        this.railways$visible = visible;
        this.railways$shouldDoInvisibility = true;
    }

    public boolean railways$shouldDoInvisibility() {
        return railways$shouldDoInvisibility;
    }

    public boolean railways$isVisible() {
        return railways$visible || !railways$shouldDoInvisibility;
    }

    @Mixin(SpriteContents.AnimatedTexture.class)
    public abstract static class MixinAnimatedTexture implements AnimatedTextureDuck {
        @Shadow
        @Final
        SpriteContents this$0;

        @Override
        public SpriteContents railways$getContents() {
            return this$0;
        }
    }

    @Mixin(SpriteContents.AnimationState.class)
    public abstract static class MixinAnimationState {
        @Shadow private int frame;
        @Shadow private int subFrame;
        @Shadow private boolean isDirty;
        @Shadow @Final private SpriteContents.AnimatedTexture animationInfo;

        @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
        private void railways$pinPhantomFrame(CallbackInfo ci) {
            SpriteContents contents = ((AnimatedTextureDuck) animationInfo).railways$getContents();
            IPotentiallyInvisibleSpriteContents sprite = (IPotentiallyInvisibleSpriteContents) contents;
            if (!sprite.railways$shouldDoInvisibility())
                return;
            int target = sprite.railways$isVisible() ? 0 : 1;
            if (frame != target) {
                frame = target;
                subFrame = 0;
                isDirty = true;
            }
            ci.cancel();
        }
    }
}
