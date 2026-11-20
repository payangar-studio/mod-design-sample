package com.payangar.designsample.vendor.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.payangar.designsample.vendor.config.ConfigSchema;
import com.payangar.designsample.vendor.config.ModSide;
import com.payangar.designsample.vendor.config.Scope;
import com.payangar.designsample.vendor.config.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.junit.jupiter.api.Test;

class ReceivedConfigTest {
    private final ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.BOTH_OPTIONAL);
    private final Setting<Integer> range = config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64));
    private final Setting<Boolean> pvp = config.define(Setting.bool("pvp", Scope.SERVER, false));
    private final Setting<String> motd = config.define(Setting.string("motd", Scope.SERVER, ""));
    private final ModNetwork network = new ModNetwork(config.build());
    private final ReceivedConfig received = network.receivedConfig();
    private final List<CustomPacketPayload> sent = new ArrayList<>();
    private boolean serverListens = true;

    ReceivedConfigTest() {
        received.connect(() -> serverListens, sent::add);
    }

    @Test
    void theDefaultsHoldUntilTheServerSendsItsSettings() {
        assertFalse(received.synced());
        assertEquals(8, received.get(range));

        received.receive(network.payloads().sync("{\"range\": 16, \"pvp\": true}"));

        assertTrue(received.synced());
        assertEquals(16, received.get(range));
        assertEquals(true, received.get(pvp));
    }

    @Test
    void leavingTheServerBringsBackTheDefaults() {
        received.receive(network.payloads().sync("{\"range\": 16}"));
        received.reset();

        assertFalse(received.synced());
        assertEquals(8, received.get(range));
    }

    @Test
    void aValueTheClientCannotReadTakesItsDefault() {
        received.receive(network.payloads().sync("{\"range\": \"far\", \"pvp\": true, \"newer\": 1}"));

        assertTrue(received.synced());
        assertEquals(8, received.get(range));
        assertEquals(true, received.get(pvp));
    }

    @Test
    void aRequestSendsOnlyWhatChanged() {
        received.receive(network.payloads().sync("{\"range\": 16, \"pvp\": false}"));
        ReceivedConfig.Edit edit = received.edit();
        edit.values().set(pvp, true);

        assertTrue(received.request(edit));

        assertEquals(1, sent.size());
        ConfigPayloads.Change change = (ConfigPayloads.Change) sent.get(0);
        assertEquals(network.payloads().changeType(), change.type());
        assertEquals(JsonParser.parseString("{\"pvp\": true}"), JsonParser.parseString(change.changes()));
        // The received settings change when the server sends them back.
        assertEquals(false, received.get(pvp));
    }

    @Test
    void nothingIsSentWhenNothingChanged() {
        received.receive(network.payloads().sync("{\"range\": 16}"));

        assertFalse(received.request(received.edit()));
        assertEquals(List.of(), sent);
    }

    @Test
    void nothingIsSentBeforeTheServerSentItsSettings() {
        ReceivedConfig.Edit edit = received.edit();
        edit.values().set(range, 32);

        assertFalse(received.request(edit));
        assertEquals(List.of(), sent);
    }

    @Test
    void nothingIsSentToAServerWithoutTheMod() {
        received.receive(network.payloads().sync("{\"range\": 16}"));
        serverListens = false;
        ReceivedConfig.Edit edit = received.edit();
        edit.values().set(range, 32);

        assertFalse(received.request(edit));
        assertEquals(List.of(), sent);
    }

    @Test
    void aRequestKeepsWhatAnotherChangeMovedSinceTheEditBegan() {
        received.receive(network.payloads().sync("{\"range\": 8, \"pvp\": false}"));
        ReceivedConfig.Edit edit = received.edit();
        // Another operator turns pvp on while this one edits the range.
        received.receive(network.payloads().sync("{\"range\": 8, \"pvp\": true}"));
        edit.values().set(range, 16);

        assertTrue(received.request(edit));

        ConfigPayloads.Change change = (ConfigPayloads.Change) sent.get(0);
        assertEquals(JsonParser.parseString("{\"range\": 16}"), JsonParser.parseString(change.changes()));
    }

    @Test
    void anEditBegunOnAnEarlierConnectionIsNotSent() {
        received.receive(network.payloads().sync("{\"range\": 8}"));
        ReceivedConfig.Edit edit = received.edit();
        received.reset();
        received.receive(network.payloads().sync("{\"range\": 8}"));
        edit.values().set(range, 16);

        assertFalse(received.request(edit));
        assertEquals(List.of(), sent);
    }

    @Test
    void aChangeTooLongForItsPayloadIsNotSent() {
        received.receive(network.payloads().sync("{\"range\": 8}"));
        ReceivedConfig.Edit edit = received.edit();
        edit.values().set(motd, "x".repeat(ConfigPayloads.MAX_CHANGE));

        assertFalse(received.request(edit));
        assertEquals(List.of(), sent);
    }
}
