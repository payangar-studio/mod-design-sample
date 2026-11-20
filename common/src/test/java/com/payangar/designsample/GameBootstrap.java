package com.payangar.designsample;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Loads the game's registries in a unit test: call {@link #run()} from a
 * {@code @BeforeAll} before touching an item, a block or any registry.
 */
public final class GameBootstrap {
    private GameBootstrap() {}

    /**
     * Bootstraps the game. Later calls do nothing, and so does a call where a
     * loader already started the game (NeoForge's JUnit runs).
     */
    public static void run() {
        try {
            SharedConstants.getCurrentVersion();
        } catch (IllegalStateException notSet) {
            SharedConstants.setVersion(DetectedVersion.tryDetectVersion());
        }
        Bootstrap.bootStrap();
    }
}
