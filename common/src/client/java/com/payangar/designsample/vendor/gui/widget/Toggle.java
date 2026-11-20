package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.gui.Draw;
import com.payangar.designsample.vendor.gui.Hover;
import com.payangar.designsample.vendor.gui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** An on/off switch: a track filled with the accent when on, and a knob that slides. */
public class Toggle extends Widget {
    public static final int WIDTH = 28;
    public static final int HEIGHT = 14;

    private final Binding<Boolean> value;
    private final Hover slide = new Hover();

    public Toggle(Component message, Binding<Boolean> value) {
        super(WIDTH, HEIGHT, message);
        this.value = value;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        float on = slide.update(value.get());
        int x = getX();
        int y = getY() + (getHeight() - HEIGHT) / 2;
        Draw.fill(g, x, y, WIDTH, HEIGHT, Draw.lerp(on, face(), accent()));
        Draw.bevel(g, x + 1, y + 1, WIDTH - 2, HEIGHT - 2, Theme.BEVEL_DARK, Theme.BEVEL_LIGHT);
        Draw.outline(g, x, y, WIDTH, HEIGHT, border());
        int knob = HEIGHT - 4;
        int knobX = x + 2 + Math.round((WIDTH - 4 - knob) * on);
        Draw.box(g, knobX, y + 2, knob, knob, active ? Theme.KNOB : Theme.KNOB_DISABLED, Theme.BEVEL_DARK);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        value.set(!value.get());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pressedBy(event)) {
            value.set(!value.get());
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        narrateValue(output, CommonComponents.optionStatus(value.get()));
    }
}
