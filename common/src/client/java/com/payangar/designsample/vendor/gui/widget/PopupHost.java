package com.payangar.designsample.vendor.gui.widget;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * A screen that shows a popup above its widgets (an open dropdown, a colour picker) and the
 * kit's tooltips. {@code ConfigScreen} is one; a widget used on another screen opens no popup.
 */
public interface PopupHost {
    /**
     * Shows {@code popup} above everything until a click outside it, Escape, or {@link
     * #closePopup}; {@code onClose} then runs. The popup takes the input first.
     */
    void openPopup(AbstractWidget popup, Runnable onClose);

    void closePopup();

    /** Shows {@code text} next to the mouse once it has rested on {@code owner} long enough. */
    void showTooltip(Object owner, Component text);

    /** The current screen, if it is a host. */
    static Optional<PopupHost> current() {
        return Minecraft.getInstance().gui.screen() instanceof PopupHost host ? Optional.of(host) : Optional.empty();
    }
}
