package com.payangar.designsample.vendor.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The current values of the settings of one scope, and the keys of their file that no setting
 * declares. Those are kept as they are and written back, so that a value written by another
 * version of the mod survives.
 */
public final class ConfigValues {
    private final ConfigSchema schema;
    private final Scope scope;
    private final Map<Setting<?>, Object> values = new LinkedHashMap<>();
    private final JsonObject unknown = new JsonObject();
    private boolean fromBrokenFile;

    /** The default values of the settings of {@code scope}. */
    public ConfigValues(ConfigSchema schema, Scope scope) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.scope = Objects.requireNonNull(scope, "scope");
        for (Setting<?> setting : schema.settings(scope)) {
            values.put(setting, setting.defaultValue());
        }
    }

    public ConfigSchema schema() {
        return schema;
    }

    public Scope scope() {
        return scope;
    }

    /**
     * Whether these are the defaults used in place of a file that could not be read. {@link
     * ConfigFile#save} refuses them, so that the file is not overwritten.
     */
    public boolean fromBrokenFile() {
        return fromBrokenFile;
    }

    public <T> T get(Setting<T> setting) {
        @SuppressWarnings("unchecked")
        T value = (T) values.get(check(setting));
        return value;
    }

    /** Sets {@code setting} to {@code value} brought within its bounds, and returns what was set. */
    public <T> T set(Setting<T> setting, T value) {
        T clamped = check(setting).type().clamp(Objects.requireNonNull(value, "value"));
        values.put(setting, clamped);
        return clamped;
    }

    /** The file contents: every setting of the scope in declaration order, then the unknown keys. */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (Setting<?> setting : values.keySet()) {
            json.add(setting.name(), write(setting));
        }
        for (Map.Entry<String, JsonElement> entry : unknown.entrySet()) {
            json.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return json;
    }

    /**
     * The values of every setting of the scope in declaration order, without the unknown keys of
     * the file: what the server sends to its players.
     */
    public JsonObject settingsJson() {
        JsonObject json = new JsonObject();
        for (Setting<?> setting : values.keySet()) {
            json.add(setting.name(), write(setting));
        }
        return json;
    }

    /**
     * Sets each setting {@code json} names to its value there, brought within its bounds. A key no
     * setting of the scope declares, and a value that is not one of its setting's type, are left
     * out.
     *
     * @return what was wrong with {@code json}, empty when every value was read as it is
     */
    public List<String> read(JsonObject json) {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            Setting<?> setting = values.keySet().stream()
                    .filter(s -> s.name().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
            if (setting == null) {
                problems.add("`" + entry.getKey() + "` is not a " + scope + " setting of " + schema.modId());
            } else {
                read(setting, entry.getValue(), problems);
            }
        }
        return problems;
    }

    /** A copy, unknown keys included, that changes independently of these values. */
    public ConfigValues copy() {
        ConfigValues copy = new ConfigValues(schema, scope);
        copy.values.putAll(values);
        for (Map.Entry<String, JsonElement> entry : unknown.entrySet()) {
            copy.keepUnknown(entry.getKey(), entry.getValue());
        }
        copy.fromBrokenFile = fromBrokenFile;
        return copy;
    }

    void markFromBrokenFile() {
        fromBrokenFile = true;
    }

    /** Keeps a key of the file that no setting of the scope declares. */
    void keepUnknown(String key, JsonElement value) {
        unknown.add(key, value.deepCopy());
    }

    private <T> void read(Setting<T> setting, JsonElement json, List<String> problems) {
        SettingType.Reading<T> reading = setting.type().read(json);
        if (reading.value() == null) {
            problems.add("`" + setting.name() + "`: " + reading.problem());
            return;
        }
        set(setting, reading.value());
        if (reading.problem() != null) {
            problems.add("`" + setting.name() + "`: " + reading.problem());
        }
    }

    private <T> JsonElement write(Setting<T> setting) {
        return setting.type().write(get(setting));
    }

    private <T> Setting<T> check(Setting<T> setting) {
        if (!values.containsKey(setting)) {
            throw new IllegalArgumentException(
                    schema.modId() + ": setting `" + setting.name() + "` is not a " + scope + " setting of this schema");
        }
        return setting;
    }
}
