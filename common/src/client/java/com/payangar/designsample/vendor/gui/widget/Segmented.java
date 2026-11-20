package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A few choices side by side, the chosen one filled with the accent. For two to four short
 * choices; a {@link Dropdown} suits more.
 *
 * @param <T> the type of a choice
 */
public class Segmented<T> extends Widget {
    private final List<T> choices;
    private final Function<T, Component> label;
    private final Binding<T> value;
    private int hovered = -1;

    public Segmented(int width, Component message, List<T> choices, Function<T, Component> label, Binding<T> value) {
        super(width, Theme.WIDGET_HEIGHT, message);
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("a segmented control needs a choice");
        }
        this.choices = List.copyOf(choices);
        this.label = label;
        this.value = value;
    }

    private int left(int index) {
        return getX() + getWidth() * index / choices.size();
    }

    private int indexAt(double mouseX) {
        for (int i = choices.size() - 1; i >= 0; i--) {
            if (mouseX >= left(i)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hovered = isHovered() && active ? indexAt(mouseX) : -1;
        int selected = choices.indexOf(value.get());
        Draw.fill(g, getX(), getY(), getWidth(), getHeight(), active ? Theme.SURFACE : Theme.SURFACE_DISABLED);
        for (int i = 0; i < choices.size(); i++) {
            int x = left(i);
            int w = left(i + 1) - x;
            if (i == selected) {
                Draw.fill(g, x + 1, getY() + 1, w - 1, getHeight() - 2, active ? Theme.ACCENT : Theme.ACCENT_DISABLED);
                Draw.bevel(g, x + 1, getY() + 1, w - 1, getHeight() - 2, Theme.BEVEL_LIGHT, Theme.BEVEL_DARK);
            } else if (i == hovered) {
                Draw.fill(g, x + 1, getY() + 1, w - 1, getHeight() - 2, Theme.SURFACE_HOVER);
            }
            if (i > 0) {
                Draw.fill(g, x, getY() + 1, 1, getHeight() - 2, border());
            }
            Component text = label.apply(choices.get(i));
            int colour = !active ? Theme.TEXT_DISABLED : i == selected ? Theme.HEADING : Theme.TEXT_DIM;
            var fitted = Draw.fit(font(), text, w - 4);
            g.text(font(), fitted, x + (w - font().width(fitted)) / 2, textY(), colour, false);
        }
        Draw.outline(g, getX(), getY(), getWidth(), getHeight(), border());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        value.set(choices.get(indexAt(event.x())));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isActive() || !(event.isLeft() || event.isRight())) {
            return false;
        }
        int index = Math.max(0, choices.indexOf(value.get())) + (event.isLeft() ? -1 : 1);
        value.set(choices.get(Math.clamp(index, 0, choices.size() - 1)));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        narrateValue(output, label.apply(value.get()));
    }
}
