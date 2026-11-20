package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A colour chooser: a saturation and brightness square, a hue bar, an opacity bar when the colour
 * has an alpha channel, a preview and the colour in hexadecimal. The value is an ARGB integer.
 */
public class ColourPicker extends Container {
    private static final int SQUARE = 64;
    private static final int BAR = 8;
    private static final int GAP = 4;
    private static final Pattern OPAQUE = Pattern.compile("#?[0-9A-Fa-f]{6}");
    private static final Pattern TRANSLUCENT = Pattern.compile("#?(?:[0-9A-Fa-f]{2})?[0-9A-Fa-f]{6}");

    private enum Area { SQUARE, HUE, ALPHA }

    private final Binding<Integer> value;
    private final boolean alpha;
    private final TextField hex;
    private float hue;
    private float saturation;
    private float brightness;
    private int known;
    private Area dragging;

    /**
     * @param alpha whether the colour has an alpha channel; without one it stays opaque
     */
    public ColourPicker(Component message, Binding<Integer> value, boolean alpha) {
        super(width(alpha), Theme.PADDING * 2 + SQUARE + GAP + Theme.WIDGET_HEIGHT, message);
        this.value = value;
        this.alpha = alpha;
        hex = add(new TextField(SQUARE, message, Binding.of(() -> format(value.get(), alpha), this::setHex))
                .validate(text -> (alpha ? TRANSLUCENT : OPAQUE).matcher(text).matches())
                .maxLength(alpha ? 9 : 7));
        sync();
        layout();
    }

    private static int width(boolean alpha) {
        return Theme.PADDING * 2 + SQUARE + GAP + BAR + (alpha ? GAP + BAR : 0);
    }

