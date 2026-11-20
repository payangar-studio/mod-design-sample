package com.payangar.designsample.vendor.gui.screen;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import com.payangar.designsample.vendor.gui.widget.ButtonWidget;
import com.payangar.designsample.vendor.gui.widget.PopupHost;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * The base of a config screen, generated or written by a mod: a vanilla {@link Screen} with the
 * kit's opaque background, a header with the title, a footer with a Done button, popups above the
 * widgets and the kit's tooltips. A custom screen (with a live preview, its own layout) extends it
 * and places the kit's widgets in {@link #initContent}:
 *
 * <pre>{@code
 * public class MyScreen extends ConfigScreen {
 *     MyScreen(Screen parent) {
 *         super(Component.translatable("mymod.config.title"), parent);
 *     }
 *
 *     protected void initContent(int x, int y, int width, int height) {
 *         addRenderableWidget(new Toggle(label, Binding.of(values, MyConfig.SHOW_HUD))).setPosition(x, y);
 *     }
 *
 *     protected void save() {
 *         // write the settings
 *     }
 * }
 * }</pre>
 *
 * Closing the screen (Done, Escape, or the game moving on) calls {@link #save}.
 */
public abstract class ConfigScreen extends Screen implements PopupHost {
    /** The height of the band holding the title. */
    public static final int HEADER = 28;
    /** The height of the band holding the Done button. */
    public static final int FOOTER = 32;

    protected final Screen parent;
    private AbstractWidget popup;
    private Runnable onPopupClose;
    private boolean popupDragging;
    private Object tooltipOwner;
    private Component tooltip;
    private long tooltipSince;
    private boolean tooltipAsked;

    protected ConfigScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected final void init() {
        closePopup();
        initContent(
                Theme.PADDING,
                HEADER + Theme.PADDING,
                width - 2 * Theme.PADDING,
                height - HEADER - FOOTER - 2 * Theme.PADDING);
        ButtonWidget done = addRenderableWidget(new ButtonWidget(150, CommonComponents.GUI_DONE, button -> onClose()));
        done.setPosition((width - done.getWidth()) / 2, height - FOOTER + (FOOTER - done.getHeight()) / 2);
    }

    /** Adds the screen's widgets in the area between the header and the footer. */
    protected abstract void initContent(int x, int y, int width, int height);

    /** Keeps the settings: called once when the screen closes. Nothing by default. */
    protected void save() {}

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        closePopup();
        save();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Draw.fill(g, 0, 0, width, height, Theme.BACKGROUND);
        Draw.fill(g, 0, 0, width, HEADER, Theme.BAND);
        Draw.fill(g, 0, HEADER, width, 1, Theme.SEPARATOR);
        Draw.fill(g, 0, height - FOOTER, width, FOOTER, Theme.BAND);
        Draw.fill(g, 0, height - FOOTER - 1, width, 1, Theme.SEPARATOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        tooltipAsked = false;
        Draw.centred(g, font, title, width / 2, (HEADER - font.lineHeight) / 2 + 1, Theme.HEADING);
        // Under an open popup, the widgets see the mouse nowhere: none is hovered through it.
        boolean overPopup = popup != null && popup.isMouseOver(mouseX, mouseY);
        super.extractRenderState(g, overPopup ? -1 : mouseX, overPopup ? -1 : mouseY, partialTick);
        if (popup != null) {
            g.nextStratum();
            popup.extractRenderState(g, mouseX, mouseY, partialTick);
        }
        if (!tooltipAsked) {
            tooltipOwner = null;
        } else if (popup == null && Util.getMillis() - tooltipSince >= Theme.TOOLTIP_DELAY_MILLIS) {
            g.nextStratum();
            Draw.tooltip(g, font, font.split(tooltip, Math.max(100, width / 3)), mouseX, mouseY, width, height);
        }
    }

    @Override
    public void showTooltip(Object owner, Component text) {
        if (owner != tooltipOwner) {
            tooltipOwner = owner;
            tooltipSince = Util.getMillis();
        }
        tooltip = text;
        tooltipAsked = true;
    }

    @Override
    public void openPopup(AbstractWidget popup, Runnable onClose) {
        closePopup();
        this.popup = popup;
        this.onPopupClose = onClose;
    }

    @Override
    public void closePopup() {
        if (popup == null) {
            return;
        }
        Runnable onClose = onPopupClose;
        popup.setFocused(false);
        popup = null;
        onPopupClose = null;
        popupDragging = false;
        onClose.run();
    }

    /** The open popup, or {@code null}. */
    public AbstractWidget popup() {
        return popup;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (popup == null) {
            return super.mouseClicked(event, doubleClick);
        }
        if (popup.isMouseOver(event.x(), event.y())) {
            popupDragging = true;
            popup.mouseClicked(event, doubleClick);
        } else {
            closePopup();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (popup == null) {
            return super.mouseReleased(event);
        }
        if (popupDragging) {
            popupDragging = false;
            popup.mouseReleased(event);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (popup == null) {
            return super.mouseDragged(event, dx, dy);
        }
        if (popupDragging) {
            popup.mouseDragged(event, dx, dy);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (popup == null) {
            return super.mouseScrolled(x, y, scrollX, scrollY);
        }
        if (popup.isMouseOver(x, y)) {
            popup.mouseScrolled(x, y, scrollX, scrollY);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (popup == null) {
            return super.keyPressed(event);
        }
        if (event.isEscape()) {
            closePopup();
            return true;
        }
        popup.keyPressed(event);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (popup == null) {
            return super.charTyped(event);
        }
        popup.charTyped(event);
        return true;
    }
}
