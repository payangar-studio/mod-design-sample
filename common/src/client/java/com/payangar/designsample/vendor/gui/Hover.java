package com.payangar.designsample.vendor.gui;

import net.minecraft.util.Util;

/**
 * The hover animation of a widget: a progress from 0 (at rest) to 1 (under the mouse) that moves
 * over {@link Theme#HOVER_MILLIS}, eased. It settles once the state stops changing, so a still
 * screen always draws the same.
 */
public final class Hover {
    private float progress;
    private long last = -1;

    /** Moves the animation towards {@code hovered}, and returns the eased progress. */
    public float update(boolean hovered) {
        long now = Util.getMillis();
        float step = last < 0 ? 0 : (now - last) / (float) Theme.HOVER_MILLIS;
        last = now;
        progress = Math.clamp(progress + (hovered ? step : -step), 0f, 1f);
        return value();
    }

    /** The eased progress, without moving it. */
    public float value() {
        return progress * progress * (3 - 2 * progress);
    }
}
