package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** The title of a group of widgets, underlined by a short accent bar. */
public class Heading extends Label {
    public Heading(int width, Component text) {
        super(width, text);
        setHeight(font().lineHeight + 6);
        colour(Theme.HEADING);
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Draw.text(g, font(), getMessage(), getX(), getY() + 1, getWidth(), active ? Theme.HEADING : Theme.TEXT_DISABLED);
        int bar = Math.min(getWidth(), Math.max(12, font().width(getMessage()) / 3));
        Draw.fill(g, getX(), getBottom() - 2, bar, 2, active ? Theme.ACCENT : Theme.ACCENT_DISABLED);
    }
}
