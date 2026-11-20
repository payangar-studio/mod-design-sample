package com.payangar.designsample.vendor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigValuesTest {
    private final ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.BOTH_REQUIRED);
    private final Setting<Boolean> showHud = config.define(Setting.bool("show_hud", Scope.CLIENT, true));
    private final Setting<Integer> range = config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64));
    private final Setting<Boolean> pvp = config.define(Setting.bool("pvp", Scope.SERVER, false));
    private final ConfigSchema schema = config.build();

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void settingsJsonHoldsTheSettingsOfTheScopeWithoutUnknownKeys() {
        ConfigValues values = new ConfigValues(schema, Scope.SERVER);
        values.keepUnknown("old", json("{\"x\": 1}"));

        assertEquals(json("{\"range\": 8, \"pvp\": false}"), values.settingsJson());
        assertTrue(values.toJson().has("old"));
    }

    @Test
    void readSetsTheValuesItNames() {
        ConfigValues values = new ConfigValues(schema, Scope.SERVER);

        assertEquals(List.of(), values.read(json("{\"range\": 16}")));
        assertEquals(16, values.get(range));
        assertEquals(false, values.get(pvp));
    }

    @Test
    void readReportsWhatItLeavesOutOrClamps() {
        ConfigValues values = new ConfigValues(schema, Scope.SERVER);

        List<String> problems = values.read(json("{\"range\": 100, \"pvp\": \"yes\", \"show_hud\": false, \"gone\": 1}"));

        assertEquals(4, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("above the maximum 64"), problems.get(0));
        assertTrue(problems.get(1).contains("not true or false"), problems.get(1));
        assertTrue(problems.get(2).contains("`show_hud` is not a SERVER setting"), problems.get(2));
        assertTrue(problems.get(3).contains("`gone` is not a SERVER setting"), problems.get(3));
        assertEquals(64, values.get(range));
        assertEquals(false, values.get(pvp));
    }

    @Test
    void aCopyChangesIndependently() {
        ConfigValues values = new ConfigValues(schema, Scope.CLIENT);
        values.keepUnknown("old", json("{\"x\": 1}"));
        ConfigValues copy = values.copy();

        copy.set(showHud, false);

        assertEquals(true, values.get(showHud));
        assertEquals(false, copy.get(showHud));
        assertEquals(values.toJson().get("old"), copy.toJson().get("old"));
    }

    @Test
    void aCopyOfValuesFromABrokenFileIsNotSaved() {
        ConfigValues values = new ConfigValues(schema, Scope.CLIENT);
        values.markFromBrokenFile();

        assertTrue(values.copy().fromBrokenFile());
    }
}
