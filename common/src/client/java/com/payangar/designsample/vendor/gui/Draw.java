package com.payangar.designsample.vendor.gui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The drawing layer of the kit, over the game's GUI drawing class ({@link GuiGraphicsExtractor}
 * in 26.x, {@code GuiGraphics} before): rectangles given as position and size, never textures.
 */
public final class Draw {
    private Draw() {}

    /** A filled rectangle. */
    public static void fill(GuiGraphicsExtractor g, int x, int y, int width, int height, int colour) {
        if (width > 0 && height > 0) {
            g.fill(x, y, x + width, y + height, colour);
        }
    }

    /** A rectangle shaded from {@code top} to {@code bottom}. */
    public static void gradient(GuiGraphicsExtractor g, int x, int y, int width, int height, int top, int bottom) {
        if (width > 0 && height > 0) {
            g.fillGradient(x, y, x + width, y + height, top, bottom);
        }
    }

    /** A rectangle shaded from {@code left} to {@code right}, one column at a time. */
    public static void gradientHorizontal(
            GuiGraphicsExtractor g, int x, int y, int width, int height, int left, int right) {
        for (int i = 0; i < width; i++) {
            float t = width == 1 ? 0 : i / (float) (width - 1);
            g.fill(x + i, y, x + i + 1, y + height, lerp(t, left, right));
        }
    }

    /** The one-pixel border of a rectangle. */
    public static void outline(GuiGraphicsExtractor g, int x, int y, int width, int height, int colour) {
        if (width > 0 && height > 0) {
            g.outline(x, y, width, height, colour);
        }
    }

    /**
     * The edges of a raised rectangle, inside its border: {@code light} on the top and left,
     * {@code dark} on the bottom and right. Swapped, it looks sunken.
     */
    public static void bevel(GuiGraphicsExtractor g, int x, int y, int width, int height, int light, int dark) {
        if (width < 2 || height < 2) {
            return;
        }
        g.fill(x, y, x + width - 1, y + 1, light);
        g.fill(x, y + 1, x + 1, y + height - 1, light);
        g.fill(x + 1, y + height - 1, x + width, y + height, dark);
        g.fill(x + width - 1, y, x + width, y + height - 1, dark);
    }

    /** A widget's box: a face, a raised bevel inside it, and a border. */
    public static void box(GuiGraphicsExtractor g, int x, int y, int width, int height, int face, int border) {
        fill(g, x, y, width, height, face);
        bevel(g, x + 1, y + 1, width - 2, height - 2, Theme.BEVEL_LIGHT, Theme.BEVEL_DARK);
        outline(g, x, y, width, height, border);
    }

    /** Text without shadow, the kit's flat look. */
    public static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int colour) {
        g.text(font, text, x, y, colour, false);
    }

    /** Text without shadow, cut with an ellipsis to fit {@code width}. */
    public static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int colour) {
        g.text(font, fit(font, text, width), x, y, colour, false);
    }

    /** Text centred on {@code centreX}, without shadow. */
    public static void centred(GuiGraphicsExtractor g, Font font, Component text, int centreX, int y, int colour) {
        g.text(font, text, centreX - font.width(text) / 2, y, colour, false);
    }

    /** {@code text} cut with an ellipsis to fit {@code width}. */
    public static FormattedCharSequence fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String ellipsis = "...";
        String plain = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width(ellipsis)));
        return Component.literal(plain + ellipsis).withStyle(text.getStyle()).getVisualOrderText();
    }

    /**
     * A tooltip box with {@code lines}, next to the mouse and kept on the screen of {@code
     * screenWidth} by {@code screenHeight}.
     */
    public static void tooltip(
            GuiGraphicsExtractor g,
            Font font,
            List<FormattedCharSequence> lines,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight) {
        if (lines.isEmpty()) {
            return;
        }
        int width = 0;
        for (FormattedCharSequence line : lines) {
            width = Math.max(width, font.width(line));
        }
        int height = lines.size() * (font.lineHeight + 1) - 1;
        int boxWidth = width + 8;
        int boxHeight = height + 8;
        int x = mouseX + 10;
        int y = mouseY - 4;
        if (x + boxWidth > screenWidth) {
            x = Math.max(0, mouseX - 10 - boxWidth);
        }
        y = Math.max(0, Math.min(y, screenHeight - boxHeight));
        fill(g, x, y, boxWidth, boxHeight, Theme.TOOLTIP);
        outline(g, x, y, boxWidth, boxHeight, Theme.TOOLTIP_BORDER);
        int lineY = y + 4;
        for (FormattedCharSequence line : lines) {
            g.text(font, line, x + 4, lineY, Theme.TEXT, false);
            lineY += font.lineHeight + 1;
        }
    }

    /** The colour {@code t} of the way from {@code from} to {@code to}, channel by channel. */
    public static int lerp(float t, int from, int to) {
        if (t <= 0) {
            return from;
        }
        if (t >= 1) {
            return to;
        }
        int a = channel(t, from >>> 24, to >>> 24);
        int r = channel(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int gr = channel(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = channel(t, from & 0xFF, to & 0xFF);
        return a << 24 | r << 16 | gr << 8 | b;
    }

    private static int channel(float t, int from, int to) {
        return Math.round(from + (to - from) * t);
    }
}
