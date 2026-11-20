package com.payangar.designsample.vendor.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

/**
 * The client settings of a mod, in {@code <config dir>/<modid>.json} (see {@link ConfigFile}).
 * They are loaded on first use, so a mod may create this while it loads, before the game knows
 * its folders.
 *
 * <pre>{@code
 * ClientSettings settings = ClientSettings.of(MyConfig.SCHEMA);
 * boolean showHud = settings.values().get(MyConfig.SHOW_HUD);
 * }</pre>
 */
public final class ClientSettings implements SettingsSource {
    private final ConfigSchema schema;
    private final Supplier<Path> file;
    private ConfigValues values;

    private ClientSettings(ConfigSchema schema, Supplier<Path> file) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.file = file;
    }

    /**
     * The client settings of {@code schema} in the game's config folder, {@code <game
     * dir>/config}: the folder both loaders use.
     */
    public static ClientSettings of(ConfigSchema schema) {
        return new ClientSettings(schema, () -> ConfigFile.clientPath(
                Minecraft.getInstance().gameDirectory.toPath().resolve("config"), schema.modId()));
    }

    /** The client settings of {@code schema} in {@code file}. */
    public static ClientSettings at(ConfigSchema schema, Path file) {
        Objects.requireNonNull(file, "file");
        return new ClientSettings(schema, () -> file);
    }

    public ConfigSchema schema() {
        return schema;
    }

    /** The file of the settings. */
    public Path file() {
        return file.get();
    }

    /**
     * The values in use, loaded from the file on the first call. A file that cannot be read gives
     * the defaults, which {@link #save} never writes over it; the problem is logged.
     */
    @Override
    public synchronized ConfigValues values() {
        if (values == null) {
            try {
                values = ConfigFile.load(schema, Scope.CLIENT, file()).values();
            } catch (IOException e) {
                LoggerFactory.getLogger(schema.modId())
                        .warn("{} could not be read ({}): the default client settings are used, and the file is"
                                + " left as it is", file(), e.toString());
                values = new ConfigValues(schema, Scope.CLIENT);
                values.markFromBrokenFile();
            }
        }
        return values;
    }

    @Override
    public boolean editable() {
        return true;
    }

    /** Writes the values to the file. */
    @Override
    public void save() throws IOException {
        ConfigFile.save(values(), file());
    }
}
