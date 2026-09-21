package net.vanillasettings.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.vanillasettings.VanillaSettingsClient;
import net.vanillasettings.VanillaSettingsClient.BindTarget;
import net.vanillasettings.config.ModConfig;
import net.vanillasettings.lang.Language;
import net.vanillasettings.lang.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * The settings screen. Everything sits in one scrolling list, split into five sections
 * (separated by a thin line) with only the Done button pinned to the bottom:
 * <ol>
 *   <li>visuals: particles, fog, fire/shield offsets, armor HUD</li>
 *   <li>brightness</li>
 *   <li>render distance</li>
 *   <li>totem</li>
 *   <li>binds, alert toast, language</li>
 * </ol>
 */
public class ConfigScreen extends Screen {

    /** Space reserved above the list for the title, and below it for the Done button. */
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 32;

    private final Screen parent;
    private ConfigList list;
    private Button doneButton;
    /** Set by the language button; handled at the start of the next frame (never in the middle of a click). */
    private boolean rebuildPending = false;

    public ConfigScreen(Screen parent) {
        super(Component.literal(Text.TITLE.in(VanillaSettingsClient.getConfig().language)));
        this.parent = parent;
    }

    private Language language() {
        return VanillaSettingsClient.getConfig().language;
    }

    @Override
    protected void init() {
        // The list fills everything between the title and the Done button.
        list = new ConfigList(minecraft, width, height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT,
                () -> rebuildPending = true);
        this.addRenderableWidget(list);

        doneButton = Button.builder(
                Component.literal(Text.DONE.in(language())),
                button -> onClose()
        ).bounds(width / 2 - 100, height - FOOTER_HEIGHT + 6, 200, 20).build();
        this.addRenderableWidget(doneButton);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (VanillaSettingsClient.isListeningForBind()) {
            // Esc clears the bind (like vanilla's Controls screen) instead of binding Esc itself.
            int key = event.key() == GLFW.GLFW_KEY_ESCAPE ? -1 : event.key();
            VanillaSettingsClient.captureBindKey(key);
            // Also refreshes the *other* button: binding a key that the other bind used unbinds it.
            list.refreshBindLabels();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        VanillaSettingsClient.setListeningFor(null);
        VanillaSettingsClient.getConfig().save();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (rebuildPending) {
            rebuildPending = false;
            list.rebuild();
            doneButton.setMessage(Component.literal(Text.DONE.in(language())));
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.literal(Text.TITLE.in(language())), this.width / 2, 12, 0xFFFFFFFF);
    }

    static final class ConfigList extends ContainerObjectSelectionList<ConfigList.Entry> {

        static final int ROW_WIDTH = 310;
        private static final int ROW_HEIGHT = 25;
        private static final int BUTTON_HEIGHT = 20;
        /** The language button always reads "Language: ..." regardless of the selected language. */
        private static final String LANGUAGE_LABEL = "Language: ";
        /** The totem size sliders move in steps of 0.05 (so 0.40, 0.45, 0.50, ...). */
        private static final int SCALE_STEPS_PER_UNIT = 20;

        private final ModConfig cfg = VanillaSettingsClient.getConfig();
        private final Runnable onLanguageChanged;
        private Button activateBindButton;
        private Button menuBindButton;

        /**
         * The super constructor is (client, width, HEIGHT, Y, itemHeight) - this is the argument
         * order used by vanilla and by Fabric API's own 26.2 list classes.
         */
        ConfigList(Minecraft minecraft, int listWidth, int listHeight, int listY, Runnable onLanguageChanged) {
            super(minecraft, listWidth, listHeight, listY, ROW_HEIGHT);
            this.onLanguageChanged = onLanguageChanged;
            this.centerListVertically = false;
            populate();
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        /** Rebuilds every row (used after a language change). Same list widget, so the scroll position stays. */
        void rebuild() {
            clearEntries();
            populate();
        }

        private void populate() {
            Language lang = cfg.language;

            // --- section 1: visuals ---
            addToggle(Text.PARTICLES, () -> cfg.disableParticles, v -> cfg.disableParticles = v);
            addToggle(Text.NO_FOG, () -> cfg.noFog, v -> cfg.noFog = v);
            addToggle(Text.FIRE_SHIELD_OFFSETS, () -> cfg.applyOffsets, v -> cfg.applyOffsets = v);
            addToggle(Text.ARMOR_HUD, () -> cfg.showArmorHud, v -> cfg.showArmorHud = v);
            addSeparator();

            // --- section 2: brightness ---
            addToggle(Text.SMOOTH_FULLBRIGHT, () -> cfg.smoothBrightness, v -> cfg.smoothBrightness = v);
            addSlider(
                    Text.FULLBRIGHT_STRENGTH,
                    1, 15, cfg.crystalBrightness,
                    value -> cfg.crystalBrightness = value,
                    value -> value + Text.STRENGTH_SUFFIX.in(lang)
            );
            addSlider(
                    Text.BRIGHTNESS_TRANSITION,
                    ModConfig.BRIGHTNESS_TRANSITION_MIN_SECONDS, ModConfig.BRIGHTNESS_TRANSITION_MAX_SECONDS,
                    Math.round(cfg.brightnessTransitionSeconds),
                    value -> cfg.brightnessTransitionSeconds = value,
                    value -> value + Text.SECONDS_SUFFIX.in(lang)
            );
            addSeparator();

            // --- section 3: render distance ---
            addToggle(Text.RENDER_DISTANCE_SWITCH, () -> cfg.switchRenderDistance, v -> cfg.switchRenderDistance = v);
            addSlider(
                    Text.RENDER_DISTANCE_ON,
                    2, 32, cfg.crystalRenderDistance,
                    value -> cfg.crystalRenderDistance = value,
                    value -> Text.chunks(lang, value)
            );
            addSlider(
                    Text.RENDER_DISTANCE_OFF,
                    2, 32, cfg.vanillaRenderDistance,
                    value -> cfg.vanillaRenderDistance = value,
                    value -> Text.chunks(lang, value)
            );
            addSeparator();

            // --- section 4: totem ---
            addToggle(Text.TOTEM_RESIZE, () -> cfg.resizeTotem, v -> cfg.resizeTotem = v);
            addSlider(
                    Text.TOTEM_SIZE,
                    scaleToSteps(ModConfig.TOTEM_SCALE_MIN), scaleToSteps(ModConfig.TOTEM_SCALE_MAX),
                    scaleToSteps(cfg.totemHandScale),
                    steps -> cfg.totemHandScale = stepsToScale(steps),
                    ConfigList::formatScale
            );
            addSlider(
                    Text.TOTEM_POP_SIZE,
                    scaleToSteps(ModConfig.TOTEM_SCALE_MIN), scaleToSteps(ModConfig.TOTEM_SCALE_MAX),
                    scaleToSteps(cfg.totemPopScale),
                    steps -> cfg.totemPopScale = stepsToScale(steps),
                    ConfigList::formatScale
            );
            addSeparator();

            // --- section 5: binds, alert toast, language ---
            activateBindButton = createBindButton(BindTarget.ACTIVATE);
            menuBindButton = createBindButton(BindTarget.MENU);
            addEntry(new WidgetEntry(activateBindButton));
            addEntry(new WidgetEntry(menuBindButton));
            addToggle(Text.ALERT_TOAST, () -> cfg.showAlertToast, v -> cfg.showAlertToast = v);
            addLanguageButton();
        }

        // --- helpers for the two totem sliders ---

        private static int scaleToSteps(float scale) {
            return Math.round(scale * SCALE_STEPS_PER_UNIT);
        }

        private static float stepsToScale(int steps) {
            return steps / (float) SCALE_STEPS_PER_UNIT;
        }

        private static String formatScale(int steps) {
            return String.format(Locale.ROOT, "%.2f", steps / (double) SCALE_STEPS_PER_UNIT);
        }

        // --- row builders ---

        private void addToggle(Text label, BooleanSupplier get, Consumer<Boolean> set) {
            Language lang = cfg.language;
            Button button = Button.builder(
                    toggleLabel(lang, label, get.getAsBoolean()),
                    b -> {
                        boolean next = !get.getAsBoolean();
                        set.accept(next);
                        b.setMessage(toggleLabel(lang, label, next));
                    }
            ).bounds(0, 0, ROW_WIDTH, BUTTON_HEIGHT).build();
            addEntry(new WidgetEntry(button));
        }

        private static Component toggleLabel(Language lang, Text label, boolean on) {
            return Component.literal(label.in(lang) + ": " + (on ? Text.ON : Text.OFF).in(lang));
        }

        /**
         * @param valueText turns the slider's integer value into the text shown after the label,
         *                  e.g. 3 -> "3/15" or 8 -> "0.40"
         */
        private void addSlider(Text label, int min, int max, int current,
                               IntConsumer setter, IntFunction<String> valueText) {
            String labelText = label.in(cfg.language);
            OptionInstance<Integer> option = new OptionInstance<>(
                    labelText,
                    OptionInstance.cachedConstantTooltip(Component.literal(labelText)),
                    (caption, value) -> Component.literal(labelText + ": " + valueText.apply(value)),
                    new OptionInstance.IntRange(min, max),
                    Math.max(min, Math.min(max, current)),
                    setter::accept
            );
            addOption(option);
        }

        private void addOption(OptionInstance<?> option) {
            addEntry(new WidgetEntry(option.createButton(minecraft.options, 0, 0, ROW_WIDTH)));
        }

        private void addSeparator() {
            addEntry(new SeparatorEntry());
        }

        private void addLanguageButton() {
            Button button = Button.builder(
                    Component.literal(LANGUAGE_LABEL + cfg.language.displayName()),
                    b -> {
                        cfg.language = cfg.language.next();
                        onLanguageChanged.run();
                    }
            ).bounds(0, 0, ROW_WIDTH, BUTTON_HEIGHT).build();
            addEntry(new WidgetEntry(button));
        }

        // --- key bind buttons ---

        private Button createBindButton(BindTarget target) {
            return Button.builder(
                    bindLabel(target),
                    button -> {
                        VanillaSettingsClient.setListeningFor(target);
                        refreshBindLabels();
                    }
            ).bounds(0, 0, ROW_WIDTH, BUTTON_HEIGHT).build();
        }

        private Component bindLabel(BindTarget target) {
            Language lang = cfg.language;
            String name = (target == BindTarget.ACTIVATE ? Text.ACTIVATE_BIND : Text.MENU_BIND).in(lang);
            String state = VanillaSettingsClient.getListeningFor() == target
                    ? Text.PRESS_A_KEY.in(lang)
                    : VanillaSettingsClient.bindDisplayName(target);
            return Component.literal(name + ": " + state);
        }

        void refreshBindLabels() {
            if (activateBindButton != null) {
                activateBindButton.setMessage(bindLabel(BindTarget.ACTIVATE));
            }
            if (menuBindButton != null) {
                menuBindButton.setMessage(bindLabel(BindTarget.MENU));
            }
        }

        // --- rows ---

        abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        }

        /** One full-width row holding a single widget (a toggle, a slider, or a button). */
        static final class WidgetEntry extends Entry {
            private final AbstractWidget widget;

            WidgetEntry(AbstractWidget widget) {
                this.widget = widget;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                int x = getContentXMiddle() - widget.getWidth() / 2;
                widget.setPosition(x, getContentY());
                widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(widget);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(widget);
            }
        }

        /** A thin horizontal line between two sections. Nothing to click or narrate. */
        static final class SeparatorEntry extends Entry {
            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                int middle = getContentXMiddle();
                int y = getContentY() + BUTTON_HEIGHT / 2;
                graphics.fill(middle - ROW_WIDTH / 2, y, middle + ROW_WIDTH / 2, y + 1, 0x66FFFFFF);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }
        }
    }
}
