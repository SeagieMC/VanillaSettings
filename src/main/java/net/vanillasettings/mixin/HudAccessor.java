package net.vanillasettings.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets us reuse vanilla's own hotbar-slot background sprite for the armor HUD,
 * so it looks like a native part of the hotbar instead of a floating icon row.
 * <p>
 * Field name confirmed against uku3lig/armor-hud's own "26.2" branch, which is a
 * real, currently-maintained mod targeting this exact Minecraft version.
 */
@Mixin(Hud.class)
public interface HudAccessor {

    @Accessor("HOTBAR_SPRITE")
    static Identifier vs$hotbarSprite() {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
