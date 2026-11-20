package com.payangar.designsample.vendor.network;

import com.payangar.designsample.vendor.config.ModSide;
import net.minecraft.network.chat.Component;

/**
 * Refuses a client that lacks a mod required on both sides. NeoForge would refuse it on its own
 * for a required payload, with a message about a network channel; Fabric would let it in. The
 * library registers its payloads as optional on both loaders and refuses the client itself, with
 * the same message.
 */
public final class SideCheck {
    private SideCheck() {}

    /** Whether the server refuses a client that has the mod or not. */
    public static boolean refuses(ModSide side, boolean clientHasMod) {
        return side == ModSide.BOTH_REQUIRED && !clientHasMod;
    }

    /**
     * Why the client is refused. A client without the mod has none of its translations, so the
     * message carries its English text as the fallback.
     */
    public static Component missingOnClient(String modId, String modName) {
        return Component.translatableWithFallback(
                modId + ".vendor.side_check.missing_on_client",
                "This server requires the mod %s. Install it to join.",
                modName);
    }
}
