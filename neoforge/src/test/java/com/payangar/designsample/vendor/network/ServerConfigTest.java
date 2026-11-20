package com.payangar.designsample.vendor.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.payangar.designsample.vendor.config.ConfigFile;
import com.payangar.designsample.vendor.config.ConfigSchema;
import com.payangar.designsample.vendor.config.ModSide;
import com.payangar.designsample.vendor.config.Scope;
import com.payangar.designsample.vendor.config.Setting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The server side of the sync, on a server of the game: the permission check against its list of
 * operators, the world's file, and the settings sent back, read by a client.
 */
@ExtendWith(EphemeralTestServerProvider.class)
class ServerConfigTest {
    private static final String MOD_ID = "vendor_network_test";

    private final ConfigSchema.Builder config = ConfigSchema.builder(MOD_ID, ModSide.BOTH_REQUIRED);
    private final Setting<Integer> range = config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64));
    private final Setting<Boolean> pvp = config.define(Setting.bool("pvp", Scope.SERVER, false));
    private final Setting<String> motd = config.define(Setting.string("motd", Scope.SERVER, ""));
    private final ModNetwork network = new ModNetwork(config.build());
    private final ServerConfig serverConfig = network.serverConfig();

    @AfterEach
    void stop() {
        serverConfig.stop();
    }

    private static Path file(MinecraftServer server) {
        return ConfigFile.serverPath(server.getWorldPath(LevelResource.ROOT), MOD_ID);
    }

    /** Starts the server settings from a fresh file. */
    private void start(MinecraftServer server) throws IOException {
        Files.deleteIfExists(file(server));
        serverConfig.start(server);
    }

    /**
     * The permissions of a player named {@code name}, an operator of {@code level} or not ({@code
     * null}), as the server computes those of a connected player from its list of operators.
     */
    private static PermissionSet player(MinecraftServer server, String name, LevelBasedPermissionSet level) {
        NameAndId player = new NameAndId(UUID.randomUUID(), name);
        if (level != null) {
            server.getPlayerList().op(player, Optional.of(level), Optional.empty());
        }
        return server.getProfilePermissions(player);
    }

    private Optional<ConfigPayloads.Sync> change(PermissionSet player, String changes) {
        return serverConfig.change("player", player, network.payloads().change(changes));
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    @Test
    void theSettingsOfTheWorldAreLoadedWhenTheServerStarts(MinecraftServer server) throws IOException {
        Files.createDirectories(file(server).getParent());
        Files.writeString(file(server), "{\"range\": 24}");

        serverConfig.start(server);

        assertEquals(24, serverConfig.get(range));
        assertEquals(false, serverConfig.get(pvp));
    }

    @Test
    void thereAreNoServerSettingsWithoutARunningServer() {
        assertThrows(IllegalStateException.class, () -> serverConfig.get(range));
    }

    @Test
    void anOperatorChangeIsAppliedSavedAndSentToPlayers(MinecraftServer server) throws IOException {
        start(server);
        PermissionSet operator = player(server, "operator", LevelBasedPermissionSet.GAMEMASTER);
        assertTrue(ServerConfig.canEdit(operator));

        Optional<ConfigPayloads.Sync> sync = change(operator, "{\"range\": 16}");

        assertTrue(sync.isPresent());
        assertEquals(16, serverConfig.get(range));
        assertEquals(16, read(file(server)).get("range").getAsInt());
        ReceivedConfig client = network.receivedConfig();
        client.receive(sync.get());
        assertEquals(16, client.get(range));
    }

    @Test
    void aChangeFromAPlayerWhoIsNotOperatorIsRefused(MinecraftServer server) throws IOException {
        start(server);
        PermissionSet player = player(server, "player", null);
        assertFalse(ServerConfig.canEdit(player));

        Optional<ConfigPayloads.Sync> sync = change(player, "{\"range\": 16}");

        assertTrue(sync.isEmpty());
        assertEquals(8, serverConfig.get(range));
        assertEquals(8, read(file(server)).get("range").getAsInt());
    }

    @Test
    void anOperatorBelowTheLevelOfGameRulesIsRefused(MinecraftServer server) throws IOException {
        start(server);
        PermissionSet moderator = player(server, "moderator", LevelBasedPermissionSet.MODERATOR);
        assertFalse(ServerConfig.canEdit(moderator));
        assertTrue(ServerConfig.canEdit(player(server, "owner", LevelBasedPermissionSet.OWNER)));

        assertTrue(change(moderator, "{\"range\": 16}").isEmpty());
        assertEquals(8, serverConfig.get(range));
    }

    @Test
    void aChangeWithAnInvalidValueIsRefusedWhole(MinecraftServer server) throws IOException {
        start(server);
        PermissionSet operator = player(server, "operator", LevelBasedPermissionSet.GAMEMASTER);

        for (String change : new String[] {
            "{\"pvp\": true, \"range\": \"far\"}", "{\"pvp\": true, \"range\": 100}", "{\"pvp\": true, \"other\": 1}",
            "[true]", "{\"pvp\": tru"
        }) {
            assertTrue(change(operator, change).isEmpty(), change);
        }

        assertEquals(false, serverConfig.get(pvp));
        assertEquals(8, serverConfig.get(range));
    }

    @Test
    void theSettingsSentToPlayersAreTheRunningOnes(MinecraftServer server) throws IOException {
        start(server);
        change(player(server, "operator", LevelBasedPermissionSet.GAMEMASTER), "{\"pvp\": true}");

        assertEquals(
                JsonParser.parseString("{\"range\": 8, \"pvp\": true, \"motd\": \"\"}"),
                JsonParser.parseString(serverConfig.sync().orElseThrow().values()));
    }

    /** Starts the server settings from a file whose {@code motd} is {@code length} characters long. */
    private void startWithMotd(MinecraftServer server, int length) throws IOException {
        Files.createDirectories(file(server).getParent());
        Files.writeString(file(server), "{\"motd\": \"" + "x".repeat(length) + "\"}");
        serverConfig.start(server);
    }

    @Test
    void settingsTooLongForTheirPayloadAreNotSent(MinecraftServer server) throws IOException {
        startWithMotd(server, ConfigPayloads.MAX_SYNC);

        assertTrue(serverConfig.sync().isEmpty());
    }

    @Test
    void aChangeMakingTheSettingsTooLongForTheirPayloadIsRefused(MinecraftServer server) throws IOException {
        // The settings fill their payload exactly: {"range":8,"pvp":false,"motd":"xx…"}.
        startWithMotd(server, ConfigPayloads.MAX_SYNC - "{\"range\":8,\"pvp\":false,\"motd\":\"\"}".length());
        PermissionSet operator = player(server, "operator", LevelBasedPermissionSet.GAMEMASTER);
        assertTrue(serverConfig.sync().isPresent());

        assertTrue(change(operator, "{\"range\": 16}").isEmpty());
        assertEquals(8, serverConfig.get(range));
        assertTrue(serverConfig.sync().isPresent());
    }
}
