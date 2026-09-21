package net.vanillasettings.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.vanillasettings.VanillaSettingsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the totem pop in the middle of the screen.
 * <p>
 * Vanilla gives every pop a random horizontal and vertical offset, which is why the totem so
 * often flies off towards the bottom or a corner. With Totem Resize on, both offsets are set to
 * zero right after vanilla picks them (totem-tweaks' "lock position" behaviour).
 */
@Mixin(ScreenEffectRenderer.class)
public class MixinTotemPopPosition {

    @Shadow
    private float itemActivationOffX;

    @Shadow
    private float itemActivationOffY;

    @Inject(method = "displayItemActivation", at = @At("TAIL"))
    private void vs$centerTotemPop(ItemStack stack, RandomSource random, CallbackInfo ci) {
        if (VanillaSettingsClient.shouldResizeTotem()) {
            this.itemActivationOffX = 0.0F;
            this.itemActivationOffY = 0.0F;
        }
    }
}
