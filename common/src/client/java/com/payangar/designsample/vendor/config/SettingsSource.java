package com.payangar.designsample.vendor.config;

import java.io.IOException;
import java.util.Objects;

/**
 * The settings of one scope as a config screen sees them: their current values, whether the
 * player may change them, and how a change is kept.
 */
public interface SettingsSource {
    /** The values in use. A screen changes them in place, so the game sees a change at once. */
    ConfigValues values();

    /** Whether the player may change them; a screen shows them read-only otherwise. */
    boolean editable();

    /** Keeps the values after a screen changed them. */
    void save() throws IOException;

    /** Values shown and never changed. */
    static SettingsSource readOnly(ConfigValues values) {
        Objects.requireNonNull(values, "values");
        return new SettingsSource() {
            @Override
            public ConfigValues values() {
                return values;
            }

            @Override
            public boolean editable() {
                return false;
            }

            @Override
            public void save() {}
        };
    }
}
