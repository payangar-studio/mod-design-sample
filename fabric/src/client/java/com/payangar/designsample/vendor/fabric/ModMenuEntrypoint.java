package com.payangar.designsample.vendor.fabric;

import com.payangar.designsample.vendor.gui.screen.ConfigScreens;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.util.Map;

/**
 * Mod Menu's entrypoint (`modmenu` in fabric.mod.json): opens the screen registered with {@link
 * ConfigScreens}. Mod Menu is compile-only; without it, this class is never loaded. The screen is
 * looked up each time Mod Menu asks, so the mod may register it after Mod Menu has started.
 */
public final class ModMenuEntrypoint implements ModMenuApi {
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return ConfigScreens.modId()
                .<Map<String, ConfigScreenFactory<?>>>map(id -> Map.of(id, ConfigScreens::create))
                .orElse(Map.of());
    }
}
