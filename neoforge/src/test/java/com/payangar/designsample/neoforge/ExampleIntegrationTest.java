package com.payangar.designsample.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.payangar.designsample.CommonEntrypoint;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Example integration test on NeoForge: JUnit with the game and the mod
 * loaded. A test that takes a {@link MinecraftServer} gets an ephemeral one,
 * started once for the whole run, with the data packs loaded.
 */
@ExtendWith(EphemeralTestServerProvider.class)
class ExampleIntegrationTest {
    @Test
    void theModIsLoaded() {
        assertTrue(ModList.get().isLoaded(CommonEntrypoint.MOD_ID));
    }

    @Test
    void diamondsStackToSixtyFour(MinecraftServer server) {
        assertEquals(64, new ItemStack(Items.DIAMOND).getMaxStackSize());
    }
}
