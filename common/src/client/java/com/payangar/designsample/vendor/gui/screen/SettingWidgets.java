package com.payangar.designsample.vendor.gui.screen;

import com.payangar.designsample.vendor.config.Setting;
import com.payangar.designsample.vendor.config.SettingType;
import com.payangar.designsample.vendor.gui.Texts;
import com.payangar.designsample.vendor.gui.widget.Binding;
import com.payangar.designsample.vendor.gui.widget.ColourSwatch;
import com.payangar.designsample.vendor.gui.widget.Dropdown;
import com.payangar.designsample.vendor.gui.widget.Segmented;
import com.payangar.designsample.vendor.gui.widget.Slider;
import com.payangar.designsample.vendor.gui.widget.TextField;
import com.payangar.designsample.vendor.gui.widget.Toggle;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/** The widget of a setting in the generated screen, chosen by its type. */
public final class SettingWidgets {
    /** The widest number range a slider takes; a wider one is typed. */
    public static final long SLIDER_RANGE = 1000;
    /** The most choices shown side by side; more go in a dropdown. */
    public static final int SEGMENTS = 3;

    private SettingWidgets() {}

    /** The label of a setting: its translation, or its name made readable. */
    public static Component label(Setting<?> setting) {
        return Texts.orName(setting.labelKey(), setting.name());
    }

    /**
     * The label of a choice of an enum setting: {@code <label key>.<constant in lower case>}, or
     * the constant's name made readable.
     */
    public static Component choice(Setting<?> setting, Enum<?> constant) {
        String name = constant.name().toLowerCase(Locale.ROOT);
        return Texts.orName(setting.labelKey() + "." + name, name);
    }

    /**
     * The widget for {@code setting}, {@code width} wide, showing and changing {@code binding}:
     *
     * <ul>
     *   <li>true or false: a toggle;
     *   <li>number: a slider, typed when its bounds are equal or its range is wider than {@link
     *       #SLIDER_RANGE};
     *   <li>choice: a segmented control up to {@link #SEGMENTS} choices, a dropdown beyond;
     *   <li>text: a text field;
     *   <li>colour: a swatch that opens a picker.
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public static AbstractWidget create(int width, Setting<?> setting, Binding<?> binding) {
        Component label = label(setting);
        return switch (setting.type()) {
            case SettingType.BooleanType type -> new Toggle(label, (Binding<Boolean>) binding);
            case SettingType.IntegerType(int min, int max) when min < max && (long) max - min <= SLIDER_RANGE ->
                    Slider.integer(width, label, min, max, (Binding<Integer>) binding);
            case SettingType.IntegerType(int min, int max) -> typedInteger(width, label, min, max, (Binding<Integer>) binding);
            // Bounds too far apart for a double (±Double.MAX_VALUE) give an infinite range: typed too.
            case SettingType.DecimalType(double min, double max) when min < max && max - min <= SLIDER_RANGE ->
                    Slider.decimal(width, label, min, max, (Binding<Double>) binding);
            case SettingType.DecimalType type -> typedDecimal(width, label, type, (Binding<Double>) binding);
            case SettingType.EnumType<?> type -> choices(width, label, setting, type, binding);
            case SettingType.StringType type -> new TextField(width, label, (Binding<String>) binding);
            case SettingType.ColourType(boolean alpha) -> new ColourSwatch(width, label, (Binding<Integer>) binding, alpha);
        };
    }

    private static TextField typedInteger(int width, Component label, int min, int max, Binding<Integer> binding) {
        return new TextField(width, label, Binding.of(() -> Integer.toString(binding.get()), text -> binding.set(Integer.parseInt(text.trim()))))
                .validate(text -> {
                    try {
                        int value = Integer.parseInt(text.trim());
                        return value >= min && value <= max;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                });
    }

    private static TextField typedDecimal(int width, Component label, SettingType.DecimalType type, Binding<Double> binding) {
        return new TextField(width, label, Binding.of(() -> Double.toString(binding.get()), text -> binding.set(Double.parseDouble(text.trim()))))
                .validate(text -> {
                    try {
                        double value = Double.parseDouble(text.trim());
                        return value >= type.min() && value <= type.max();
                    } catch (NumberFormatException e) {
                        return false;
                    }
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static AbstractWidget choices(int width, Component label, Setting<?> setting, SettingType.EnumType<?> type, Binding<?> binding) {
        List<Enum<?>> constants = List.of((Enum<?>[]) type.type().getEnumConstants());
        Function<Enum<?>, Component> name = constant -> choice(setting, constant);
        Binding<Enum<?>> value = (Binding) binding;
        return constants.size() <= SEGMENTS
                ? new Segmented<>(width, label, constants, name, value)
                : new Dropdown<>(width, label, constants, name, value);
    }
}
