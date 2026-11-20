package com.payangar.designsample.vendor.neoforge;

import com.payangar.designsample.vendor.gui.screen.ConfigScreens;
import java.util.function.Supplier;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Gives NeoForge's mod list the screen registered with {@link ConfigScreens}, through the config
 * screen extension point. Call it from the mod's client entrypoint, which receives its {@link
 * ModContainer}:
 *
 * <pre>{@code
 * @Mod(value = MOD_ID, dist = Dist.CLIENT)
 * public NeoForgeClientEntrypoint(ModContainer container) {
 *     NeoForgeConfigScreen.register(container);
 * }
 * }</pre>
 *
 * The screen is looked up each time the mod list asks: without one registered, the mod has no
 * config button.
 */
public final class NeoForgeConfigScreen {
    private static final IConfigScreenFactory FACTORY = (container, parent) -> ConfigScreens.create(parent);

    private NeoForgeConfigScreen() {}

    public static void register(ModContainer container) {
        Supplier<IConfigScreenFactory> factory = () -> ConfigScreens.modId().isPresent() ? FACTORY : null;
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
