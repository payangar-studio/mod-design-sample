package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Hover;
import com.payangar.designsample.vendor.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A push button drawn by the kit. It is a vanilla {@link Button}, so that tools which press
 * buttons by their text (Fabric's {@code clickScreenButton}) find it.
 */
public class ButtonWidget extends Button {
    private final Hover hover = new Hover();

    public ButtonWidget(int width, Component message, OnPress onPress) {
        super(0, 0, width, Theme.ROW_HEIGHT, message, onPress, DEFAULT_NARRATION);
    }

    /** A button as wide as its text, with a margin. */
    public static ButtonWidget fitting(Component message, OnPress onPress) {
        return new ButtonWidget(Minecraft.getInstance().font.width(message) + 16, message, onPress);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        float t = hover.update(isActive()
                && (isHovered() || isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard()));
        int face = active ? Draw.lerp(t, Theme.SURFACE, Theme.SURFACE_HOVER) : Theme.SURFACE_DISABLED;
        int border = active ? Draw.lerp(t, Theme.BORDER, Theme.ACCENT) : Theme.BORDER_DISABLED;
        Draw.box(g, getX(), getY(), getWidth(), getHeight(), face, border);
        drawLabel(g, active ? labelColour() : Theme.TEXT_DISABLED);
        handleCursor(g);
    }

    /** The colour of the text of an active button. */
    protected int labelColour() {
        return Theme.TEXT;
    }

    /** Draws the button's text centred, in {@code colour} whether active or not. */
    protected void drawLabel(GuiGraphicsExtractor g, int colour) {
        var font = Minecraft.getInstance().font;
        var text = Draw.fit(font, message, getWidth() - 6);
        int x = getX() + (getWidth() - font.width(text)) / 2;
        g.text(font, text, x, getY() + (getHeight() - font.lineHeight) / 2 + 1, colour, false);
    }
}
