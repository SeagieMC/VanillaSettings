package net.vanillasettings.lang;

/**
 * Every piece of text shown on the settings screen, once per language.
 * <p>
 * To change a translation, edit the matching line below. The three strings on each
 * line are, in order: English, Russian, Mixed Russian - the same order as {@link Language}.
 */
public enum Text {

    TITLE("Vanilla Settings", "Vanilla Settings", "Vanilla Settings"),

    // --- section 1: visuals ---
    PARTICLES("Particles", "Частицы", "Партиклы"),
    NO_FOG("No Fog (all types)", "Туман (все типы)", "Туман (все типы)"),
    FIRE_SHIELD_OFFSETS("Fire/Shield Offsets", "Смещение огня/щита", "Оффсэт огня/щита"),
    ARMOR_HUD("Armor HUD", "Индикатор брони", "HUD брони"),

    // --- section 2: brightness ---
    SMOOTH_FULLBRIGHT("Smooth Fullbright", "Полная яркость", "Яркость"),
    FULLBRIGHT_STRENGTH("Fullbright Strength", "Сила яркости", "Сила яркости"),
    BRIGHTNESS_TRANSITION("Brightness Transition", "Переход яркости", "Переход яркости"),

    // --- section 3: render distance ---
    RENDER_DISTANCE_SWITCH("Render Distance Switch", "Переключение дальности прорисовки", "Прорисовка чанков"),
    RENDER_DISTANCE_ON("Render Distance (mod ON)", "Дальность прорисовки (мод включён)", "Прорисовка чанков (мод включён)"),
    RENDER_DISTANCE_OFF("Render Distance (mod OFF)", "Дальность прорисовки (мод выключен)", "Прорисовка чанков (мод выключен)"),

    // --- section 4: totem ---
    TOTEM_RESIZE("Totem Resize", "Изменение размера тотема", "Ресайз тотема"),
    TOTEM_SIZE("Totem Size", "Размер тотема", "Размер тотема"),
    TOTEM_POP_SIZE("Totem Pop Size", "Размер анимации тотема", "Размер поп тотема"),

    // --- section 5: binds, toast, language ---
    ACTIVATE_BIND("Activate bind", "Клавиша активации", "Бинд активации"),
    MENU_BIND("Open menu bind", "Клавиша настроек", "Бинд настроек"),
    ALERT_TOAST("Alert Toast", "Уведомление", "Уведомление"),

    DONE("Done", "Готово", "Готово"),

    // --- small pieces used inside the labels above ---
    ON("ON", "ВКЛ", "ВКЛ"),
    OFF("OFF", "ВЫКЛ", "ВЫКЛ"),
    PRESS_A_KEY("Press a key...", "Нажмите клавишу…", "Выберите бинд…"),
    NOT_BOUND("Not bound", "Не назначена", "Не назначен"),
    /** Appended to the seconds value of the brightness transition slider. */
    SECONDS_SUFFIX("s", " с", " с"),
    /** Appended to the strength value of the fullbright slider. */
    STRENGTH_SUFFIX("/15", "/15", "/15");

    private final String english;
    private final String russian;
    private final String mixedRussian;

    Text(String english, String russian, String mixedRussian) {
        this.english = english;
        this.russian = russian;
        this.mixedRussian = mixedRussian;
    }

    public String in(Language language) {
        if (language == null) {
            return english;
        }
        return switch (language) {
            case ENGLISH -> english;
            case RUSSIAN -> russian;
            case MIXED_RUSSIAN -> mixedRussian;
        };
    }

    /**
     * "12 chunks" in English; in both Russian variants the word follows Russian plural rules:
     * 1 чанк, 2-4 чанка, 5-20 чанков, 21 чанк, 22-24 чанка, and so on.
     */
    public static String chunks(Language language, int count) {
        if (language == null || language == Language.ENGLISH) {
            return count + " chunks";
        }
        int lastTwo = count % 100;
        int last = count % 10;
        String word;
        if (lastTwo >= 11 && lastTwo <= 14) {
            word = "чанков";
        } else if (last == 1) {
            word = "чанк";
        } else if (last >= 2 && last <= 4) {
            word = "чанка";
        } else {
            word = "чанков";
        }
        return count + " " + word;
    }
}
