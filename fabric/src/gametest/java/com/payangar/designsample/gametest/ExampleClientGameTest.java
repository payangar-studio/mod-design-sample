package com.payangar.designsample.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotComparisonOptions;

/**
 * Example client gametest: a scenario played on a real client, here a new
 * flat world compared with the reference render
 * src/gametest/resources/templates/example_world.png. The comparison saves
 * the render it took to build/run/clientGameTest/screenshots/: references are
 * renders taken in CI, promoted with `./gradlew :fabric:promoteScreenshots`.
 * Its class is a {@code fabric-client-gametest} entrypoint of
 * src/gametest/resources/fabric.mod.json.
 */
public class ExampleClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.assertScreenshotEquals(
                    TestScreenshotComparisonOptions.of("example_world").save().disableCounterPrefix());
        }
    }
}
