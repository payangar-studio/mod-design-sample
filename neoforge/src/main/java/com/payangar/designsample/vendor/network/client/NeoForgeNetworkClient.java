package com.payangar.designsample.vendor.network.client;

import com.payangar.designsample.vendor.network.ConfigPayloads;
import com.payangar.designsample.vendor.network.ModNetwork;
import com.payangar.designsample.vendor.network.ReceivedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The network of the library on a NeoForge client. */
public final class NeoForgeNetworkClient {
    private NeoForgeNetworkClient() {}

    /** Registers {@code network}, from the mod's client constructor, with its event bus {@code modBus}. */
    public static void register(ModNetwork network, IEventBus modBus) {
        if (!network.enabled()) {
            return;
        }
        ConfigPayloads payloads = network.payloads();
        ReceivedConfig received = network.receivedConfig();
        modBus.addListener(RegisterClientPayloadHandlersEvent.class, event -> {
            event.register(payloads.presenceType(), (presence, context) -> {});
            event.register(payloads.syncType(), (sync, context) -> received.receive(sync));
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> received.reset());
        received.connect(
                () -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    return connection != null && connection.hasChannel(payloads.changeType());
                },
                payload -> ClientPacketDistributor.sendToServer(payload));
    }
}
