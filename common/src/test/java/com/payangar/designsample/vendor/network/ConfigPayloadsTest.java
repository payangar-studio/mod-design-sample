package com.payangar.designsample.vendor.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.payangar.designsample.vendor.config.ConfigSchema;
import com.payangar.designsample.vendor.config.ModSide;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;

class ConfigPayloadsTest {
    private final ConfigPayloads payloads =
            new ModNetwork(ConfigSchema.builder("demo", ModSide.BOTH_REQUIRED).build()).payloads();

    @Test
    void payloadsAreNamedWithTheModId() {
        assertEquals("demo:vendor/config_sync", payloads.syncType().id().toString());
        assertEquals("demo:vendor/config_change", payloads.changeType().id().toString());
        assertEquals("demo:vendor/presence", payloads.presenceType().id().toString());
    }

    @Test
    void payloadsComeBackAsTheyWereSent() {
        ConfigPayloads.Sync sync = payloads.sync("{\"range\":16,\"title\":\"Épée\"}");
        assertEquals(sync, roundTrip(payloads.syncCodec(), sync));

        ConfigPayloads.Change change = payloads.change("{\"range\":32}");
        assertEquals(change, roundTrip(payloads.changeCodec(), change));

        ConfigPayloads.Presence presence = new ConfigPayloads.Presence(payloads.presenceType());
        assertEquals(presence, roundTrip(payloads.presenceCodec(), presence));
    }

    private static <T> T roundTrip(StreamCodec<ByteBuf, T> codec, T value) {
        ByteBuf buffer = Unpooled.buffer();
        codec.encode(buffer, value);
        T decoded = codec.decode(buffer);
        assertEquals(0, buffer.readableBytes());
        return decoded;
    }
}
