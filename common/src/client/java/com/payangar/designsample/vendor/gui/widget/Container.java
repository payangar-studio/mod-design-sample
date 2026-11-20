package com.payangar.designsample.vendor.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

/**
 * A widget holding other widgets: it places them ({@link #layout}), draws them over its own
 * background, and passes them the input like a screen does. Its children are visited as the
 * screen's widgets, so a game test finds a button inside it.
 */
public abstract class Container extends Widget implements ContainerEventHandler {
    private final List<AbstractWidget> children = new ArrayList<>();
    private GuiEventListener focused;
    private boolean dragging;

    protected Container(int width, int height, Component message) {
        super(width, height, message);
    }

    /** Adds {@code child}, placed at the next {@link #layout}. */
    public <T extends AbstractWidget> T add(T child) {
        children.add(child);
        return child;
    }

    public void clear() {
        setFocused(null);
        children.clear();
    }

    /** Places the children inside the container's bounds. Called when they move. */
    protected abstract void layout();

    @Override
    public void setX(int x) {
        super.setX(x);
        layout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        layout();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        layout();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        layout();
    }

    @Override
    public List<AbstractWidget> children() {
        return children;
    }

    /** Draws the container's background, then its visible children. */
    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        drawBackground(g, mouseX, mouseY);
        for (AbstractWidget child : children) {
            child.extractRenderState(g, mouseX, mouseY, partialTick);
        }
    }

    /** What the container draws under its children; nothing by default. */
    protected void drawBackground(GuiGraphicsExtractor g, int mouseX, int mouseY) {}

    @Override
    protected boolean highlighted() {
        return isActive() && isHovered();
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> visitor) {
        for (AbstractWidget child : children) {
            child.visitWidgets(visitor);
        }
    }

    @Override
    public GuiEventListener getFocused() {
        return focused;
    }

    @Override
    public void setFocused(GuiEventListener child) {
        if (focused != null && focused != child) {
            focused.setFocused(false);
        }
        if (child != null) {
            child.setFocused(true);
        }
        focused = child;
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public boolean isFocused() {
        return ContainerEventHandler.super.isFocused();
    }

    @Override
    public void setFocused(boolean focused) {
        ContainerEventHandler.super.setFocused(focused);
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return isActive() ? ContainerEventHandler.super.nextFocusPath(event) : null;
    }

    @Override
    public ComponentPath getCurrentFocusPath() {
        return ContainerEventHandler.super.getCurrentFocusPath();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return isMouseOver(event.x(), event.y()) && ContainerEventHandler.super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return ContainerEventHandler.super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return ContainerEventHandler.super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return isMouseOver(x, y) && ContainerEventHandler.super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double x, double y) {
        for (AbstractWidget child : children) {
            child.mouseMoved(x, y);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return ContainerEventHandler.super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return ContainerEventHandler.super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return ContainerEventHandler.super.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        return ContainerEventHandler.super.preeditUpdated(event);
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (focused instanceof NarratableEntry entry) {
            entry.updateNarration(output);
        }
    }

    @Override
    public NarrationPriority narrationPriority() {
        return focused != null ? NarrationPriority.FOCUSED : super.narrationPriority();
    }
}
