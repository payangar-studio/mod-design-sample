package com.payangar.designsample.vendor.gui.screen;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Texts;
import com.payangar.designsample.vendor.gui.Theme;
import com.payangar.designsample.vendor.config.Setting;
import com.payangar.designsample.vendor.gui.widget.Binding;
import com.payangar.designsample.vendor.gui.widget.Container;
import com.payangar.designsample.vendor.gui.widget.ResetButton;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * A row of the generated screen: the setting's label, its widget and a reset button. A read-only
 * row disables both. The setting's tooltip, when the mod translates its key, shows on the row.
 */
public class SettingRow extends Container {
    /** The width of a setting's widget. */
    public static final int CONTROL_WIDTH = 140;

    private final Setting<?> setting;
    private final boolean editable;
    private final AbstractWidget control;
    private final ResetButton reset;

    public <T> SettingRow(int width, String modId, Setting<T> setting, Binding<T> binding, boolean editable) {
        super(width, Theme.ROW_HEIGHT, SettingWidgets.label(setting));
        this.setting = setting;
        this.editable = editable;
        control = add(SettingWidgets.create(CONTROL_WIDTH, setting, binding));
        control.active = editable;
        reset = add(new ResetButton(Texts.vendor(modId, "config.reset"), binding, setting.defaultValue()));
        reset.setEnabled(editable);
        setHint(Texts.ifPresent(setting.tooltipKey()));
        layout();
    }

    public Setting<?> setting() {
        return setting;
    }

    /** The setting's widget. */
    public AbstractWidget control() {
        return control;
    }

    public ResetButton reset() {
        return reset;
    }

    /** Whether the row matches a search: its label or name contains {@code query}, ignoring case. */
    public boolean matches(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        return q.isEmpty()
                || getMessage().getString().toLowerCase(Locale.ROOT).contains(q)
                || setting.name().contains(q);
    }

    @Override
    protected void layout() {
        if (reset == null) {
            return;
        }
        int right = getRight() - Theme.PADDING;
        reset.setPosition(right - reset.getWidth(), getY() + (getHeight() - reset.getHeight()) / 2);
        int controlX = reset.getX() - Theme.PADDING - CONTROL_WIDTH;
        control.setPosition(controlX, getY() + (getHeight() - control.getHeight()) / 2);
    }

    @Override
    protected void drawBackground(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovered()) {
            Draw.fill(g, getX(), getY(), getWidth(), getHeight(), Theme.ROW_HOVER);
        }
        int labelWidth = control.getX() - getX() - 2 * Theme.PADDING;
        Draw.text(g, font(), getMessage(), getX() + Theme.PADDING, textY(), labelWidth, editable ? Theme.TEXT : Theme.TEXT_DIM);
    }
}
