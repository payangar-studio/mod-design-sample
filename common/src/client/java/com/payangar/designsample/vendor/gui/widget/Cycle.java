package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A choice shown one at a time between two arrows: a click moves to the next one, a click with
 * Shift (or on the left arrow) to the previous one.
 *
 * @param <T> the type of a choice
 */
public class Cycle<T> extends Widget {
    private final List<T> choices;
    private final Function<T, Component> label;
    private final Binding<T> value;

    public Cycle(int width, Component message, List<T> choices, Function<T, Component> label, Binding<T> value) {
        super(width, com.payangar.designsample.vendor.gui.Theme.WIDGET_HEIGHT, message);
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("a cycle needs a choice");
        }
        this.choices = List.copyOf(choices);
        this.label = label;
        this.value = value;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Draw.box(g, getX(), getY(), getWidth(), getHeight(), face(), border());
        int arrow = textColour();
        int middle = getY() + getHeight() / 2;
        for (int i = 0; i < 4; i++) {
            // A left-pointing arrow on the left, a right-pointing one on the right.
            Draw.fill(g, getX() + 5 + i, middle - i, 1, 2 * i + 1, arrow);
            Draw.fill(g, getRight() - 6 - i, middle - i, 1, 2 * i + 1, arrow);
        }
        var text = Draw.fit(font(), label.apply(value.get()), getWidth() - 24);
        g.text(font(), text, getX() + (getWidth() - font().width(text)) / 2, textY(), textColour(), false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        move(event.hasShiftDown() || event.x() < getX() + 12 ? -1 : 1);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isActive()) {
            return false;
        }
        if (event.isLeft() || event.isRight()) {
            move(event.isLeft() ? -1 : 1);
            return true;
        }
        if (pressedBy(event)) {
            move(event.hasShiftDown() ? -1 : 1);
            return true;
        }
        return false;
    }

    private void move(int by) {
        int index = Math.max(0, choices.indexOf(value.get()));
        value.set(choices.get(Math.floorMod(index + by, choices.size())));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        narrateValue(output, label.apply(value.get()));
    }
}
