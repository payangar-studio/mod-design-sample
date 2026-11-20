package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** A box ticked when on, followed by its text. A click on the text ticks it too. */
public class CheckboxWidget extends Widget {
    public static final int BOX = 12;

    private final Binding<Boolean> value;

    public CheckboxWidget(int width, Component message, Binding<Boolean> value) {
        super(width, Theme.WIDGET_HEIGHT, message);
        this.value = value;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY() + (getHeight() - BOX) / 2;
        boolean on = value.get();
        Draw.fill(g, x, y, BOX, BOX, on ? accent() : face());
        Draw.bevel(g, x + 1, y + 1, BOX - 2, BOX - 2, Theme.BEVEL_DARK, Theme.BEVEL_LIGHT);
        Draw.outline(g, x, y, BOX, BOX, border());
        if (on) {
            int mark = active ? Theme.KNOB : Theme.KNOB_DISABLED;
            // A tick: a short stroke down to the right, then a long one up to the right.
            for (int i = 0; i < 3; i++) {
                Draw.fill(g, x + 3 + i, y + 5 + i, 2, 2, mark);
            }
            for (int i = 0; i < 4; i++) {
                Draw.fill(g, x + 6 + i, y + 6 - i, 2, 2, mark);
            }
        }
        Draw.text(g, font(), getMessage(), x + BOX + 5, textY(), getWidth() - BOX - 5, textColour());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        value.set(!value.get());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pressedBy(event)) {
            value.set(!value.get());
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        narrateValue(output, CommonComponents.optionStatus(value.get()));
    }
}
