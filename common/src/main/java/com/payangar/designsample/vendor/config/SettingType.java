package com.payangar.designsample.vendor.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The type of a setting: how its value is written to JSON, read back and kept within its bounds.
 * A screen picks its widget by switching over these records.
 *
 * @param <T> the Java type of the value
 */
public sealed interface SettingType<T>
        permits SettingType.BooleanType,
                SettingType.IntegerType,
                SettingType.DecimalType,
                SettingType.EnumType,
                SettingType.StringType,
                SettingType.ColourType {

    /** {@code value} as stored in the file. */
    JsonElement write(T value);

    /** The value stored as {@code json}, brought within the bounds. */
    Reading<T> read(JsonElement json);

    /** {@code value} brought within the bounds. */
    T clamp(T value);

    /**
     * What {@link #read} found: the value to use, or {@code null} when the stored one is not a
     * value of this type; and what was wrong with the stored one, or {@code null}.
     */
    record Reading<T>(T value, String problem) {
        static <T> Reading<T> valid(T value) {
            return new Reading<>(value, null);
        }

        static <T> Reading<T> invalid(String problem) {
            return new Reading<>(null, problem);
        }
    }

    record BooleanType() implements SettingType<Boolean> {
        @Override
        public JsonElement write(Boolean value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Reading<Boolean> read(JsonElement json) {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
                return Reading.valid(json.getAsBoolean());
            }
            return Reading.invalid(json + " is not true or false");
        }

        @Override
        public Boolean clamp(Boolean value) {
            return value;
        }
    }

    /** A whole number between {@code min} and {@code max}, both included. */
    record IntegerType(int min, int max) implements SettingType<Integer> {
        public IntegerType {
            if (min > max) {
                throw new IllegalArgumentException("min " + min + " is above max " + max);
            }
        }

        @Override
        public JsonElement write(Integer value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Reading<Integer> read(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
                return Reading.invalid(json + " is not a number");
            }
            BigDecimal number;
            try {
                number = json.getAsBigDecimal();
            } catch (NumberFormatException e) {
                return Reading.invalid(json + " is not a number");
            }
            if (number.stripTrailingZeros().scale() > 0) {
                return Reading.invalid(json + " is not a whole number");
            }
            if (number.compareTo(BigDecimal.valueOf(min)) < 0) {
                return new Reading<>(min, number + " is below the minimum " + min + ", set to " + min);
            }
            if (number.compareTo(BigDecimal.valueOf(max)) > 0) {
                return new Reading<>(max, number + " is above the maximum " + max + ", set to " + max);
            }
            return Reading.valid(number.intValueExact());
        }

        @Override
        public Integer clamp(Integer value) {
            return Math.clamp(value, min, max);
        }
    }

    /** A finite decimal number between {@code min} and {@code max}, both included. */
    record DecimalType(double min, double max) implements SettingType<Double> {
        public DecimalType {
            // Double.compare, not >: Math.clamp refuses a min of 0.0 with a max of -0.0.
            if (!Double.isFinite(min) || !Double.isFinite(max) || Double.compare(min, max) > 0) {
                throw new IllegalArgumentException("bounds " + min + " and " + max + " are not a range");
            }
        }

        @Override
        public JsonElement write(Double value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Reading<Double> read(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
                return Reading.invalid(json + " is not a number");
            }
            double number;
            try {
                number = json.getAsDouble();
            } catch (NumberFormatException e) {
                return Reading.invalid(json + " is not a number");
            }
            if (!Double.isFinite(number)) {
                return Reading.invalid(json + " is not a finite number");
            }
            if (number < min) {
                return new Reading<>(min, number + " is below the minimum " + min + ", set to " + min);
            }
            if (number > max) {
                return new Reading<>(max, number + " is above the maximum " + max + ", set to " + max);
            }
            return Reading.valid(number);
        }

        @Override
        public Double clamp(Double value) {
            if (Double.isNaN(value)) {
                throw new IllegalArgumentException("NaN is not a value of a decimal setting");
            }
            // Kept as it is when within the bounds: Math.clamp takes -0.0 for below a min of 0.0.
            return value >= min && value <= max ? value : Math.clamp(value, min, max);
        }
    }

    /** One of the constants of {@code type}, stored as its name in lower case. */
    record EnumType<E extends Enum<E>>(Class<E> type) implements SettingType<E> {
        @Override
        public JsonElement write(E value) {
            return new JsonPrimitive(value.name().toLowerCase(Locale.ROOT));
        }

        @Override
        public Reading<E> read(JsonElement json) {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                String name = json.getAsString();
                for (E constant : type.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(name)) {
                        return Reading.valid(constant);
                    }
                }
            }
            return Reading.invalid(json + " is not one of " + choices());
        }

        @Override
        public E clamp(E value) {
            return value;
        }

        private String choices() {
            StringBuilder out = new StringBuilder();
            for (E constant : type.getEnumConstants()) {
                if (!out.isEmpty()) {
                    out.append(", ");
                }
                out.append(constant.name().toLowerCase(Locale.ROOT));
            }
            return out.toString();
        }
    }

    record StringType() implements SettingType<String> {
        @Override
        public JsonElement write(String value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Reading<String> read(JsonElement json) {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                return Reading.valid(json.getAsString());
            }
            return Reading.invalid(json + " is not a string");
        }

        @Override
        public String clamp(String value) {
            return value;
        }
    }

    /**
     * A colour as an ARGB integer, stored as {@code "#RRGGBB"}, or {@code "#AARRGGBB"} when it has
     * an {@code alpha} channel. Without one, the alpha is always opaque.
     */
    record ColourType(boolean alpha) implements SettingType<Integer> {
        private static final Pattern OPAQUE = Pattern.compile("#[0-9A-Fa-f]{6}");
        private static final Pattern TRANSLUCENT = Pattern.compile("#[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?");

        @Override
        public JsonElement write(Integer value) {
            return new JsonPrimitive(
                    alpha
                            ? String.format(Locale.ROOT, "#%08X", value)
                            : String.format(Locale.ROOT, "#%06X", value & 0xFFFFFF));
        }

        @Override
        public Reading<Integer> read(JsonElement json) {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                String text = json.getAsString();
                if ((alpha ? TRANSLUCENT : OPAQUE).matcher(text).matches()) {
                    int value = Integer.parseUnsignedInt(text.substring(1), 16);
                    return Reading.valid(text.length() == 7 ? value | 0xFF000000 : value);
                }
            }
            return Reading.invalid(json + " is not a colour written " + (alpha ? "#AARRGGBB or #RRGGBB" : "#RRGGBB"));
        }

        /** {@code value}, opaque when the colour has no alpha channel. */
        @Override
        public Integer clamp(Integer value) {
            return alpha ? value : value | 0xFF000000;
        }
    }
}
