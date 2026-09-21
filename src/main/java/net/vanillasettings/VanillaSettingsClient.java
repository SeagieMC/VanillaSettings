package net.vanillasettings;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.vanillasettings.config.ModConfig;
import net.vanillasettings.gui.ConfigScreen;
import net.vanillasettings.hud.AlertToast;
import net.vanillasettings.hud.ArmorHud;
import net.vanillasettings.lang.Text;
import net.vanillasettings.util.BrightnessTransition;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Vanilla Settings
 * -----------------
 * A single button flips between "pure vanilla" and a crystal-PvP visual preset.
 * Everything it touches is a normal client-side render/option setting - nothing
 * here changes gameplay, so it's fine to run on any vanilla server including
 * anti-cheat protected ones like DonutSMP.
 */
public class VanillaSettingsClient implements ClientModInitializer {

    public static final String MOD_ID = "vanillasettings";

    /** The two keys the user can bind from the settings screen. */
    public enum BindTarget {
        /** Toggles crystal mode on/off. */
        ACTIVATE,
        /** Opens the mod's settings screen. */
        MENU
    }

    private static ModConfig config;
    private static boolean active = false;
    private static boolean activateWasDown = false;
    private static boolean menuWasDown = false;
    /** When non-null, the next key press is captured as the new bind for that target instead of being used normally. */
    private static BindTarget listeningFor = null;

    // remembered vanilla state, restored exactly when switching back off
    private static Object prevParticles = null;
    private static int prevRenderDistance = -1;

    // BactroMod-style lightmap multiplier. BactroMod uses a 1-15 gamma
    // multiplier on the lightmap rather than changing the player's vanilla
    // gamma option. The fade between 1 (vanilla) and the configured strength is
    // one continuous value that is used for BOTH directions, so switching the mod
    // on/off in the middle of a fade continues smoothly from where it currently is.
    private static final BrightnessTransition brightness = new BrightnessTransition();

    public static ModConfig getConfig() {
        return config;
    }

    public static boolean isActive() {
        return active;
    }

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();

        // Do not access mc.options here: Fabric client initialization happens before
        // Minecraft has finished constructing its Options instance on 26.2.
        ClientTickEvents.END_CLIENT_TICK.register(VanillaSettingsClient::onClientTick);

