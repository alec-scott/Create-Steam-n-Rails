/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.util.DevCapeUtils;
import com.railwayteam.railways.util.Utils;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Dev capes for Railways developers, and the conductor skin inside the dev env.
 *
 * <p>PORT-NOTE(26.1): {@code PlayerInfo} resolves a whole {@link PlayerSkin} record (no texture map or skin model
 * string), so both are applied to the skin it returns. The dev-env skin used a method-level {@code @DevEnvMixin}, which
 * only the unported release-build stripping honoured, so it is checked at runtime instead.</p>
 */
@Mixin(PlayerInfo.class)
public class MixinPlayerInfo {
    @Shadow @Final private GameProfile profile;

    @Unique private static final ClientAsset.ResourceTexture DEV_CAPE = new ClientAsset.ResourceTexture(
        Railways.asResource("misc/dev_cape"), Railways.asResource("textures/misc/dev_cape.png"));
    @Unique private static final ClientAsset.ResourceTexture DEV_SKIN = new ClientAsset.ResourceTexture(
        Railways.asResource("misc/devenv_skin"), Railways.asResource("textures/misc/devenv_skin.png"));

    @ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
    private PlayerSkin railways$devSkinAndCape(PlayerSkin skin) {
        // Replaces the skin inside the dev env with the conductor skin, on the default "steve" model
        if (Utils.isDevEnv())
            skin = new PlayerSkin(DEV_SKIN, skin.cape(), skin.elytra(), PlayerModelType.WIDE, skin.secure());
        if (DevCapeUtils.INSTANCE.useDevCape(profile.id()))
            skin = new PlayerSkin(skin.body(), DEV_CAPE, skin.elytra(), skin.model(), skin.secure());
        return skin;
    }
}
