package com.payangar.designsample.fabric.client;

import com.payangar.designsample.client.ClientEntrypoint;
import net.fabricmc.api.ClientModInitializer;

/** Fabric's client entrypoint, named in the generated fabric.mod.json. */
public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientEntrypoint.init();
    }
}
