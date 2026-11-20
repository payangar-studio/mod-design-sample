package com.payangar.designsample.gametest;

import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Example server gametest on NeoForge, which has no {@code @GameTest}: each
 * test is a function of the {@code test_function} registry, declared as a
 * test through {@link RegisterGameTestsEvent}. This class is the test mod of
 * src/gametest/resources/META-INF/neoforge.mods.toml.
 */
@Mod(ExampleServerGameTest.MOD_ID)
public final class ExampleServerGameTest {
    public static final String MOD_ID = "design_sample_gametest";

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, MOD_ID);

    private static final ResourceKey<Consumer<GameTestHelper>> PLACES_A_BLOCK =
            FUNCTIONS.register("places_a_block", () -> ExampleServerGameTest::placesABlock).getKey();

    public ExampleServerGameTest(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(ExampleServerGameTest::register);
    }

    private static void placesABlock(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.DIAMOND_BLOCK);
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, 1, 1, 1);
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(MOD_ID, "default"));
        event.registerTest(
                Identifier.fromNamespaceAndPath(MOD_ID, "places_a_block"),
                new FunctionGameTestInstance(
                        PLACES_A_BLOCK,
                        new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }
}
