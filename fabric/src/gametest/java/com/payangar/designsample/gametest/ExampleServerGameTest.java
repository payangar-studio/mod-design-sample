package com.payangar.designsample.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * Example server gametest: a method annotated {@code @GameTest} runs in a
 * world on a headless server. Its class is a {@code fabric-gametest}
 * entrypoint of src/gametest/resources/fabric.mod.json.
 */
public class ExampleServerGameTest {
    @GameTest
    public void placesABlock(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.DIAMOND_BLOCK);
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, 1, 1, 1);
        helper.succeed();
    }
}
