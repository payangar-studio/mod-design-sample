package com.payangar.designsample.vendor.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The network of the library on NeoForge, both sides. Its payloads are optional, so that NeoForge
 * does not refuse a client without them with its own message about a network channel: a mod
 * required on both sides refuses it here, with the same message as on Fabric.
 */
public final class NeoForgeNetwork {
    /** The version of the payloads: a client and a server with different ones do not exchange them. */
    private static final String VERSION = "1";

    private NeoForgeNetwork() {}

    /** Registers {@code network}, from the mod's constructor, with its event bus {@code modBus}. */
    public static void register(ModNetwork network, IEventBus modBus) {
        ServerConfig config = network.serverConfig();
        if (network.hasServerSettings()) {
            NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, event -> config.start(event.getServer()));
            NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> config.stop());
        }
        if (!network.enabled()) {
            return;
        }
        ConfigPayloads payloads = network.payloads();
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(VERSION).optional();
            registrar.configurationToClient(payloads.presenceType(), payloads.presenceCodec());
            registrar.playToClient(payloads.syncType(), payloads.syncCodec());
            registrar.playToServer(payloads.changeType(), payloads.changeCodec(), (change, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    config.change(player, change).ifPresent(sync -> sendToAll(player.level().getServer(), sync));
                }
            });
        });
        modBus.addListener(RegisterConfigurationTasksEvent.class, event -> {
            var listener = event.getListener();
            if (SideCheck.refuses(network.side(), listener.hasChannel(payloads.presenceType()))) {
                listener.disconnect(SideCheck.missingOnClient(network.modId(), name(network.modId())));
            }
        });

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && player.connection.hasChannel(payloads.syncType())) {
                config.sync().ifPresent(sync -> PacketDistributor.sendToPlayer(player, sync));
            }
        });
    }

    /** Sends {@code sync} to every player whose client has the mod. */
    private static void sendToAll(MinecraftServer server, ConfigPayloads.Sync sync) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.connection.hasChannel(sync.type())) {
                PacketDistributor.sendToPlayer(player, sync);
            }
        }
    }

    private static String name(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .map(mod -> mod.getModInfo().getDisplayName())
                .orElse(modId);
    }
}
