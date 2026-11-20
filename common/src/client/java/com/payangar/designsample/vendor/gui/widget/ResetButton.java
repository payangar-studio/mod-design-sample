package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Theme;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The button that puts a value back to its default. It is enabled only while the value differs
 * from the default, and draws its text in {@link Theme#RESET} then.
 */
public class ResetButton extends ButtonWidget {
    private final Supplier<Boolean> canReset;
    private boolean enabled = true;

    /**
     * @param binding the value
     * @param defaultValue what a press sets it to
     */
    public <T> ResetButton(Component message, Binding<T> binding, T defaultValue) {
        super(
                Minecraft.getInstance().font.width(message) + 10,
                message,
                button -> binding.set(defaultValue));
        this.canReset = () -> !Objects.equals(binding.get(), defaultValue);
        setHeight(Theme.WIDGET_HEIGHT);
    }

    /** Whether the button may be used at all: false for a read-only value. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        active = enabled && canReset.get();
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected int labelColour() {
        return Theme.RESET;
    }
}
