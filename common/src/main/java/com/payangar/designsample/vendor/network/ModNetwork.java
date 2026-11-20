package com.payangar.designsample.vendor.network;

import com.payangar.designsample.vendor.config.ConfigSchema;
import com.payangar.designsample.vendor.config.ModSide;

/**
 * What the library does over the network for one mod: refuse a client without the mod when it is
 * required on both sides, and sync the server settings of its schema. A mod creates one, in code
 * shared by the loaders, and registers it from each loader's entrypoints:
 *
 * <pre>{@code
 * public static final ModNetwork NETWORK = new ModNetwork(CONFIG);
 * // Fabric: FabricNetwork.register(NETWORK), and FabricNetworkClient.register(NETWORK) on the client.
 * // NeoForge: NeoForgeNetwork.register(NETWORK, modBus), and NeoForgeNetworkClient.register(NETWORK, modBus)
 * // on the client.
 * }</pre>
 *
 * A mod on one side only has no network: the other side does not have it. A server-only mod
 * registers it all the same, from its {@code ModInitializer} or its constructor, so that its server
 * settings are loaded with each world.
 */
public final class ModNetwork {
    private final ConfigSchema schema;
    private final ConfigPayloads payloads;
    private final ServerConfig serverConfig;
    private final ReceivedConfig receivedConfig;

    public ModNetwork(ConfigSchema schema) {
        this.schema = schema;
        this.payloads = new ConfigPayloads(schema.modId());
        this.serverConfig = new ServerConfig(schema, payloads);
        this.receivedConfig = new ReceivedConfig(schema, payloads);
    }

    public String modId() {
        return schema.modId();
    }

    public ModSide side() {
        return schema.side();
    }

    /** Whether the mod has server settings, loaded from the world when a server starts. */
    public boolean hasServerSettings() {
        return schema.side().allowsServerScope();
    }

    /** Whether the mod has a network: it runs on both sides. */
    public boolean enabled() {
        return schema.side() == ModSide.BOTH_REQUIRED || schema.side() == ModSide.BOTH_OPTIONAL;
    }

    public ConfigPayloads payloads() {
        return payloads;
    }

    /** The server settings of the running server. */
    public ServerConfig serverConfig() {
        return serverConfig;
    }

    /** The server settings the client received. */
    public ReceivedConfig receivedConfig() {
        return receivedConfig;
    }
}
