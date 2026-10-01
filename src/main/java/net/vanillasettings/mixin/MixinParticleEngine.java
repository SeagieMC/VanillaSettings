package net.vanillasettings.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.vanillasettings.VanillaSettingsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hard particle switch. Vanilla's "Minimal" particle setting still lets "important" particles
 * through - explosions are the classic example - so instead of relying on it we stop every
 * particle from being created at all while the switch is on.
 * <p>
 * Every particle spawned through the level (including the explosion emitter and the particles
 * it spawns) goes through {@link ParticleEngine#createParticle}; returning null there means
 * "no particle", which the callers already handle - with one exception, see
 * {@link #vs$isUnsafeToBlock}.
 */
@Mixin(ParticleEngine.class)
public class MixinParticleEngine {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void vs$blockParticles(ParticleOptions options, double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ,
                                   CallbackInfoReturnable<Particle> cir) {
        if (VanillaSettingsClient.shouldDisableParticles() && !vs$isUnsafeToBlock(options)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * Firework explosions are the one case that must never be cancelled here. Each spark of a
     * firework burst is spawned by {@code FireworkParticles$Starter} calling this exact method
     * with {@link ParticleTypes#FIREWORK} and then immediately calling a method on the particle
     * it gets back, with no null check. Cancelling that call (returning null, as we do for every
     * other particle) crashes the game the instant any firework with an effect explodes -
     * this is exactly the crash reported against this mod. Leaving this one type alone means
     * firework sparks stay visible while the mod is on; every other "explosion"-style particle
     * (TNT, creepers, and so on) is still blocked as before.
     */
    private static boolean vs$isUnsafeToBlock(ParticleOptions options) {
        return options.getType() == ParticleTypes.FIREWORK;
    }
}