    /** {@code argb} as {@code #RRGGBB}, or {@code #AARRGGBB} with an alpha channel. */
    public static String format(int argb, boolean alpha) {
        return alpha ? String.format(Locale.ROOT, "#%08X", argb) : String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    private void setHex(String text) {
        String digits = text.startsWith("#") ? text.substring(1) : text;
        int parsed = Integer.parseUnsignedInt(digits, 16);
        value.set(digits.length() == 6 ? parsed | 0xFF000000 : parsed);
    }

    private int left() {
        return getX() + Theme.PADDING;
    }

    private int top() {
        return getY() + Theme.PADDING;
    }

    private int hueX() {
        return left() + SQUARE + GAP;
    }

    private int alphaX() {
        return hueX() + BAR + GAP;
    }

    @Override
    protected void layout() {
        if (hex != null) {
            hex.setPosition(left() + 20, top() + SQUARE + GAP);
            hex.setWidth(getWidth() - Theme.PADDING * 2 - 20);
        }
    }

    /**
     * Takes the hue, saturation and brightness from the value, when it changed elsewhere. Never
     * during a drag: the hue of a nearly grey colour, rounded to 8 bits per channel, is not the
     * hue picked.
     */
    private void sync() {
        int argb = value.get();
        if (dragging != null || argb == known) {
            return;
        }
        known = argb;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        brightness = max;
        saturation = max == 0 ? 0 : delta / max;
        if (delta > 0) {
            float h;
            if (max == r) {
                h = (g - b) / delta;
            } else if (max == g) {
                h = 2 + (b - r) / delta;
            } else {
                h = 4 + (r - g) / delta;
            }
            hue = ((h / 6) % 1 + 1) % 1;
        }
    }

    /** The opaque colour of {@code hue}, {@code saturation} and {@code brightness}, each 0 to 1. */
    public static int hsv(float hue, float saturation, float brightness) {
        float h = (hue % 1 + 1) % 1 * 6;
        int sector = (int) h;
        float f = h - sector;
        float p = brightness * (1 - saturation);
        float q = brightness * (1 - saturation * f);
        float t = brightness * (1 - saturation * (1 - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> { r = brightness; g = t; b = p; }
            case 1 -> { r = q; g = brightness; b = p; }
            case 2 -> { r = p; g = brightness; b = t; }
            case 3 -> { r = p; g = q; b = brightness; }
            case 4 -> { r = t; g = p; b = brightness; }
            default -> { r = brightness; g = p; b = q; }
        }
        return 0xFF000000 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }

    private void apply() {
        int a = alpha ? value.get() >>> 24 : 0xFF;
        known = (a << 24) | (hsv(hue, saturation, brightness) & 0xFFFFFF);
        value.set(known);
    }

    @Override
    protected void drawBackground(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        sync();
        Draw.fill(g, getX(), getY(), getWidth(), getHeight(), Theme.BAND);
        Draw.outline(g, getX(), getY(), getWidth(), getHeight(), active ? Theme.ACCENT : Theme.BORDER_DISABLED);
        int x = left();
        int y = top();
        for (int i = 0; i < SQUARE; i++) {
            int top = hsv(hue, i / (float) (SQUARE - 1), 1);
            Draw.gradient(g, x + i, y, 1, SQUARE, top, 0xFF000000);
        }
        int markX = x + Math.round(saturation * (SQUARE - 1));
        int markY = y + Math.round((1 - brightness) * (SQUARE - 1));
        Draw.outline(g, markX - 2, markY - 2, 5, 5, brightness > 0.5f ? 0xFF000000 : 0xFFFFFFFF);

        int hueX = hueX();
        for (int i = 0; i < 6; i++) {
            int y0 = y + SQUARE * i / 6;
            int y1 = y + SQUARE * (i + 1) / 6;
            Draw.gradient(g, hueX, y0, BAR, y1 - y0, hsv(i / 6f, 1, 1), hsv((i + 1) / 6f, 1, 1));
        }
        marker(g, hueX, y + Math.round(hue * (SQUARE - 1)));

        int colour = value.get();
        if (alpha) {
            int alphaX = alphaX();
            checkerboard(g, alphaX, y, BAR, SQUARE);
            Draw.gradient(g, alphaX, y, BAR, SQUARE, colour | 0xFF000000, colour & 0x00FFFFFF);
            marker(g, alphaX, y + Math.round((1 - (colour >>> 24) / 255f) * (SQUARE - 1)));
        }
        int previewY = y + SQUARE + GAP;
        checkerboard(g, x, previewY, 16, Theme.WIDGET_HEIGHT);
        Draw.fill(g, x, previewY, 16, Theme.WIDGET_HEIGHT, colour);
        Draw.outline(g, x, previewY, 16, Theme.WIDGET_HEIGHT, Theme.BORDER);
        if (!active) {
            Draw.fill(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, (Theme.SURFACE_DISABLED & 0x00FFFFFF) | 0x99000000);
        }
    }

    private static void marker(GuiGraphicsExtractor g, int x, int y) {
        Draw.fill(g, x - 1, y - 1, BAR + 2, 3, 0xFF000000);
        Draw.fill(g, x - 1, y, BAR + 2, 1, 0xFFFFFFFF);
    }

    /** Grey squares, under a colour that may be translucent. */
    static void checkerboard(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        int cell = 4;
        for (int cy = 0; cy < height; cy += cell) {
            for (int cx = 0; cx < width; cx += cell) {
                int colour = ((cx / cell + cy / cell) & 1) == 0 ? 0xFFBFBFBF : 0xFF7F7F7F;
                Draw.fill(g, x + cx, y + cy, Math.min(cell, width - cx), Math.min(cell, height - cy), colour);
            }
        }
    }

    private Area areaAt(double mouseX, double mouseY) {
        int y = top();
        if (mouseY < y || mouseY >= y + SQUARE) {
            return null;
        }
        if (mouseX >= left() && mouseX < left() + SQUARE) {
            return Area.SQUARE;
        }
        if (mouseX >= hueX() && mouseX < hueX() + BAR) {
            return Area.HUE;
        }
        if (alpha && mouseX >= alphaX() && mouseX < alphaX() + BAR) {
            return Area.ALPHA;
        }
        return null;
    }

    private void pick(double mouseX, double mouseY) {
        float fx = (float) Math.clamp((mouseX - left()) / (SQUARE - 1), 0, 1);
        float fy = (float) Math.clamp((mouseY - top()) / (SQUARE - 1), 0, 1);
        switch (dragging) {
            case SQUARE -> {
                saturation = fx;
                brightness = 1 - fy;
                apply();
            }
            case HUE -> {
                hue = Math.min(fy, 0.999f);
                apply();
            }
            case ALPHA -> {
                int a = Math.round((1 - fy) * 255);
                known = (a << 24) | (value.get() & 0xFFFFFF);
                value.set(known);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isActive() || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        Area area = event.button() == 1 ? areaAt(event.x(), event.y()) : null;
        if (area != null) {
            setFocused(null);
            dragging = area;
            pick(event.x(), event.y());
            return true;
        }
        super.mouseClicked(event, doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            pick(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            dragging = null;
        }
    }
}
