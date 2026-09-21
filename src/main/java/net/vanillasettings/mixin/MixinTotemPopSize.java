package net.vanillasettings.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.vanillasettings.VanillaSettingsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Size of the totem-pop animation.
 * <p>
 * The hook is the same one totem-tweaks uses on 26.2: the animation draws the item with a single
 * uniform PoseStack.scale(x, y, z) call, and vanilla's value for it is 0.8. We replace it with
 * 0.8 * the configured size, so a setting of 1.0 is exactly vanilla and 0.4 is 40% of vanilla.
 * <p>
 * This lives in its own mixin class on purpose: if any other injection into ScreenEffectRenderer
 * ever fails to apply, the totem size must not go down with it.
 */
@Mixin(ScreenEffectRenderer.class)
public class MixinTotemPopSize {

    @ModifyArgs(
            method = "renderItemActivationAnimation",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            )
    )
    private void vs$scaleTotemPop(Args args) {
        if (!VanillaSettingsClient.shouldResizeTotem()) {
            return;
        }

        float scale = 0.8F * VanillaSettingsClient.totemPopScale();
        args.set(0, scale);
        args.set(1, scale);
        args.set(2, scale);
    }
}
