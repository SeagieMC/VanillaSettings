package net.vanillasettings.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Small bottom-center notification.
 */
public final class AlertToast {

    private static final long VISIBLE_MS = 1400L;
    private static final long FADE_MS = 450L;

    private static String message = null;
    private static long shownAt = 0L;

    private AlertToast() {
    }

    public static void show(String rawMessage) {
        message = rawMessage
                .replace("&f", "")
                .replace("&d", "")
                .replace("&a", "");
        shownAt = System.currentTimeMillis();
    }

    public static void render(GuiGraphicsExtractor graphics, Font font,
                              int screenWidth, int screenHeight) {
        if (message == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - shownAt;
        long total = VISIBLE_MS + FADE_MS;
        if (elapsed >= total) {
            message = null;
            return;
        }

        float alpha = 1.0f;
        if (elapsed > VISIBLE_MS) {
            alpha = 1.0f - (float) (elapsed - VISIBLE_MS) / FADE_MS;
        }

        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        int color = (a << 24) | 0xFFFFFF;

        Component text = Component.literal(message);
        int textWidth = font.width(text);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 68;

        graphics.text(font, text, x, y, color, true);
    }
}
