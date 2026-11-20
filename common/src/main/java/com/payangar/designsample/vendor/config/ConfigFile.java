package com.payangar.designsample.vendor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Where the settings of each scope are stored, and the JSON file itself.
 *
 * <ul>
 *   <li>client scope: {@code <config dir>/<modid>.json}, the loader's config directory;
 *   <li>server scope: {@code <world dir>/serverconfig/<modid>.json}, in the folder of the world.
 * </ul>
 */
public final class ConfigFile {
    /** The folder of the server settings inside the world folder. */
    public static final String SERVER_DIR = "serverconfig";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ConfigFile() {}

    /** The file of the client settings, in the loader's config directory {@code configDir}. */
    public static Path clientPath(Path configDir, String modId) {
        return configDir.resolve(modId + ".json");
    }

    /** The file of the server settings of the world whose folder is {@code worldDir}. */
    public static Path serverPath(Path worldDir, String modId) {
        return worldDir.resolve(SERVER_DIR).resolve(modId + ".json");
    }

    /** The settings of a scope as loaded, and what was wrong with the file. */
    public record Loaded(ConfigValues values, List<String> problems) {}

    /**
     * Loads the settings of {@code scope} from {@code file}.
     *
     * <ul>
     *   <li>A missing file is written with the defaults.
     *   <li>A missing key takes its default. A value out of bounds is clamped, a value of the wrong
     *       type takes the default; both are reported. The file is then written back with the
     *       values in use.
     *   <li>A key no setting of the scope declares is kept as it is.
     *   <li>A file that is not a JSON object in UTF-8 is reported and left untouched; the defaults
     *       are used, and {@link #save} refuses to write them over it.
     *   <li>A file that cannot be written (read-only folder) is reported; the values read are used.
     * </ul>
     *
     * Each problem is also logged as a warning.
     *
     * @throws IOException when the file exists but cannot be read
     */
    public static Loaded load(ConfigSchema schema, Scope scope, Path file) throws IOException {
        ConfigValues values = new ConfigValues(schema, scope);
        List<String> problems = new ArrayList<>();
        if (!Files.exists(file)) {
            writeBack(values, file, problems);
            report(schema, problems);
            return new Loaded(values, List.copyOf(problems));
        }
        JsonObject json;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                throw new JsonParseException("not a JSON object");
            }
            json = root.getAsJsonObject();
        } catch (CharacterCodingException e) {
            return broken(values, file, "is not UTF-8 text");
        } catch (JsonParseException e) {
            return broken(values, file, "is not a JSON object (" + e.getMessage() + ")");
        }

        boolean changed = false;
        for (Setting<?> setting : schema.settings(scope)) {
            JsonElement stored = json.get(setting.name());
            if (stored == null) {
                changed = true;
            } else {
                changed |= read(values, setting, stored, file, problems);
            }
        }
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (schema.settings(scope).stream().noneMatch(s -> s.name().equals(entry.getKey()))) {
                values.keepUnknown(entry.getKey(), entry.getValue());
            }
        }
        if (changed) {
            writeBack(values, file, problems);
        }
        report(schema, problems);
        return new Loaded(values, List.copyOf(problems));
    }

    /**
     * Writes {@code values} to {@code file}, creating its folder. The file is replaced at once: a
     * crash while writing leaves the previous one. An existing file keeps its permissions, and a
     * symbolic link is written through, to the file it points to.
     *
     * @throws IOException when the file cannot be written, or when {@code values} were loaded from
     *     a file that could not be read ({@link ConfigValues#fromBrokenFile()}): that file is left
     *     as it is
     */
    public static void save(ConfigValues values, Path file) throws IOException {
        if (values.fromBrokenFile()) {
            throw new IOException("The settings of " + values.schema().modId() + " were not read from their"
                    + " file, which is broken, so " + file + " is left as it is. Fix it or delete it, then"
                    + " restart.");
        }
        boolean exists = Files.exists(file);
        Path target = exists ? file.toRealPath() : file.toAbsolutePath();
        Path dir = target.getParent();
        Files.createDirectories(dir);
        // Not Files.createTempFile: its file is readable by its owner only, whatever the umask.
        Path temp = Files.createFile(dir.resolve(target.getFileName() + "." + UUID.randomUUID() + ".tmp"));
        try {
            Files.writeString(temp, GSON.toJson(values.toJson()) + "\n", StandardCharsets.UTF_8);
            PosixFileAttributeView permissions = Files.getFileAttributeView(target, PosixFileAttributeView.class);
            if (exists && permissions != null) {
                Files.setPosixFilePermissions(temp, permissions.readAttributes().permissions());
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** The defaults, for a file that could not be read; it is left as it is. */
    private static Loaded broken(ConfigValues values, Path file, String why) {
        values.markFromBrokenFile();
        List<String> problems = List.of(file + " " + why + ": defaults are used, and the file is left as it"
                + " is. Fix it or delete it, then restart.");
        report(values.schema(), problems);
        return new Loaded(values, problems);
    }

    /**
     * Writes back the values in use. The values are valid whether or not this succeeds, so a
     * failure (a read-only folder) is reported rather than thrown.
     */
    private static void writeBack(ConfigValues values, Path file, List<String> problems) {
        try {
            save(values, file);
        } catch (IOException e) {
            problems.add(file + " could not be written (" + e + "): the values in use stay, and the file"
                    + " is left as it is.");
        }
    }

    /** Reads {@code stored} into {@code values}; whether the file must be written back. */
    private static <T> boolean read(
            ConfigValues values, Setting<T> setting, JsonElement stored, Path file, List<String> problems) {
        SettingType.Reading<T> reading = setting.type().read(stored);
        if (reading.value() == null) {
            problems.add(file + ": `" + setting.name() + "`: " + reading.problem() + ", set to the default "
                    + setting.type().write(setting.defaultValue()));
            return true;
        }
        values.set(setting, reading.value());
        if (reading.problem() != null) {
            problems.add(file + ": `" + setting.name() + "`: " + reading.problem());
            return true;
        }
        return false;
    }

    private static void report(ConfigSchema schema, List<String> problems) {
        if (problems.isEmpty()) {
            return;
        }
        Logger logger = LoggerFactory.getLogger(schema.modId());
        problems.forEach(logger::warn);
    }
}
