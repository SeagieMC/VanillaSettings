package net.vanillasettings.mixin;

import net.vanillasettings.VanillaSettingsClient;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * BactroMod-style fullbright for 26.2.
 *
 * BactroMod modifies the fifth lightmap gamma-multiplier argument in
 * Lightmap#render rather than changing Options.gamma(). We do the same,
 * but animate the multiplier from 1 to the configured crystal value.
 */
@Mixin(Lightmap.class)
public class MixinLightTexture {

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
                    ordinal = 5
            )
    )
    private float vs$modifyGammaMultiplier(float original) {
        return original * VanillaSettingsClient.brightnessMultiplier();
    }
}
