package com.payangar.designsample.vendor.gui;

import java.util.Locale;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The library's strings. They live in the socle's translation file and are merged into the host
 * mod's language file under {@code <modid>.vendor.*}, so that each mod's copy has its own keys.
 */
public final class Texts {
    private Texts() {}

    /** The library string {@code key} of the mod {@code modId}: {@code <modId>.vendor.<key>}. */
    public static MutableComponent vendor(String modId, String key, Object... args) {
        return Component.translatable(modId + ".vendor." + key, args);
    }

    /**
     * The translation of {@code key}, or {@code name} made readable ({@code show_hud} as "Show
     * hud") when the mod has none.
     */
    public static MutableComponent orName(String key, String name) {
        return Component.translatableWithFallback(key, readable(name));
    }

    /** The translation of {@code key}, or {@code null} when the mod has none. */
    public static Component ifPresent(String key) {
        return Language.getInstance().has(key) ? Component.translatable(key) : null;
    }

    /** {@code name} as words: underscores as spaces, the first letter in capital. */
    public static String readable(String name) {
        String words = name.replace('_', ' ').toLowerCase(Locale.ROOT).trim();
        return words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
