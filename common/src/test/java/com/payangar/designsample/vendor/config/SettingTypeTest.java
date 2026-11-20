package com.payangar.designsample.vendor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

class SettingTypeTest {
    @Test
    void aColourIsHexDigitsOnly() {
        SettingType.ColourType opaque = new SettingType.ColourType(false);
        SettingType.ColourType translucent = new SettingType.ColourType(true);
        assertNull(opaque.read(new JsonPrimitive("#+12345")).value());
        assertNull(translucent.read(new JsonPrimitive("#+1234567")).value());
        assertEquals(0xFFABCDEF, opaque.read(new JsonPrimitive("#abcdef")).value());
        assertEquals(0x12ABCDEF, translucent.read(new JsonPrimitive("#12ABCDEF")).value());
    }

    @Test
    void aDecimalBoundOfZeroHoldsWhateverItsSign() {
        ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.BOTH_REQUIRED);
        Setting<Double> offset = config.define(Setting.decimal("offset", Scope.CLIENT, -0.0, 0.0, 1.0));
        assertEquals(0.0, new ConfigValues(config.build(), Scope.CLIENT).set(offset, -0.0), 0.0);
        assertThrows(IllegalArgumentException.class, () -> Setting.decimal("range", Scope.CLIENT, 0.0, 0.0, -0.0));
    }
}
