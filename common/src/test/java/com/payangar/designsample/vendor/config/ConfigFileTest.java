package com.payangar.designsample.vendor.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFileTest {
    enum Quality {
        FAST,
        FANCY
    }

    @TempDir
    Path tmp;

    private final ConfigSchema.Builder config = ConfigSchema.builder("demo", ModSide.BOTH_REQUIRED);
    private final Setting<Boolean> showHud = config.define(Setting.bool("show_hud", Scope.CLIENT, true));
    private final Setting<Integer> range = config.define(Setting.integer("range", Scope.SERVER, 8, 1, 64));
    private final Setting<Double> speed = config.define(Setting.decimal("speed", Scope.SERVER, 1.0, 0.5, 2.0));
    private final Setting<Quality> quality = config.define(Setting.choice("quality", Scope.CLIENT, Quality.FANCY));
    private final Setting<String> title = config.define(Setting.string("title", Scope.CLIENT, "Hello"));
    private final Setting<Integer> tint = config.define(Setting.colour("tint", Scope.CLIENT, 0x336699, false));
    private final Setting<Integer> glow = config.define(Setting.colour("glow", Scope.CLIENT, 0x80FF0000, true));
    private final ConfigSchema schema = config.build();

    @Test
    void defaultsAreWrittenOnFirstLoad() throws IOException {
        Path file = ConfigFile.clientPath(tmp.resolve("config"), "demo");
        ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.CLIENT, file);

        assertEquals(List.of(), loaded.problems());
        assertEquals(tmp.resolve("config/demo.json"), file);
        JsonObject json = read(file);
        assertEquals(List.of("show_hud", "quality", "title", "tint", "glow"), List.copyOf(json.keySet()));
        assertTrue(json.get("show_hud").getAsBoolean());
        assertEquals("fancy", json.get("quality").getAsString());
        assertEquals("Hello", json.get("title").getAsString());
        assertEquals("#336699", json.get("tint").getAsString());
        assertEquals("#80FF0000", json.get("glow").getAsString());
        assertEquals(0xFF336699, loaded.values().get(tint));
    }

    @Test
    void missingKeysTakeTheirDefault() throws IOException {
        Path file = ConfigFile.serverPath(tmp.resolve("world"), "demo");
        write(file, "{\"range\": 12}");
        ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.SERVER, file);

        assertEquals(List.of(), loaded.problems());
        assertEquals(12, loaded.values().get(range));
        assertEquals(1.0, loaded.values().get(speed));
        assertEquals(1.0, read(file).get("speed").getAsDouble());
    }

    @Test
    void valuesOutOfBoundsAreClampedAndReported() throws IOException {
        Path file = ConfigFile.serverPath(tmp.resolve("world"), "demo");
        write(file, "{\"range\": 500, \"speed\": 0.1}");
        ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.SERVER, file);

        assertEquals(64, loaded.values().get(range));
        assertEquals(0.5, loaded.values().get(speed));
        assertEquals(2, loaded.problems().size(), loaded.problems().toString());
        assertTrue(loaded.problems().get(0).contains("`range`: 500 is above the maximum 64, set to 64"), loaded.problems().get(0));
        assertTrue(loaded.problems().get(1).contains("`speed`: 0.1 is below the minimum 0.5, set to 0.5"), loaded.problems().get(1));
        JsonObject json = read(file);
        assertEquals(64, json.get("range").getAsInt());
        assertEquals(0.5, json.get("speed").getAsDouble());
    }

    @Test
    void valuesOfTheWrongTypeTakeTheDefaultAndAreReported() throws IOException {
        Path file = ConfigFile.clientPath(tmp, "demo");
        write(file, """
                {"show_hud": "yes", "quality": "ultra", "title": 3, "tint": "#12345", "glow": "#AA00FF00"}""");
        ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.CLIENT, file);

        assertEquals(4, loaded.problems().size(), loaded.problems().toString());
        assertEquals(true, loaded.values().get(showHud));
        assertEquals(Quality.FANCY, loaded.values().get(quality));
        assertEquals("Hello", loaded.values().get(title));
        assertEquals(0xFF336699, loaded.values().get(tint));
        assertEquals(0xAA00FF00, loaded.values().get(glow));
        assertEquals("fancy", read(file).get("quality").getAsString());
    }

    @Test
    void aWholeNumberSettingRefusesAFraction() throws IOException {
        Path file = ConfigFile.serverPath(tmp, "demo");
        write(file, "{\"range\": 8.5, \"speed\": 1}");
        ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.SERVER, file);

        assertEquals(1, loaded.problems().size(), loaded.problems().toString());
        assertEquals(8, loaded.values().get(range));
        assertEquals(1.0, loaded.values().get(speed));
    }

    @Test
    void unknownKeysAreKept() throws IOException {
        Path file = ConfigFile.clientPath(tmp, "demo");
        write(file, "{\"show_hud\": false, \"removed\": {\"nested\": [1, 2]}}");
        ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.CLIENT, file);
        loaded.values().set(title, "Changed");
        ConfigFile.save(loaded.values(), file);

        JsonObject json = read(file);
        assertEquals(JsonParser.parseString("{\"nested\": [1, 2]}"), json.get("removed"));
        assertFalse(json.get("show_hud").getAsBoolean());
        assertEquals("Changed", json.get("title").getAsString());
    }

    @Test
    void valuesSurviveARoundTrip() throws IOException {
        Path client = ConfigFile.clientPath(tmp, "demo");
        ConfigValues values = ConfigFile.load(schema, Scope.CLIENT, client).values();
        values.set(showHud, false);
        values.set(quality, Quality.FAST);
        values.set(title, "Quote \" and <tag> and é");
        values.set(tint, 0x00ABCDEF);
        values.set(glow, 0x12345678);
        ConfigFile.save(values, client);

        Path server = ConfigFile.serverPath(tmp, "demo");
        ConfigValues rules = ConfigFile.load(schema, Scope.SERVER, server).values();
        rules.set(range, 33);
        rules.set(speed, 1.25);
        ConfigFile.save(rules, server);

        ConfigFile.Loaded again = ConfigFile.load(schema, Scope.CLIENT, client);
        assertEquals(List.of(), again.problems());
        assertEquals(false, again.values().get(showHud));
        assertEquals(Quality.FAST, again.values().get(quality));
        assertEquals("Quote \" and <tag> and é", again.values().get(title));
        assertEquals(0xFFABCDEF, again.values().get(tint));
        assertEquals(0x12345678, again.values().get(glow));
        ConfigFile.Loaded rulesAgain = ConfigFile.load(schema, Scope.SERVER, server);
        assertEquals(List.of(), rulesAgain.problems());
        assertEquals(33, rulesAgain.values().get(range));
        assertEquals(1.25, rulesAgain.values().get(speed));
    }

    @Test
    void settingAValueClampsIt() {
        ConfigValues values = new ConfigValues(schema, Scope.SERVER);
        assertEquals(64, values.set(range, 1000));
        assertEquals(0.5, values.set(speed, -3.0));
        assertEquals(64, values.get(range));
    }

    @Test
    void eachScopeHasItsOwnFile() throws IOException {
        Path configDir = tmp.resolve("config");
        Path worldDir = tmp.resolve("saves/My World");
        Path client = ConfigFile.clientPath(configDir, "demo");
        Path server = ConfigFile.serverPath(worldDir, "demo");
        ConfigFile.load(schema, Scope.CLIENT, client);
        ConfigFile.load(schema, Scope.SERVER, server);

        assertEquals(worldDir.resolve("serverconfig/demo.json"), server);
        assertEquals(List.of("range", "speed"), List.copyOf(read(server).keySet()));
        assertFalse(read(client).has("range"));
    }

    @Test
    void serverSettingsArePerWorld() throws IOException {
        Path first = ConfigFile.serverPath(tmp.resolve("saves/first"), "demo");
        Path second = ConfigFile.serverPath(tmp.resolve("saves/second"), "demo");
        assertNotEquals(first, second);
        ConfigValues firstRules = ConfigFile.load(schema, Scope.SERVER, first).values();
        firstRules.set(range, 3);
        ConfigFile.save(firstRules, first);

        assertEquals(8, ConfigFile.load(schema, Scope.SERVER, second).values().get(range));
        assertEquals(3, ConfigFile.load(schema, Scope.SERVER, first).values().get(range));
    }

    @Test
    void aBrokenFileIsReportedAndLeftAsItIs() throws IOException {
        Path file = ConfigFile.clientPath(tmp, "demo");
        write(file, "{\"show_hud\": false,,,");
        ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.CLIENT, file);

        assertEquals(1, loaded.problems().size());
        assertEquals(true, loaded.values().get(showHud));
        assertEquals("{\"show_hud\": false,,,", Files.readString(file));
    }

    @Test
    void aFileNotInUtf8IsReportedAndLeftAsItIs() throws IOException {
        Path file = ConfigFile.clientPath(tmp, "demo");
        byte[] latin1 = "{\"title\": \"Café\"}".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file, latin1);
        ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.CLIENT, file);

        assertEquals(1, loaded.problems().size(), loaded.problems().toString());
        assertTrue(loaded.problems().get(0).contains("is not UTF-8 text"), loaded.problems().get(0));
        assertEquals("Hello", loaded.values().get(title));
        assertArrayEquals(latin1, Files.readAllBytes(file));
    }

    @Test
    void aBrokenFileIsNotOverwrittenByASave() throws IOException {
        Path file = ConfigFile.clientPath(tmp, "demo");
        write(file, "{\"show_hud\": false,,,");
        ConfigValues values = ConfigFile.load(schema, Scope.CLIENT, file).values();
        values.set(title, "Changed");

        IOException error = assertThrows(IOException.class, () -> ConfigFile.save(values, file));
        assertTrue(error.getMessage().contains("Fix it or delete it"), error.getMessage());
        assertEquals("{\"show_hud\": false,,,", Files.readString(file));
    }

    @Test
    void aFolderThatCannotBeWrittenIsReportedAndTheValuesAreUsed() throws IOException {
        assumeTrue(posix());
        Path file = ConfigFile.serverPath(tmp, "demo");
        write(file, "{\"range\": 12}");
        Path folder = file.getParent();
        Files.setPosixFilePermissions(folder, PosixFilePermissions.fromString("r-xr-xr-x"));
        try {
            assumeFalse(Files.isWritable(folder), "the folder stays writable, as for root");
            ConfigFile.Loaded loaded = ConfigFile.load(schema, Scope.SERVER, file);
            assertEquals(12, loaded.values().get(range));
            assertEquals(1.0, loaded.values().get(speed));
            assertEquals(1, loaded.problems().size(), loaded.problems().toString());
            assertTrue(loaded.problems().get(0).contains("could not be written"), loaded.problems().get(0));
            assertEquals("{\"range\": 12}", Files.readString(file));

            Path missing = folder.resolve("other.json");
            ConfigFile.Loaded fresh = ConfigFile.load(schema, Scope.SERVER, missing);
            assertEquals(8, fresh.values().get(range));
            assertEquals(1, fresh.problems().size(), fresh.problems().toString());
            assertFalse(Files.exists(missing));
        } finally {
            Files.setPosixFilePermissions(folder, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    @Test
    void aRewriteKeepsThePermissionsOfTheFile() throws IOException {
        assumeTrue(posix());
        Path file = ConfigFile.clientPath(tmp, "demo");
        write(file, "{\"show_hud\": false}");
        Set<PosixFilePermission> shared = PosixFilePermissions.fromString("rw-rw-r--");
        Files.setPosixFilePermissions(file, shared);
        ConfigFile.load(schema, Scope.CLIENT, file);

        assertTrue(read(file).has("title"));
        assertEquals(shared, Files.getPosixFilePermissions(file));
    }

    @Test
    void aNewFileHasTheDefaultPermissions() throws IOException {
        assumeTrue(posix());
        Path reference = Files.createFile(tmp.resolve("reference"));
        Path file = ConfigFile.clientPath(tmp, "demo");
        ConfigFile.load(schema, Scope.CLIENT, file);

        assertEquals(Files.getPosixFilePermissions(reference), Files.getPosixFilePermissions(file));
    }

    @Test
    void aRewriteWritesThroughASymbolicLink() throws IOException {
        assumeTrue(posix());
        Path target = tmp.resolve("managed/demo.json");
        write(target, "{\"show_hud\": false}");
        Path link = ConfigFile.clientPath(tmp.resolve("config"), "demo");
        Files.createDirectories(link.getParent());
        Files.createSymbolicLink(link, target);
        ConfigFile.load(schema, Scope.CLIENT, link);

        assertTrue(Files.isSymbolicLink(link));
        assertTrue(read(target).has("title"));
        assertFalse(read(target).get("show_hud").getAsBoolean());
    }

    private static boolean posix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }
}
