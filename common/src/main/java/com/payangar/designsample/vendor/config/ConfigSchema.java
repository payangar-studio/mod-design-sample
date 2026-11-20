package com.payangar.designsample.vendor.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The settings of a mod, declared once. The JSON files and the generated screen derive from it.
 *
 * <pre>{@code
 * ConfigSchema.Builder config = ConfigSchema.builder(MOD_ID, ModSide.BOTH_REQUIRED);
 * Setting<Boolean> showHud = config.define(Setting.bool("show_hud", Scope.CLIENT, true));
 * Setting<Integer> range = config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64));
 * ConfigSchema schema = config.build();
 * }</pre>
 */
public final class ConfigSchema {
    /** A setting name: a JSON key and part of a translation key. */
    private static final Pattern NAME = Pattern.compile("[a-z0-9_]+");

    private final String modId;
    private final ModSide side;
    private final List<Setting<?>> settings;

    private ConfigSchema(String modId, ModSide side, List<Setting<?>> settings) {
        this.modId = modId;
        this.side = side;
        this.settings = List.copyOf(settings);
    }

    public static Builder builder(String modId, ModSide side) {
        return new Builder(modId, side);
    }

    public String modId() {
        return modId;
    }

    public ModSide side() {
        return side;
    }

    /** Every setting, in declaration order. */
    public List<Setting<?>> settings() {
        return settings;
    }

    /** The settings of {@code scope}, in declaration order. */
    public List<Setting<?>> settings(Scope scope) {
        return settings.stream().filter(s -> s.scope() == scope).toList();
    }

    public static final class Builder {
        private final String modId;
        private final ModSide side;
        private final List<Setting<?>> settings = new ArrayList<>();
        private boolean built;

        private Builder(String modId, ModSide side) {
            this.modId = Objects.requireNonNull(modId, "modId");
            this.side = Objects.requireNonNull(side, "side");
        }

        /**
         * Adds a setting to the schema.
         *
         * @throws IllegalArgumentException when the name is not lower case letters, digits and
         *     underscores, is already taken, or the default is out of bounds; or when the setting
         *     has the server scope and the mod is client only, or the client scope and the mod is
         *     server only
         */
        public <T> Setting<T> define(Setting.Spec<T> spec) {
            if (built) {
                throw new IllegalStateException(modId + ": the config schema is already built");
            }
            String name = spec.name();
            if (!NAME.matcher(name).matches()) {
                throw new IllegalArgumentException(
                        modId + ": setting `" + name + "` must be named with a-z, 0-9 and _ only");
            }
            if (settings.stream().anyMatch(s -> s.name().equals(name))) {
                throw new IllegalArgumentException(modId + ": setting `" + name + "` is declared twice");
            }
            if (spec.scope() == Scope.SERVER && !side.allowsServerScope()) {
                throw new IllegalArgumentException(
                        modId + ": setting `" + name + "` has the server scope, but a client-only mod has"
                                + " client settings only. Declare it with Scope.CLIENT, or change the side"
                                + " of the mod in mod.toml.");
            }
            if (spec.scope() == Scope.CLIENT && !side.allowsClientScope()) {
                throw new IllegalArgumentException(
                        modId + ": setting `" + name + "` has the client scope, but a server-only mod has"
                                + " server settings only. Declare it with Scope.SERVER, or change the side"
                                + " of the mod in mod.toml.");
            }
            Setting<T> setting = spec.build(modId);
            settings.add(setting);
            return setting;
        }

        public ConfigSchema build() {
            built = true;
            return new ConfigSchema(modId, side, settings);
        }
    }
}
