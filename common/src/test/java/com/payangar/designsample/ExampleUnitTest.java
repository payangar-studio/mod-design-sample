package com.payangar.designsample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Example unit test: JUnit, with the game's registries loaded. Item
 * components come from data packs: a test that needs them is a gametest.
 */
class ExampleUnitTest {
    @BeforeAll
    static void bootstrap() {
        GameBootstrap.run();
    }

    @Test
    void diamondIsRegistered() {
        assertEquals("minecraft:diamond", BuiltInRegistries.ITEM.getKey(Items.DIAMOND).toString());
    }
}