        // The armor HUD is attached right after the vanilla hotbar so it is drawn in the same layer.
        // That way it is dimmed together with the hotbar whenever a screen (inventory, chest, ...)
        // is open. ("addLast" would draw it after the open screen, on top of the dark overlay.)
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "armor_hud"),
                VanillaSettingsClient::renderArmorHud
        );

        // The alert toast is meant to stay bright and on top of everything.
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "alert_toast"),
                VanillaSettingsClient::renderToast
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("vanillasettings")
                        .executes(ctx -> {
                            openConfigScreen();
                            return 1;
                        })));
    }

    private static void openConfigScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new ConfigScreen(mc.gui.screen())));
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.player == null) {
            activateWasDown = false;
            menuWasDown = false;
            return;
        }

        pollBinds(mc);
    }

    /**
     * Polls both binds. The "was down" state is tracked even while a screen is open so
     * that a key which is still held when a screen closes doesn't fire a second time -
     * but the binds only ever <i>act</i> when no screen is open (which also covers our
     * own settings screen and the "press a key to bind" prompt).
     */
    private static void pollBinds(Minecraft mc) {
        boolean activateDown = isBindDown(mc, config.bindKeyCode);
        boolean menuDown = isBindDown(mc, config.menuBindKeyCode);

        if (mc.gui.screen() == null && listeningFor == null) {
            if (activateDown && !activateWasDown) {
                toggle();
            }
            if (menuDown && !menuWasDown) {
                openConfigScreen();
            }
        }

        activateWasDown = activateDown;
        menuWasDown = menuDown;
    }

    private static boolean isBindDown(Minecraft mc, int keyCode) {
        return keyCode != -1 && InputConstants.isKeyDown(mc.getWindow(), keyCode);
    }

    // --- bind capture (used by the config screen) ---

    /** Start capturing the next key press as the bind for {@code target}; pass {@code null} to stop listening. */
    public static void setListeningFor(BindTarget target) {
        listeningFor = target;
    }

    public static BindTarget getListeningFor() {
        return listeningFor;
    }

    public static boolean isListeningForBind() {
        return listeningFor != null;
    }

    /**
     * Stores {@code glfwKeyCode} as the bind for whatever we're currently listening for
     * and stops listening. Pass -1 to unbind. If the other bind already uses the same
     * key it gets unbound, so one key press can never do both things at once.
     */
    public static void captureBindKey(int glfwKeyCode) {
        BindTarget target = listeningFor;
        listeningFor = null;
        if (target == null) {
            return;
        }

        if (target == BindTarget.ACTIVATE) {
            config.bindKeyCode = glfwKeyCode;
            if (glfwKeyCode != -1 && config.menuBindKeyCode == glfwKeyCode) {
                config.menuBindKeyCode = -1;
            }
        } else {
            config.menuBindKeyCode = glfwKeyCode;
            if (glfwKeyCode != -1 && config.bindKeyCode == glfwKeyCode) {
                config.bindKeyCode = -1;
            }
        }
        config.save();
    }

    public static String bindDisplayName(BindTarget target) {
        return keyName(target == BindTarget.ACTIVATE ? config.bindKeyCode : config.menuBindKeyCode);
    }

    private static String keyName(int keyCode) {
        if (keyCode == -1) {
            return Text.NOT_BOUND.in(config.language);
        }
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) {
            return name.toUpperCase();
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LEFT SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RIGHT SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LEFT CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RIGHT CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LEFT ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RIGHT ALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS LOCK";
            case GLFW.GLFW_KEY_INSERT -> "INSERT";
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PAGE UP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PAGE DOWN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> "KEY " + keyCode;
        };
    }

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        active = !active;

        if (active) {
            applyCrystalMode(mc);
            if (config.showAlertToast) {
                AlertToast.show("&fSwitched to &dCrystal &fmode.");
            }
        } else {
            restoreVanillaMode(mc);
            if (config.showAlertToast) {
                AlertToast.show("&fSwitched to &aVanilla &fmode.");
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setOptionValue(OptionInstance<?> option, Object value) {
        ((OptionInstance) option).set(value);
    }

    private static Object findParticleStatus(String constantName) {
        Object current = Minecraft.getInstance().options.particles().get();
        if (!(current instanceof Enum<?> currentEnum)) {
            return null;
        }

        for (Object constant : currentEnum.getDeclaringClass().getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(constantName)) {
                return constant;
            }
        }
        return null;
    }

    private static void renderArmorHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }

        if (config != null && active && config.showArmorHud) {
            ArmorHud.render(graphics, mc.font, mc, graphics.guiWidth(), graphics.guiHeight());
        }
    }

    private static void renderToast(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }

        if (config != null && config.showAlertToast) {
            AlertToast.render(graphics, mc.font, graphics.guiWidth(), graphics.guiHeight());
        }
    }

    private static void applyCrystalMode(Minecraft mc) {
        OptionInstance<?> particles = mc.options.particles();
        if (config.disableParticles) {
            prevParticles = particles.get();
            Object minimal = findParticleStatus("MINIMAL");
            if (minimal != null) {
                setOptionValue(particles, minimal);
            }
        }

        if (config.switchRenderDistance) {
            prevRenderDistance = mc.options.renderDistance().get();
            mc.options.renderDistance().set(config.crystalRenderDistance);
        }

        fadeBrightness(true);
    }

    private static void restoreVanillaMode(Minecraft mc) {
        if (config.disableParticles && prevParticles != null) {
            setOptionValue(mc.options.particles(), prevParticles);
            prevParticles = null;
        }

        if (config.switchRenderDistance) {
            mc.options.renderDistance().set(config.vanillaRenderDistance);
        }

        fadeBrightness(false);
    }

    /**
     * Starts fading the lightmap multiplier towards the crystal strength (mod on) or back to
     * vanilla (mod off). The fade always starts from the multiplier's <i>current</i> value, so
     * switching the mod off (or back on) part-way through a fade reverses smoothly instead of
     * jumping, and takes only as long as the remaining distance needs.
     */
    private static void fadeBrightness(boolean crystalOn) {
        if (!config.smoothBrightness) {
            // Feature disabled: keep the internal state at vanilla so it can never leave a stale
            // value behind that would pop up later if the option is re-enabled.
            brightness.snapTo(BrightnessTransition.VANILLA);
            return;
        }

        int strength = Math.max(1, Math.min(15, config.crystalBrightness));
        double target = crystalOn ? strength : BrightnessTransition.VANILLA;
        brightness.fadeTo(target, strength, config.brightnessTransitionSeconds, System.nanoTime());
    }

    /**
     * Current BactroMod-style lightmap multiplier, from 1 (vanilla) to 15.
     * <p>
     * Deliberately NOT gated on {@link #isActive()}: when the mod is switched off the value has to
     * keep fading down to 1, otherwise the brightness would snap back instantly.
     */
    public static float brightnessMultiplier() {
        if (config == null || !config.smoothBrightness) {
            return 1.0f;
        }
        double value = brightness.valueAt(System.nanoTime());
        return (float) Math.max(1.0, Math.min(15.0, value));
    }

    // --- read by mixins ---

    /** True when every kind of fog (lava, powder snow, blindness, darkness, water, atmospheric) should be suppressed. */
    public static boolean shouldDisableFog() {
        return active && config.noFog;
    }

    /** True while the mod is on and "Totem Resize" is enabled (covers both totem sizes and the centered pop). */
    public static boolean shouldResizeTotem() {
        return active && config.resizeTotem;
    }

    /** Size of the totem pop animation, 1.0 = vanilla. */
    public static float totemPopScale() {
        return config.totemPopScale;
    }

    /** Size of a totem held in hand, 1.0 = vanilla. */
    public static float totemHandScale() {
        return config.totemHandScale;
    }

    /** True while every particle should be blocked from spawning (including explosions). */
    public static boolean shouldDisableParticles() {
        return active && config.disableParticles;
    }

    public static float fireOffset() {
        return (active && config.applyOffsets) ? config.fireOffset : 0f;
    }

    public static float shieldOffset() {
        return (active && config.applyOffsets) ? config.shieldOffset : 0f;
    }
}
