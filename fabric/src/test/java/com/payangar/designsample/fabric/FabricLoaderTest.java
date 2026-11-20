package com.payangar.designsample.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.payangar.designsample.CommonEntrypoint;
import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.Test;

/**
 * Example unit test on Fabric: fabric-loader-junit runs JUnit inside Fabric
 * Loader, so the mod is loaded, with its mixins applied.
 */
class FabricLoaderTest {
    @Test
    void theModIsLoaded() {
        assertTrue(FabricLoader.getInstance().isModLoaded(CommonEntrypoint.MOD_ID));
    }
}
