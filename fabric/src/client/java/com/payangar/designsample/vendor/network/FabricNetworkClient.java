package com.payangar.designsample.vendor.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** The network of the library on a Fabric client. */
public final class FabricNetworkClient {
    private FabricNetworkClient() {}

    /** Registers {@code network}, from the mod's {@code ClientModInitializer}. */
    public static void register(ModNetwork network) {
        if (!network.enabled()) {
            return;
        }
        ConfigPayloads payloads = network.payloads();
        ReceivedConfig received = network.receivedConfig();
        // Receiving it is what tells the server this client has the mod.
        ClientConfigurationNetworking.registerGlobalReceiver(payloads.presenceType(), (presence, context) -> {});
        ClientPlayNetworking.registerGlobalReceiver(payloads.syncType(), (sync, context) -> received.receive(sync));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> received.reset());
        received.connect(() -> ClientPlayNetworking.canSend(payloads.changeType()), ClientPlayNetworking::send);
    }
}
