package net.vanillasettings.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.vanillasettings.lang.Language;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Everything here is what gets flipped when "Crystal mode" is switched on.
 * Every field is exposed in {@link net.vanillasettings.gui.ConfigScreen}
 * so you can pick exactly what the single activation button changes.
 * <p>
 * Saved to config/vanillasettings.json
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path PATH = CONFIG_DIR.resolve("vanillasettings.json");
    /** Config file name from before the mod was renamed. Read once so nobody loses their settings. */
    private static final Path LEGACY_PATH = CONFIG_DIR.resolve("bettervanilla.json");

    // Limits shared by the settings screen and the config loader.
    public static final float TOTEM_SCALE_MIN = 0.10f;
    public static final float TOTEM_SCALE_MAX = 2.00f;
    public static final int BRIGHTNESS_TRANSITION_MIN_SECONDS = 1;
    public static final int BRIGHTNESS_TRANSITION_MAX_SECONDS = 5;

    // --- language ---
    /** Language of the settings screen. English until the player changes it. */
    public Language language = Language.ENGLISH;

    // --- activation ---
    /** GLFW key code for the single-button toggle. -1 = not bound. */
    public int bindKeyCode = -1;

    /** GLFW key code that opens this settings screen in-game. -1 = not bound. */
    public int menuBindKeyCode = -1;

    // --- visuals turned off in crystal mode ---
    public boolean disableParticles = true;

    /**
     * One switch for every kind of fog vanilla can draw: lava, powder snow,
     * blindness, darkness, water and the normal atmospheric distance fog.
     */
    public boolean noFog = true;

    // --- totem ---
    /**
     * Master switch for both totem sizes below. (Called "resizeTotemPop" in older versions,
     * which is still accepted when reading an old config file.)
     */
    @SerializedName(value = "resizeTotem", alternate = {"resizeTotemPop"})
    public boolean resizeTotem = true;
    /** Size of the totem held in your hands. 1.0 = vanilla. */
    public float totemHandScale = 1.0f;
    /** Size of the totem pop animation. 1.0 = vanilla, 0.5 = half size, etc. */
    public float totemPopScale = 0.5f;

    // --- brightness ---
    public boolean smoothBrightness = true;
    /** 1-15; 1 is vanilla, higher is brighter. */
    public int crystalBrightness = 3;
    /** How long a full fade between vanilla and the configured strength takes (1-5 s). */
    public float brightnessTransitionSeconds = 3.0f;

    // --- render distance ---
    public boolean switchRenderDistance = true;
    public int crystalRenderDistance = 12;
    public int vanillaRenderDistance = 32;

    // --- held item offsets (crystal mode only) ---
    // Values are divided by 100 before being applied (matches the -100..100 style used by BactroMod).
    public boolean applyOffsets = true;
    public float fireOffset = -100f;
    public float shieldOffset = -20f;

    // --- HUD ---
    public boolean showArmorHud = true;
    public boolean showAlertToast = true;

    public static ModConfig load() {
        Path source = Files.exists(PATH) ? PATH : (Files.exists(LEGACY_PATH) ? LEGACY_PATH : null);

        if (source != null) {
            try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    if (source != PATH) {
                        // First launch after the rename: carry the old settings over.
                        loaded.save();
                    }
                    return loaded;
                }
            } catch (IOException | RuntimeException e) {
                System.err.println("[Vanilla Settings] Failed to read config, using defaults: " + e);
            }
        }
        ModConfig fresh = new ModConfig();
        fresh.save();
        return fresh;
    }

    /** Clamps everything that came from a file (which may be hand-edited or from an older version). */
    private void sanitize() {
        if (language == null) {
            language = Language.ENGLISH;
        }
        crystalBrightness = Math.max(1, Math.min(15, crystalBrightness));
        brightnessTransitionSeconds = Math.max(BRIGHTNESS_TRANSITION_MIN_SECONDS,
                Math.min(BRIGHTNESS_TRANSITION_MAX_SECONDS, brightnessTransitionSeconds));
        totemPopScale = Math.max(TOTEM_SCALE_MIN, Math.min(TOTEM_SCALE_MAX, totemPopScale));
        totemHandScale = Math.max(TOTEM_SCALE_MIN, Math.min(TOTEM_SCALE_MAX, totemHandScale));
        crystalRenderDistance = Math.max(2, Math.min(32, crystalRenderDistance));
        vanillaRenderDistance = Math.max(2, Math.min(32, vanillaRenderDistance));
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[Vanilla Settings] Failed to save config: " + e);
        }
    }
}
