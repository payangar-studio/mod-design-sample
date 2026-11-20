package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Theme;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

/**
 * A line of text to edit, in a sunken box drawn by the kit. The editing itself (cursor, selection,
 * clipboard, input methods) is the vanilla {@link EditBox}'s, without its textured border.
 *
 * <p>With a {@link #validate validator}, text that it refuses is shown with a red border and not
 * written to the value; once the field loses the focus, it shows the value again.
 */
public class TextField extends Widget {
    private static final int INVALID = 0xFFD9534F;

    private final EditBox box;
    private final Binding<String> value;
    private Predicate<String> valid = text -> true;

    public TextField(int width, Component message, Binding<String> value) {
        super(width, Theme.WIDGET_HEIGHT, message);
        this.value = value;
        box = new EditBox(font(), 0, 0, Math.max(1, width - 8), font().lineHeight, message);
        box.setBordered(false);
        box.setMaxLength(1024);
        box.setTextColor(Theme.TEXT);
        box.setTextColorUneditable(Theme.TEXT_DISABLED);
        box.setValue(Objects.toString(value.get(), ""));
        box.setResponder(this::changed);
        place();
    }

    /** Text shown in the empty field while it is not focused. */
    public TextField hint(Component hint) {
        box.setHint(hint);
        return this;
    }

    /** Only text that {@code valid} accepts is written to the value. */
    public TextField validate(Predicate<String> valid) {
        this.valid = valid;
        return this;
    }

    public TextField maxLength(int length) {
        box.setMaxLength(length);
        return this;
    }

    /** The text in the field, valid or not. */
    public String text() {
        return box.getValue();
    }

    private void changed(String text) {
        if (valid.test(text) && !text.equals(value.get())) {
            value.set(text);
        }
    }

    private void place() {
        box.setX(getX() + 4);
        box.setY(getY() + (getHeight() - font().lineHeight) / 2 + 1);
        box.setWidth(Math.max(1, getWidth() - 8));
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        place();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        place();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        place();
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        String current = Objects.toString(value.get(), "");
        if (!box.isFocused() && !box.getValue().equals(current)) {
            // Changed elsewhere (a reset), or text refused while it was edited.
            box.setValue(current);
        }
        box.setEditable(active);
        boolean invalid = !valid.test(box.getValue());
        int face = active ? Theme.BACKGROUND : Theme.SURFACE_DISABLED;
        int border = invalid ? INVALID : box.isFocused() ? Theme.ACCENT : border();
        Draw.fill(g, getX(), getY(), getWidth(), getHeight(), face);
        Draw.bevel(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, Theme.BEVEL_DARK, Theme.BEVEL_LIGHT);
        Draw.outline(g, getX(), getY(), getWidth(), getHeight(), border);
        box.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    /** {@code event} moved inside the text, so that a click on the margin still lands. */
    private MouseButtonEvent inside(MouseButtonEvent event) {
        double x = Math.clamp(event.x(), box.getX(), box.getRight() - 1);
        double y = Math.clamp(event.y(), box.getY(), box.getBottom() - 1);
        return new MouseButtonEvent(x, y, event.buttonInfo());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isActive() || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        box.setFocused(true);
        box.mouseClicked(inside(event), doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return box.mouseDragged(inside(event), dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return box.mouseReleased(inside(event));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return box.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return box.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        return box.preeditUpdated(event);
    }

    @Override
    public boolean capturesInput() {
        return box.capturesInput();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        box.setFocused(focused);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        box.updateWidgetNarration(output);
    }
}
