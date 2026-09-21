package net.vanillasettings.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.vanillasettings.VanillaSettingsClient;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;

/**
 * Suppresses every kind of fog (lava, powder snow, blindness, darkness, water,
 * atmospheric) behind a single toggle. Injection point, field names and the
 * FogData reset values are copied directly from BactroMod's own MixinFogRenderer,
 * which is a real, currently-maintained mod confirmed to work on Minecraft 26.2.
 */
@Mixin(value = FogRenderer.class, priority = 1500)
public abstract class MixinFogRenderer {

    @Shadow
    @Final
    private static List<FogEnvironment> FOG_ENVIRONMENTS;

    @Inject(
            method = "setupFog",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;renderDistanceEnd:F", ordinal = 0, shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD)
    )
    private void vs$suppressFog(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> cir, @Local(name = "fog") FogData fog, @Local(name = "fogType") FogType fogType, @Local(name = "entity") Entity entity, @Local(name = "renderDistanceInBlocks") float renderDistanceInBlocks) {
        if (!VanillaSettingsClient.shouldDisableFog()) {
            return;
        }

        for (int i = 0; i < FOG_ENVIRONMENTS.size(); ++i) {
            if (FOG_ENVIRONMENTS.get(i).isApplicable(fogType, entity)) {
                fog.environmentalStart = Float.MAX_VALUE;
                fog.environmentalEnd = Float.MAX_VALUE;
                fog.renderDistanceStart = Float.MAX_VALUE;
                fog.renderDistanceEnd = Float.MAX_VALUE;

                // atmospheric fog (index 5) still needs a sane sky/cloud cutoff,
                // everything else can just be pushed out to "never happens".
                fog.skyEnd = i == 5 ? Mth.clamp(renderDistanceInBlocks, 2 * 16, 32 * 16) : Float.MAX_VALUE;
                fog.cloudEnd = i == 5 ? Minecraft.getInstance().options.cloudRange().get() * 16 : Float.MAX_VALUE;

                break;
            }
        }
    }
}
