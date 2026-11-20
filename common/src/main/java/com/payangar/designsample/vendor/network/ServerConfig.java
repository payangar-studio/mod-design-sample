package com.payangar.designsample.vendor.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.payangar.designsample.vendor.config.ConfigFile;
import com.payangar.designsample.vendor.config.ConfigSchema;
import com.payangar.designsample.vendor.config.ConfigValues;
import com.payangar.designsample.vendor.config.Scope;
import com.payangar.designsample.vendor.config.Setting;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server settings of the running server, which owns them: loaded from the folder of its world
 * when it starts, changed by operators only, sent to its players. In singleplayer the integrated
 * server plays that role. Used on the server thread.
 */
public final class ServerConfig {
    private final ConfigSchema schema;
    private final ConfigPayloads payloads;
    private final Logger logger;
    private volatile ConfigValues values;
    private volatile Path file;
    /** The players whose change was refused for lack of permission, logged once each per server run. */
    private final Set<String> refused = ConcurrentHashMap.newKeySet();

    ServerConfig(ConfigSchema schema, ConfigPayloads payloads) {
        this.schema = schema;
        this.payloads = payloads;
        this.logger = LoggerFactory.getLogger(schema.modId());
    }

    /**
     * Whether {@code player} may change server settings: the permission vanilla asks to change a
     * game rule, which an operator has. The client checks it on its own player to show the values
     * read-only; the server checks it again on every change.
     */
    public static boolean canEdit(Player player) {
        return canEdit(player.permissions());
    }

    static boolean canEdit(PermissionSet permissions) {
        return permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * Loads the settings of {@code server}'s world, from {@code <world>/serverconfig/<modid>.json}.
     *
     * @throws UncheckedIOException when the file exists but cannot be read
     */
    public void start(MinecraftServer server) {
        Path path = ConfigFile.serverPath(server.getWorldPath(LevelResource.ROOT), schema.modId());
        try {
            values = ConfigFile.load(schema, Scope.SERVER, path).values();
        } catch (IOException e) {
            throw new UncheckedIOException(schema.modId() + ": cannot read " + path, e);
        }
        file = path;
    }

    /** Forgets the settings of the server that stopped. */
    public void stop() {
        values = null;
        file = null;
        refused.clear();
    }

    /**
     * The value of {@code setting} on the running server.
     *
     * @throws IllegalStateException when no server is running
     */
    public <T> T get(Setting<T> setting) {
        return running().get(setting);
    }

    /**
     * The settings to send to a player; empty, and logged, when they are too long for their payload
     * (a long text written by hand in the world's file).
     */
    public Optional<ConfigPayloads.Sync> sync() {
        String json = running().settingsJson().toString();
        if (json.length() > ConfigPayloads.MAX_SYNC) {
            logger.error("The server settings of {} are not sent to players: {} characters, more than the {} they"
                    + " may hold. Shorten their texts in {}.", schema.modId(), json.length(), ConfigPayloads.MAX_SYNC,
                    file);
            return Optional.empty();
        }
        return Optional.of(payloads.sync(json));
    }

    /**
     * Applies the change {@code player} asks for, when they may change server settings, every value
     * of the change is valid, and the settings stay short enough to be sent: a change is applied
     * whole or not at all. The settings are then saved to the world's file.
     *
     * @return the settings to send to every player, empty when the change is refused
     */
    public Optional<ConfigPayloads.Sync> change(ServerPlayer player, ConfigPayloads.Change change) {
        return change(player.getGameProfile().name(), player.permissions(), change);
    }

    /** {@link #change(ServerPlayer, ConfigPayloads.Change)} from the player named {@code name}. */
    Optional<ConfigPayloads.Sync> change(String name, PermissionSet permissions, ConfigPayloads.Change change) {
        if (!canEdit(permissions)) {
            // Once per player: a modified client could send a change every tick.
            if (refused.add(name)) {
                logger.warn("{} tried to change server settings of {} without the permission of an operator", name,
                        schema.modId());
            }
            return Optional.empty();
        }
        ConfigValues changed = running().copy();
        JsonObject asked = new JsonObject();
        List<String> problems;
        try {
            JsonElement json = JsonParser.parseString(change.changes());
            if (json.isJsonObject()) {
                asked = json.getAsJsonObject();
                problems = changed.read(asked);
            } else {
                problems = List.of("not a JSON object");
            }
        } catch (JsonParseException e) {
            problems = List.of("not a JSON object (" + e.getMessage() + ")");
        }
        JsonObject settings = changed.settingsJson();
        int length = settings.toString().length();
        if (problems.isEmpty() && length > ConfigPayloads.MAX_SYNC) {
            problems = List.of("the settings would be " + length + " characters long, more than the "
                    + ConfigPayloads.MAX_SYNC + " sent to players may hold");
        }
        if (!problems.isEmpty()) {
            logger.warn("Refused the change of server settings of {} from {}: {}", schema.modId(), name,
                    String.join("; ", problems));
            return Optional.empty();
        }
        values = changed;
        try {
            ConfigFile.save(changed, file);
        } catch (IOException e) {
            logger.error("The server settings of {} changed by {} apply until the server stops, but were not"
                    + " saved: {}", schema.modId(), name, e.getMessage());
        }
        JsonObject applied = new JsonObject();
        for (String key : asked.keySet()) {
            applied.add(key, settings.get(key));
        }
        logger.info("{} changed server settings of {}: {}", name, schema.modId(), applied);
        return sync();
    }

    private ConfigValues running() {
        ConfigValues current = values;
        if (current == null) {
            throw new IllegalStateException(schema.modId() + ": no server is running, so there are no server settings");
        }
        return current;
    }
}
