package net.vanillasettings.lang;

/**
 * Languages the settings screen can be shown in. The order here is the order the
 * "Language" button cycles through.
 */
public enum Language {
    ENGLISH("English"),
    RUSSIAN("Russian"),
    MIXED_RUSSIAN("Mixed Russian");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    /** Name shown on the language button, e.g. "Mixed Russian". Always written in English. */
    public String displayName() {
        return displayName;
    }

    /** The language after this one; wraps around to the first. */
    public Language next() {
        Language[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
