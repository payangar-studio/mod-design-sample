package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A colour shown as a patch and its hexadecimal value; a click opens a {@link ColourPicker} above
 * the other widgets, through the screen's {@link PopupHost}.
 */
public class ColourSwatch extends Widget {
    private final Binding<Integer> value;
    private final boolean alpha;
    private boolean open;

    public ColourSwatch(int width, Component message, Binding<Integer> value, boolean alpha) {
        super(width, com.payangar.designsample.vendor.gui.Theme.WIDGET_HEIGHT, message);
        this.value = value;
        this.alpha = alpha;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Draw.box(g, getX(), getY(), getWidth(), getHeight(), face(), open ? accent() : border());
        int patch = getHeight() - 4;
        ColourPicker.checkerboard(g, getX() + 2, getY() + 2, patch * 2, patch);
        Draw.fill(g, getX() + 2, getY() + 2, patch * 2, patch, active ? value.get() : value.get() & 0x66FFFFFF);
        Component text = Component.literal(ColourPicker.format(value.get(), alpha));
        Draw.text(g, font(), text, getX() + patch * 2 + 7, textY(), getWidth() - patch * 2 - 9, textColour());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        openPicker();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pressedBy(event)) {
            openPicker();
            return true;
        }
        return false;
    }

    /** Opens the picker under the swatch, or above it when the screen ends first. */
    public void openPicker() {
        PopupHost.current().ifPresent(host -> {
            ColourPicker picker = new ColourPicker(getMessage(), value, alpha);
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int x = Math.max(0, Math.min(getRight() - picker.getWidth(), screenWidth - picker.getWidth()));
            int below = getBottom() + 1;
            int y = below + picker.getHeight() <= screenHeight ? below : getY() - 1 - picker.getHeight();
            picker.setPosition(x, y);
            open = true;
            host.openPopup(picker, () -> open = false);
        });
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        narrateValue(output, Component.literal(ColourPicker.format(value.get(), alpha)));
    }
}
