package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * A column of widgets taller than its bounds, scrolled by the wheel or by dragging its bar. The
 * scroll eases towards its target, so the wheel moves smoothly. Children are stacked from the top
 * with a gap, as wide as the panel minus the bar; a child that is not {@code visible} takes no
 * room.
 */
public class ScrollPanel extends Container {
    private static final int BAR = 4;
    private static final int GAP = 2;
    /** Pixels per notch of the wheel. */
    private static final int WHEEL = 24;
    /** The share of the remaining distance the scroll covers in 10 ms. */
    private static final double EASING = 0.25;

    private double scroll;
    private double target;
    private long lastFrame = -1;
    private boolean draggingBar;

    public ScrollPanel(int width, int height, Component message) {
        super(width, height, message);
    }

    /** The width of a child, the bar left aside. */
    public int innerWidth() {
        return getWidth() - BAR - Theme.PADDING;
    }

    /** The height of the visible children stacked. */
    public int contentHeight() {
        int height = 0;
        for (AbstractWidget child : children()) {
            if (child.visible) {
                height += child.getHeight() + GAP;
            }
        }
        return Math.max(0, height - GAP);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - getHeight());
    }

    public double scroll() {
        return scroll;
    }

    /** Scrolls to {@code amount} at once. */
    public void setScroll(double amount) {
        target = Math.clamp(amount, 0, maxScroll());
        scroll = target;
        layout();
    }

    @Override
    protected void layout() {
        int y = getY() - (int) Math.round(scroll);
        for (AbstractWidget child : children()) {
            if (!child.visible) {
                continue;
            }
            child.setPosition(getX(), y);
            if (child.getWidth() != innerWidth() && child instanceof Container) {
                child.setWidth(innerWidth());
            }
            y += child.getHeight() + GAP;
        }
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        ease();
        g.enableScissor(getX(), getY(), getRight(), getBottom());
        for (AbstractWidget child : children()) {
            if (child.visible && child.getBottom() >= getY() && child.getY() <= getBottom()) {
                child.extractRenderState(g, mouseX, mouseY, partialTick);
            }
        }
        g.disableScissor();
        int max = maxScroll();
        if (max > 0) {
            int track = getHeight();
            int thumb = Math.max(16, track * track / (track + max));
            int thumbY = getY() + (int) Math.round((track - thumb) * scroll / max);
            int barX = getRight() - BAR;
            Draw.fill(g, barX, getY(), BAR, track, Theme.SURFACE_DISABLED);
            boolean hot = draggingBar || isHovered() && mouseX >= barX;
            Draw.fill(g, barX, thumbY, BAR, thumb, hot ? Theme.ACCENT_HOVER : Theme.BORDER_HOVER);
        }
    }

    /** Moves the scroll towards its target, by the time since the last frame. */
    private void ease() {
        long now = Util.getMillis();
        double frames = lastFrame < 0 ? 1 : (now - lastFrame) / 10.0;
        lastFrame = now;
        target = Math.clamp(target, 0, maxScroll());
        if (Math.abs(target - scroll) < 0.5) {
            scroll = target;
        } else {
            scroll += (target - scroll) * (1 - Math.pow(1 - EASING, frames));
        }
        layout();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!isMouseOver(x, y)) {
            return false;
        }
        if (super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }
        target = Math.clamp(target - scrollY * WHEEL, 0, maxScroll());
        return maxScroll() > 0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) {
            return false;
        }
        if (maxScroll() > 0 && event.x() >= getRight() - BAR) {
            draggingBar = true;
            dragBar(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingBar) {
            dragBar(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingBar) {
            draggingBar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void dragBar(double mouseY) {
        double t = Math.clamp((mouseY - getY()) / getHeight(), 0, 1);
        setScroll(t * maxScroll());
    }

    /** Focusing a child, by the keyboard for instance, scrolls it into view. */
    @Override
    public void setFocused(GuiEventListener child) {
        super.setFocused(child);
        if (child instanceof AbstractWidget widget) {
            double top = widget.getY() - getY() + scroll;
            if (top < target) {
                target = top;
            } else if (top + widget.getHeight() > target + getHeight()) {
                target = top + widget.getHeight() - getHeight();
            }
        }
    }
}
