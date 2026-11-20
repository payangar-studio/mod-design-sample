package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Hover;
import com.payangar.designsample.vendor.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The base of the kit's widgets: a vanilla {@link AbstractWidget} (focus, keyboard navigation,
 * narration) drawn in code with the {@link Theme}, with a hover animation. A disabled widget
 * ({@code active = false}) is drawn dimmed and takes no input.
 *
 * <p>A widget is created at the origin with its size; its container or screen places it.
 */
public abstract class Widget extends AbstractWidget {
    private final Hover hover = new Hover();
    private Component hint;

    protected Widget(int width, int height, Component message) {
        super(0, 0, width, height, message);
    }

    /** The text shown next to the mouse when it rests on the widget, or {@code null}. */
    public Component hint() {
        return hint;
    }

    public Widget setHint(Component hint) {
        this.hint = hint;
        return this;
    }

    @Override
    protected final void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hover.update(highlighted());
        draw(g, mouseX, mouseY, partialTick);
        if (hint != null && isHovered()) {
            PopupHost.current().ifPresent(host -> host.showTooltip(this, hint));
        }
    }

    /** Draws the widget at its place; {@link #hover()} gives the animation's progress. */
    protected abstract void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick);

    /**
     * Whether the widget is highlighted: under the mouse, or focused by the keyboard. A widget
     * clicked keeps the focus without staying highlighted once the mouse leaves.
     */
    protected boolean highlighted() {
        return isActive()
                && (isHovered() || isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard());
    }

    /** The hover animation's progress, from 0 at rest to 1 highlighted. */
    protected final float hover() {
        return hover.value();
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    /** The face of the widget's box in its state. */
    protected int face() {
        return active ? Draw.lerp(hover(), Theme.SURFACE, Theme.SURFACE_HOVER) : Theme.SURFACE_DISABLED;
    }

    /** The border of the widget's box in its state. */
    protected int border() {
        return active ? Draw.lerp(hover(), Theme.BORDER, Theme.BORDER_HOVER) : Theme.BORDER_DISABLED;
    }

    /** The colour of the widget's text in its state. */
    protected int textColour() {
        return active ? Theme.TEXT : Theme.TEXT_DISABLED;
    }

    /** The colour of what is on or filled, in the widget's state. */
    protected int accent() {
        return active ? Draw.lerp(hover(), Theme.ACCENT, Theme.ACCENT_HOVER) : Theme.ACCENT_DISABLED;
    }

    /** The y of a line of text centred in the widget. */
    protected int textY() {
        return getY() + (getHeight() - font().lineHeight) / 2 + 1;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    /** Narrates the widget as its name and {@code value}, with the usage hint when active. */
    protected void narrateValue(NarrationElementOutput output, Component value) {
        output.add(NarratedElementType.TITLE, CommonComponents.optionNameValue(getMessage(), value));
        if (active) {
            output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
        }
    }

    /**
     * Whether {@code event} presses the widget (Enter, Space): if so, plays the click sound. Use
     * it from {@code keyPressed}.
     */
    protected boolean pressedBy(KeyEvent event) {
        if (isActive() && event.isSelection()) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        return false;
    }
}
