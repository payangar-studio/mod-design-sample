package com.payangar.designsample.vendor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigSchemaTest {
    @Test
    void aClientOnlyModRefusesAServerSetting() {
        ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.CLIENT);
        config.define(Setting.bool("show_hud", Scope.CLIENT, true));
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64)));
        assertTrue(error.getMessage().contains("`range` has the server scope"), error.getMessage());
        assertEquals(List.of("show_hud"), names(config.build().settings()));
    }

    @Test
    void aServerOnlyModRefusesAClientSetting() {
        ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.SERVER);
        config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64));
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> config.define(Setting.bool("show_hud", Scope.CLIENT, true)));
        assertTrue(error.getMessage().contains("`show_hud` has the client scope"), error.getMessage());
        assertEquals(List.of("range"), names(config.build().settings()));
    }

    @Test
    void everyOtherSideAcceptsServerSettings() {
        for (ModSide side : List.of(ModSide.SERVER, ModSide.BOTH_REQUIRED, ModSide.BOTH_OPTIONAL)) {
            ConfigSchema.Builder config = ConfigSchema.builder("demo", side);
            config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64));
            assertEquals(1, config.build().settings(Scope.SERVER).size(), side.toString());
        }
    }

    @Test
    void settingsAreSplitByScopeInDeclarationOrder() {
        ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.BOTH_REQUIRED);
        config.define(Setting.bool("b", Scope.CLIENT, true));
        config.define(Setting.integer("a", Scope.SERVER, 1, 0, 2));
        config.define(Setting.string("c", Scope.CLIENT, ""));
        ConfigSchema schema = config.build();
        assertEquals(List.of("b", "a", "c"), names(schema.settings()));
        assertEquals(List.of("b", "c"), names(schema.settings(Scope.CLIENT)));
        assertEquals(List.of("a"), names(schema.settings(Scope.SERVER)));
    }

    @Test
    void translationKeysDefaultToTheModNamespace() {
        ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.CLIENT);
        Setting<Boolean> plain = config.define(Setting.bool("show_hud", Scope.CLIENT, true));
        Setting<Boolean> custom = config.define(
                Setting.bool("other", Scope.CLIENT, true).label("demo.hud").tooltip("demo.hud.help"));
        assertEquals("demo.config.show_hud", plain.labelKey());
        assertEquals("demo.config.show_hud.tooltip", plain.tooltipKey());
        assertEquals("demo.hud", custom.labelKey());
        assertEquals("demo.hud.help", custom.tooltipKey());
    }

    @Test
    void invalidDeclarationsAreRefused() {
        ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.BOTH_REQUIRED);
        config.define(Setting.bool("taken", Scope.CLIENT, true));
        assertThrows(IllegalArgumentException.class, () -> config.define(Setting.bool("taken", Scope.SERVER, true)));
        assertThrows(IllegalArgumentException.class, () -> config.define(Setting.bool("Bad-Name", Scope.CLIENT, true)));
        assertThrows(IllegalArgumentException.class, () -> config.define(Setting.integer("low", Scope.CLIENT, 0, 1, 64)));
        assertThrows(IllegalArgumentException.class, () -> config.define(Setting.decimal("high", Scope.CLIENT, 2, 0, 1)));
        assertThrows(IllegalArgumentException.class, () -> Setting.integer("range", Scope.CLIENT, 5, 10, 1));
        config.build();
        assertThrows(IllegalStateException.class, () -> config.define(Setting.bool("late", Scope.CLIENT, true)));
    }

    private static List<String> names(List<Setting<?>> settings) {
        return settings.stream().map(Setting::name).toList();
    }
}
