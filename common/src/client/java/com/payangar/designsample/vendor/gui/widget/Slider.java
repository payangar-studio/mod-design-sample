package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import java.util.Locale;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A number between two bounds, set by clicking or dragging along the track, or with the arrow
 * keys. The value is written right of the track.
 */
public class Slider extends Widget {
    private final double min;
    private final double max;
    private final double step;
    private final Binding<Double> value;
    private final DoubleFunction<Component> format;

    /**
     * @param step the precision of the value, and the move of an arrow key; values are rounded to
     *     it
     */
    public Slider(
            int width,
            Component message,
            double min,
            double max,
            double step,
            Binding<Double> value,
            DoubleFunction<Component> format) {
        super(width, Theme.WIDGET_HEIGHT, message);
        if (!(min < max) || !(step > 0)) {
            throw new IllegalArgumentException("a slider needs min < max and a positive step");
        }
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = value;
        this.format = format;
    }

    /** A slider over whole numbers. */
    public static Slider integer(int width, Component message, int min, int max, Binding<Integer> value) {
        Binding<Double> asDouble = Binding.of(() -> (double) value.get(), v -> value.set((int) Math.round(v)));
        return new Slider(width, message, min, max, 1, asDouble, v -> Component.literal(Long.toString(Math.round(v))));
    }

    /** A slider over decimal numbers, to the hundredth of its range. */
    public static Slider decimal(int width, Component message, double min, double max, Binding<Double> value) {
        double step = Math.pow(10, Math.floor(Math.log10((max - min) / 100)));
        int decimals = Math.max(0, (int) -Math.log10(step));
        String pattern = "%." + decimals + "f";
        return new Slider(width, message, min, max, step, value, v -> Component.literal(String.format(Locale.ROOT, pattern, v)));
    }

    /** Where the value is along the track, from 0 to 1. */
    private double position() {
        return Math.clamp((value.get() - min) / (max - min), 0, 1);
    }

    /** The width of the value written right of the track: the widest of the bounds. */
    private int valueWidth() {
        return Math.max(font().width(format.apply(min)), font().width(format.apply(max))) + 6;
    }

    /** The left of the track. */
    public int trackX() {
        return getX();
    }

    /** The width of the track; the knob's centre runs from 3 pixels in to 3 pixels before its end. */
    public int trackWidth() {
        return getWidth() - valueWidth();
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = trackX();
        int w = trackWidth();
        int h = getHeight();
        int middle = getY() + h / 2;
        // The track: a groove, filled with the accent up to the knob.
        Draw.fill(g, x, middle - 2, w, 4, active ? Theme.BACKGROUND : Theme.SURFACE_DISABLED);
        Draw.outline(g, x, middle - 2, w, 4, border());
        int knob = 6;
        int knobX = x + (int) Math.round((w - knob) * position());
        Draw.fill(g, x + 1, middle - 1, knobX - x, 2, accent());
        Draw.box(g, knobX, getY() + 2, knob, h - 4, active ? Draw.lerp(hover(), Theme.KNOB, 0xFFFFFFFF) : Theme.KNOB_DISABLED, border());
        Component text = format.apply(value.get());
        int textX = getRight() - font().width(text);
        Draw.text(g, font(), text, textX, textY(), textColour());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        setFromMouse(event.x());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        setFromMouse(event.x());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isActive()) {
            return false;
        }
        if (event.isLeft() || event.isRight()) {
            set(value.get() + (event.isLeft() ? -step : step));
            return true;
        }
        return false;
    }

    private void setFromMouse(double mouseX) {
        double t = Math.clamp((mouseX - trackX() - 3) / (trackWidth() - 6), 0, 1);
        set(min + t * (max - min));
    }

    /** Sets {@code v} rounded to the step and kept within the bounds. */
    private void set(double v) {
        double rounded = min + Math.round((v - min) / step) * step;
        // Rounds away the binary noise of the multiplication (0.30000000000000004).
        rounded = Double.parseDouble(String.format(Locale.ROOT, "%.10f", rounded));
        value.set(Math.clamp(rounded, min, max));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        narrateValue(output, format.apply(value.get()));
    }
}
