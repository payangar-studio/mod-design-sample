package com.payangar.designsample.vendor.gui.widget;

import com.payangar.designsample.vendor.config.ConfigValues;
import com.payangar.designsample.vendor.config.Setting;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The value a widget shows and changes. A widget reads it on every frame, so a change made
 * elsewhere (a reset, another widget) shows at once, and a screen with a live preview reads the
 * same value.
 *
 * @param <T> the type of the value
 */
public interface Binding<T> {
    T get();

    void set(T value);

    /** A binding over a getter and a setter. */
    static <T> Binding<T> of(Supplier<T> getter, Consumer<T> setter) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(setter, "setter");
        return new Binding<>() {
            @Override
            public T get() {
                return getter.get();
            }

            @Override
            public void set(T value) {
                setter.accept(value);
            }
        };
    }

    /** {@code setting} in {@code values}, brought within its bounds when set. */
    static <T> Binding<T> of(ConfigValues values, Setting<T> setting) {
        return of(() -> values.get(setting), value -> values.set(setting, value));
    }

    /** A value held by the binding itself, starting at {@code initial}. */
    static <T> Binding<T> holding(T initial) {
        Object[] value = {initial};
        @SuppressWarnings("unchecked")
        Supplier<T> getter = () -> (T) value[0];
        return of(getter, v -> value[0] = v);
    }

    /** This binding, calling {@code listener} with each value set. */
    default Binding<T> onChange(Consumer<T> listener) {
        Binding<T> self = this;
        return of(self::get, value -> {
            self.set(value);
            listener.accept(self.get());
        });
    }
}
