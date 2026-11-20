package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A line of text, cut with an ellipsis when it does not fit. It takes no input. */
public class Label extends Widget {
    private int colour = Theme.TEXT;

    public Label(int width, Component text) {
        super(width, font().lineHeight + 2, text);
    }

    /** A label in the dimmed colour of secondary text. */
    public static Label dim(int width, Component text) {
        return new Label(width, text).colour(Theme.TEXT_DIM);
    }

    public Label colour(int colour) {
        this.colour = colour;
        return this;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Draw.text(g, font(), getMessage(), getX(), textY(), getWidth(), active ? colour : Theme.TEXT_DISABLED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
