package com.payangar.designsample.fabric;

import com.payangar.designsample.CommonEntrypoint;
import net.fabricmc.api.ModInitializer;

/** Fabric's entrypoint, named in the generated fabric.mod.json. */
public final class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonEntrypoint.init();
    }
}
