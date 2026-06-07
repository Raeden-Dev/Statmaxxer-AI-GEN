package com.raeden.ors_to_do.dependencies.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.TaskItem;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StorageManager {

    private static final String APP_DIR = System.getenv("APPDATA") + File.separator + "TaskTracker";

    /**
     * Absolute path of the directory where the app keeps {@code tasks.json}, {@code stats.json},
     * their rolling backups, and any user-imported bundles. Exposed so settings UI can open the
     * folder in the OS file explorer.
     */
    public static File getDataDirectory() {
        return new File(APP_DIR);
    }

    private static final String DATA_FILE = APP_DIR + File.separator + "tasks.json";
    private static final String STATS_FILE = APP_DIR + File.separator + "stats.json";

    private static final String LEGACY_DATA_FILE = APP_DIR + File.separator + "tasks.dat";
    private static final String LEGACY_STATS_FILE = APP_DIR + File.separator + "stats.dat";

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
            .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, typeOfSrc, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME)))
            .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, typeOfT, context) -> LocalTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_TIME))
            .create();

    private static void safeSaveJson(Object data, String baseFilename) {
        File directory = new File(APP_DIR);
        if (!directory.exists()) directory.mkdirs();

        File tempFile = new File(baseFilename + ".tmp");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Failed to write temp JSON file for " + baseFilename + ": " + e.getMessage());
            return;
        }

        try {
            for (int i = 2; i >= 1; i--) {
                File src = new File(baseFilename + ".bak" + i);
                File dest = new File(baseFilename + ".bak" + (i + 1));
                if (src.exists()) Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            File original = new File(baseFilename);
            File bak1 = new File(baseFilename + ".bak1");
            if (original.exists()) Files.move(original.toPath(), bak1.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Files.move(tempFile.toPath(), original.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved securely to JSON: " + baseFilename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static <T> T safeLoadJson(String baseFilename, Type type) {
        String[] filesToTry = { baseFilename, baseFilename + ".bak1", baseFilename + ".bak2", baseFilename + ".bak3" };

        for (String path : filesToTry) {
            File f = new File(path);
            if (f.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                    T obj = gson.fromJson(reader, type);
                    if (!path.equals(baseFilename)) {
                        System.out.println("⚠️ RECOVERED JSON FROM BACKUP: " + path);
                    }
                    return obj;
                } catch (Exception e) {
                    System.err.println("Corrupted JSON file detected: " + path + " - Attempting older backup...");
                }
            }
        }
        return null;
    }

    private static Object loadLegacyDat(String filename) {
        File f = new File(filename);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                return ois.readObject();
            } catch (Exception e) {
                System.err.println("Failed to load legacy .dat file: " + filename);
            }
        }
        return null;
    }

    public static void saveTasks(List<TaskItem> tasks) {
        safeSaveJson(tasks, DATA_FILE);
    }

    @SuppressWarnings("unchecked")
    public static List<TaskItem> loadTasks() {
        Type type = new TypeToken<List<TaskItem>>(){}.getType();
        List<TaskItem> loaded = safeLoadJson(DATA_FILE, type);

        if (loaded == null) {
            Object legacy = loadLegacyDat(LEGACY_DATA_FILE);
            if (legacy != null) {
                System.out.println("🔄 Migrating Tasks from Legacy .dat to .json format...");
                loaded = (List<TaskItem>) legacy;
            } else {
                loaded = new ArrayList<>();
            }
        }

        // --- FIXED: Compatibility Integrity Check ---
        for (TaskItem task : loaded) {
            if (task.getStatRewards() == null) task.setStatRewards(new java.util.HashMap<>());
            if (task.getStatCapRewards() == null) task.setStatCapRewards(new java.util.HashMap<>());
            if (task.getStatCosts() == null) task.setStatCosts(new java.util.HashMap<>());
            if (task.getStatPenalties() == null) task.setStatPenalties(new java.util.HashMap<>());
            if (task.getStatRequirements() == null) task.setStatRequirements(new java.util.HashMap<>());
        }

        return loaded;
    }

    public static void saveStats(AppStats stats) {
        safeSaveJson(stats, STATS_FILE);
    }

    public static AppStats loadStats() {
        AppStats loaded = safeLoadJson(STATS_FILE, AppStats.class);

        if (loaded == null) {
            Object legacy = loadLegacyDat(LEGACY_STATS_FILE);
            if (legacy != null) {
                System.out.println("🔄 Migrating AppStats from Legacy .dat to .json format...");
                loaded = (AppStats) legacy;
            } else {
                loaded = new AppStats();
            }
        }

        // --- FIXED: Initialize potentially missing fields from older versions ---
        if (loaded.getFocusStatRewards() == null) loaded.setFocusStatRewards(new java.util.HashMap<>());
        if (loaded.getUrgeQuotes() == null) loaded.setUrgeQuotes(new ArrayList<>());

        return loaded;
    }
}