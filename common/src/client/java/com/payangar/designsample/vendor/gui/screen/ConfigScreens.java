package com.payangar.designsample.vendor.gui.screen;

import com.payangar.designsample.vendor.config.ClientSettings;
import com.payangar.designsample.vendor.config.ConfigSchema;
import com.payangar.designsample.vendor.config.ConfigValues;
import com.payangar.designsample.vendor.config.Scope;
import com.payangar.designsample.vendor.config.SettingsSource;
import com.payangar.designsample.vendor.gui.Texts;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;

/**
 * The config screen of the host mod, which the loaders' mod lists open: Mod Menu on Fabric, the
 * config screen extension point on NeoForge. Each mod carries its own copy of the library, so
 * this registry holds one mod's screen. Register it from the mod's client entrypoint:
 *
 * <pre>{@code
 * ClientSettings settings = ClientSettings.of(MyConfig.SCHEMA);
 * ConfigScreens.register(MyConfig.SCHEMA, settings);                     // the generated screen
 * ConfigScreens.register(MyConfig.SCHEMA, parent -> new MyScreen(parent)); // or a custom one
 * }</pre>
 */
public final class ConfigScreens {
    private static ConfigSchema schema;
    private static Function<Screen, Screen> factory;
    private static Supplier<SettingsSource> server;

    private ConfigScreens() {}

    /** Registers the screen generated from {@code schema}, over the client settings {@code client}. */
    public static synchronized void register(ConfigSchema schema, ClientSettings client) {
        Objects.requireNonNull(client, "client");
        register(schema, parent -> generated(parent, schema, client));
    }

    /** Registers a screen of the mod's own, built by {@code factory} from the screen to go back to. */
    public static synchronized void register(ConfigSchema schema, Function<Screen, Screen> factory) {
        ConfigScreens.schema = Objects.requireNonNull(schema, "schema");
        ConfigScreens.factory = Objects.requireNonNull(factory, "factory");
    }

    /**
     * Where the generated screen takes the server settings from. Without one, they are shown with
     * their defaults, read-only.
     */
    public static synchronized void serverSettings(Supplier<SettingsSource> source) {
        server = source;
    }

    /** The id of the mod whose screen is registered. */
    public static synchronized Optional<String> modId() {
        return Optional.ofNullable(schema).map(ConfigSchema::modId);
    }

    /** The registered screen, going back to {@code parent}; {@code null} when none is. */
    public static synchronized Screen create(Screen parent) {
        return factory == null ? null : factory.apply(parent);
    }

    /** The generated screen of {@code schema}, titled with the library's config title. */
    public static Screen generated(Screen parent, ConfigSchema schema, ClientSettings client) {
        Map<Scope, SettingsSource> sources = new EnumMap<>(Scope.class);
        sources.put(Scope.CLIENT, client);
        sources.put(Scope.SERVER, serverSource(schema));
        return new GeneratedConfigScreen(Texts.vendor(schema.modId(), "config.title"), parent, schema, sources);
    }

    private static synchronized SettingsSource serverSource(ConfigSchema schema) {
        return server != null ? server.get() : SettingsSource.readOnly(new ConfigValues(schema, Scope.SERVER));
    }
}
