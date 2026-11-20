package com.payangar.designsample.vendor.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The network of the library on Fabric, both sides. Fabric payloads are optional: a client without
 * them joins, so a mod required on both sides refuses it here.
 */
public final class FabricNetwork {
    private FabricNetwork() {}

    /** Registers {@code network}, from the mod's {@code ModInitializer}. */
    public static void register(ModNetwork network) {
        ServerConfig config = network.serverConfig();
        if (network.hasServerSettings()) {
            ServerLifecycleEvents.SERVER_STARTING.register(config::start);
            ServerLifecycleEvents.SERVER_STOPPED.register(server -> config.stop());
        }
        if (!network.enabled()) {
            return;
        }
        ConfigPayloads payloads = network.payloads();
        PayloadTypeRegistry.clientboundConfiguration().register(payloads.presenceType(), payloads.presenceCodec());
        PayloadTypeRegistry.clientboundPlay().register(payloads.syncType(), payloads.syncCodec());
        PayloadTypeRegistry.serverboundPlay().register(payloads.changeType(), payloads.changeCodec());

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            boolean hasMod = ServerConfigurationNetworking.canSend(handler, payloads.presenceType());
            if (SideCheck.refuses(network.side(), hasMod)) {
                handler.disconnect(SideCheck.missingOnClient(network.modId(), name(network.modId())));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler, payloads.syncType())) {
                config.sync().ifPresent(sender::sendPacket);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(payloads.changeType(), (change, context) -> config
                .change(context.player(), change)
                .ifPresent(sync -> sendToAll(context.server(), sync)));
    }

    /** Sends {@code sync} to every player whose client has the mod. */
    private static void sendToAll(MinecraftServer server, ConfigPayloads.Sync sync) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, sync.type())) {
                ServerPlayNetworking.send(player, sync);
            }
        }
    }

    private static String name(String modId) {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(mod -> mod.getMetadata().getName())
                .orElse(modId);
    }
}
