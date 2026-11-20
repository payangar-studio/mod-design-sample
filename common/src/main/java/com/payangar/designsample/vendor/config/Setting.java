package com.payangar.designsample.vendor.config;

import java.util.Objects;

/**
 * A setting of the schema: its name (the key in the JSON file), scope, type, default value and
 * translation keys. Declared through {@link ConfigSchema.Builder#define}, from one of the
 * factories here:
 *
 * <pre>{@code
 * Setting<Integer> range = config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64));
 * }</pre>
 *
 * @param <T> the Java type of the value
 */
public final class Setting<T> {
    private final String name;
    private final Scope scope;
    private final SettingType<T> type;
    private final T defaultValue;
    private final String labelKey;
    private final String tooltipKey;

    private Setting(Spec<T> spec, String modId) {
        this.name = spec.name;
        this.scope = spec.scope;
        this.type = spec.type;
        this.defaultValue = spec.defaultValue;
        String base = modId + ".config." + spec.name;
        this.labelKey = spec.labelKey != null ? spec.labelKey : base;
        this.tooltipKey = spec.tooltipKey != null ? spec.tooltipKey : base + ".tooltip";
    }

    public String name() {
        return name;
    }

    public Scope scope() {
        return scope;
    }

    public SettingType<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    /** Translation key of the label: {@code <modid>.config.<name>} unless overridden. */
    public String labelKey() {
        return labelKey;
    }

    /** Translation key of the tooltip: {@code <modid>.config.<name>.tooltip} unless overridden. */
    public String tooltipKey() {
        return tooltipKey;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Spec<Boolean> bool(String name, Scope scope, boolean defaultValue) {
        return new Spec<>(name, scope, new SettingType.BooleanType(), defaultValue);
    }

    /** A whole number between {@code min} and {@code max}, both included. */
    public static Spec<Integer> integer(String name, Scope scope, int defaultValue, int min, int max) {
        return new Spec<>(name, scope, new SettingType.IntegerType(min, max), defaultValue);
    }

    /** A decimal number between {@code min} and {@code max}, both included. */
    public static Spec<Double> decimal(String name, Scope scope, double defaultValue, double min, double max) {
        return new Spec<>(name, scope, new SettingType.DecimalType(min, max), defaultValue);
    }

    /** One of the constants of the enum of {@code defaultValue}. */
    public static <E extends Enum<E>> Spec<E> choice(String name, Scope scope, E defaultValue) {
        return new Spec<>(name, scope, new SettingType.EnumType<>(defaultValue.getDeclaringClass()), defaultValue);
    }

    public static Spec<String> string(String name, Scope scope, String defaultValue) {
        return new Spec<>(name, scope, new SettingType.StringType(), defaultValue);
    }

    /** An ARGB colour; without {@code alpha}, always opaque. */
    public static Spec<Integer> colour(String name, Scope scope, int defaultArgb, boolean alpha) {
        SettingType.ColourType type = new SettingType.ColourType(alpha);
        return new Spec<>(name, scope, type, type.clamp(defaultArgb));
    }

    /** A setting not yet in a schema, whose translation keys may still be overridden. */
    public static final class Spec<T> {
        private final String name;
        private final Scope scope;
        private final SettingType<T> type;
        private final T defaultValue;
        private String labelKey;
        private String tooltipKey;

        private Spec(String name, Scope scope, SettingType<T> type, T defaultValue) {
            this.name = Objects.requireNonNull(name, "name");
            this.scope = Objects.requireNonNull(scope, "scope");
            this.type = type;
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        }

        public Spec<T> label(String key) {
            this.labelKey = Objects.requireNonNull(key, "key");
            return this;
        }

        public Spec<T> tooltip(String key) {
            this.tooltipKey = Objects.requireNonNull(key, "key");
            return this;
        }

        String name() {
            return name;
        }

        Scope scope() {
            return scope;
        }

        Setting<T> build(String modId) {
            if (!type.clamp(defaultValue).equals(defaultValue)) {
                throw new IllegalArgumentException(
                        modId + ": the default " + defaultValue + " of setting `" + name + "` is out of its bounds");
            }
            return new Setting<>(this, modId);
        }
    }
}
