package net.vanillasettings.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
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
 * "no particle", which the callers already handle.
 */
@Mixin(ParticleEngine.class)
public class MixinParticleEngine {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void vs$blockParticles(ParticleOptions options, double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ,
                                   CallbackInfoReturnable<Particle> cir) {
        if (VanillaSettingsClient.shouldDisableParticles()) {
            cir.setReturnValue(null);
        }
    }
}
