package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The chosen value in a box; a click opens the list of choices above the other widgets, through
 * the screen's {@link PopupHost}.
 *
 * @param <T> the type of a choice
 */
public class Dropdown<T> extends Widget {
    /** The height of a choice in the open list. */
    public static final int ITEM_HEIGHT = 14;
    /** The choices the open list shows at once; it scrolls for more. */
    public static final int VISIBLE_ITEMS = 8;

    private final List<T> choices;
    private final Function<T, Component> label;
    private final Binding<T> value;
    private boolean open;

    public Dropdown(int width, Component message, List<T> choices, Function<T, Component> label, Binding<T> value) {
        super(width, Theme.WIDGET_HEIGHT, message);
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("a dropdown needs a choice");
        }
        this.choices = List.copyOf(choices);
        this.label = label;
        this.value = value;
    }

    /** Whether the list is open. */
    public boolean isOpen() {
        return open;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Draw.box(g, getX(), getY(), getWidth(), getHeight(), face(), open ? accent() : border());
        Draw.text(g, font(), label.apply(value.get()), getX() + 5, textY(), getWidth() - 20, textColour());
        int middle = getY() + getHeight() / 2 - 1;
        for (int i = 0; i < 4; i++) {
            // A triangle pointing down, or up while open.
            int row = open ? middle + 2 - i : middle - 1 + i;
            Draw.fill(g, getRight() - 12 + i, row, 7 - 2 * i, 1, textColour());
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        openList();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pressedBy(event)) {
            openList();
            return true;
        }
        return false;
    }

    /** Opens the list of choices, when the screen can show it. */
    public void openList() {
        PopupHost.current().ifPresent(host -> {
            open = true;
            host.openPopup(new ChoiceList(host), () -> open = false);
        });
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        narrateValue(output, label.apply(value.get()));
    }

    /** The open list, under the dropdown, or above it when the screen ends first. */
    private final class ChoiceList extends Widget {
        private final PopupHost host;
        private int first;
        private int highlighted;
        /** Where the mouse was at the last frame: only a move highlights the choice under it. */
        private int lastMouseX = Integer.MIN_VALUE;
        private int lastMouseY = Integer.MIN_VALUE;

        ChoiceList(PopupHost host) {
            super(Dropdown.this.getWidth(), Math.min(choices.size(), VISIBLE_ITEMS) * ITEM_HEIGHT + 2, Dropdown.this.getMessage());
            this.host = host;
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int below = Dropdown.this.getBottom() + 1;
            setPosition(Dropdown.this.getX(), below + getHeight() <= screenHeight ? below : Dropdown.this.getY() - 1 - getHeight());
            highlighted = Math.max(0, choices.indexOf(value.get()));
            first = Math.clamp(highlighted - VISIBLE_ITEMS / 2, 0, Math.max(0, choices.size() - VISIBLE_ITEMS));
        }

        private int indexAt(double mouseY) {
            return first + (int) ((mouseY - getY() - 1) / ITEM_HEIGHT);
        }

        @Override
        protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            if (isHovered() && (mouseX != lastMouseX || mouseY != lastMouseY)) {
                highlighted = Math.clamp(indexAt(mouseY), 0, choices.size() - 1);
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            Draw.fill(g, getX(), getY(), getWidth(), getHeight(), Theme.SURFACE);
            Draw.outline(g, getX(), getY(), getWidth(), getHeight(), Theme.ACCENT);
            int selected = choices.indexOf(value.get());
            int shown = Math.min(choices.size() - first, VISIBLE_ITEMS);
            for (int i = 0; i < shown; i++) {
                int index = first + i;
                int y = getY() + 1 + i * ITEM_HEIGHT;
                if (index == highlighted) {
                    Draw.fill(g, getX() + 1, y, getWidth() - 2, ITEM_HEIGHT, Theme.SURFACE_HOVER);
                }
                if (index == selected) {
                    Draw.fill(g, getX() + 1, y, 2, ITEM_HEIGHT, Theme.ACCENT);
                }
                int colour = index == selected ? Theme.HEADING : Theme.TEXT;
                Draw.text(g, font(), label.apply(choices.get(index)), getX() + 6, y + (ITEM_HEIGHT - font().lineHeight) / 2 + 1, getWidth() - 10, colour);
            }
            if (choices.size() > VISIBLE_ITEMS) {
                int track = getHeight() - 2;
                int thumb = Math.max(6, track * VISIBLE_ITEMS / choices.size());
                int thumbY = (track - thumb) * first / (choices.size() - VISIBLE_ITEMS);
                Draw.fill(g, getRight() - 3, getY() + 1 + thumbY, 2, thumb, Theme.BORDER_HOVER);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            int index = indexAt(event.y());
            if (index >= 0 && index < choices.size()) {
                choose(index);
            }
        }

        @Override
        public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
            first = Math.clamp(first - (int) Math.signum(scrollY), 0, Math.max(0, choices.size() - VISIBLE_ITEMS));
            highlighted = Math.clamp(indexAt(y), 0, choices.size() - 1);
            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isUp() || event.isDown()) {
                highlighted = Math.clamp(highlighted + (event.isUp() ? -1 : 1), 0, choices.size() - 1);
                first = Math.clamp(first, highlighted - VISIBLE_ITEMS + 1, highlighted);
                return true;
            }
            if (event.isConfirmation()) {
                choose(highlighted);
                return true;
            }
            return false;
        }

        private void choose(int index) {
            value.set(choices.get(index));
            host.closePopup();
        }
    }
}
