package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A row of tab titles; the chosen one is underlined with the accent. The value is its index. */
public class Tabs extends Widget {
    private static final int MARGIN = 8;

    private final List<Component> titles;
    private final Binding<Integer> selected;
    private int hovered = -1;

    public Tabs(Component message, List<Component> titles, Binding<Integer> selected) {
        super(width(titles), Theme.ROW_HEIGHT, message);
        if (titles.isEmpty()) {
            throw new IllegalArgumentException("tabs need a title");
        }
        this.titles = List.copyOf(titles);
        this.selected = selected;
    }

    private static int width(List<Component> titles) {
        int width = 0;
        for (Component title : titles) {
            width += font().width(title) + 2 * MARGIN;
        }
        return width;
    }

    private int left(int index) {
        int x = getX();
        for (int i = 0; i < index; i++) {
            x += font().width(titles.get(i)) + 2 * MARGIN;
        }
        return x;
    }

    private int indexAt(double mouseX) {
        for (int i = titles.size() - 1; i >= 0; i--) {
            if (mouseX >= left(i)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hovered = isHovered() ? indexAt(mouseX) : -1;
        int chosen = selected.get();
        Draw.fill(g, getX(), getBottom() - 1, getWidth(), 1, Theme.SEPARATOR);
        for (int i = 0; i < titles.size(); i++) {
            int x = left(i);
            int w = font().width(titles.get(i)) + 2 * MARGIN;
            if (i == hovered && i != chosen) {
                Draw.fill(g, x, getY(), w, getHeight() - 1, Theme.ROW_HOVER);
            }
            if (i == chosen) {
                Draw.fill(g, x, getBottom() - 2, w, 2, active ? Theme.ACCENT : Theme.ACCENT_DISABLED);
            }
            int colour = !active ? Theme.TEXT_DISABLED : i == chosen ? Theme.HEADING : Theme.TEXT_DIM;
            Draw.text(g, font(), titles.get(i), x + MARGIN, textY() - 1, colour);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        selected.set(indexAt(event.x()));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isActive() || !(event.isLeft() || event.isRight())) {
            return false;
        }
        selected.set(Math.clamp(selected.get() + (event.isLeft() ? -1 : 1), 0, titles.size() - 1));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        narrateValue(output, titles.get(Math.clamp(selected.get(), 0, titles.size() - 1)));
    }
}
