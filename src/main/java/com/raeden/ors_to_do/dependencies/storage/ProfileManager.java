package com.raeden.ors_to_do.dependencies.storage;

import com.google.gson.Gson;
import com.raeden.ors_to_do.dependencies.models.Profile;
import com.raeden.ors_to_do.dependencies.storage.sqlite.GsonProvider;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages the set of data {@link Profile}s and which one is active. The registry lives in
 * {@code profiles.json} alongside the databases (NOT inside any per-profile DB, so it survives
 * profile switches). The active profile drives which database {@link StorageManager} opens.
 *
 * <p>The built-in {@code default} profile always exists and maps to the original
 * {@code tasktracker.db}, so upgrading installs keep their data on the default profile.</p>
 */
public final class ProfileManager {

    public static final String DEFAULT_ID = "default";
    private static final String REGISTRY_FILE = "profiles.json";

    private static final Gson GSON = GsonProvider.compact();

    /** Serialized shape of {@code profiles.json}. */
    private static class Registry {
        List<Profile> profiles = new ArrayList<>();
        String activeId = DEFAULT_ID;
    }

    private static Registry cache;

    private ProfileManager() { }

    /** Loads the registry and points {@link StorageManager} at the active profile. Call once at startup. */
    public static synchronized void init() {
        load();
        StorageManager.useProfile(cache.activeId);
    }

    private static synchronized void load() {
        File f = registryFile();
        if (f.exists()) {
            try (FileReader r = new FileReader(f)) {
                Registry loaded = GSON.fromJson(r, Registry.class);
                if (loaded != null && loaded.profiles != null && !loaded.profiles.isEmpty()) {
                    cache = loaded;
                    ensureDefault();
                    if (find(cache.activeId) == null) cache.activeId = cache.profiles.get(0).getId();
                    return;
                }
            } catch (Exception ignore) { }
        }
        // Fresh registry with just the default profile.
        cache = new Registry();
        cache.profiles.add(new Profile(DEFAULT_ID, "Default"));
        cache.activeId = DEFAULT_ID;
        save();
    }

    private static void ensureDefault() {
        if (find(DEFAULT_ID) == null) cache.profiles.add(0, new Profile(DEFAULT_ID, "Default"));
    }

    private static synchronized void save() {
        try {
            File dir = StorageManager.getDataDirectory();
            if (!dir.exists()) dir.mkdirs();
            try (FileWriter w = new FileWriter(new File(dir, REGISTRY_FILE))) {
                GSON.toJson(cache, w);
            }
        } catch (Exception e) {
            System.err.println("[ProfileManager] save failed: " + e.getMessage());
        }
    }

    private static File registryFile() {
        return new File(StorageManager.getDataDirectory(), REGISTRY_FILE);
    }

    // ------------------------------------------------------------------ queries

    public static synchronized List<Profile> getProfiles() {
        if (cache == null) load();
        return new ArrayList<>(cache.profiles);
    }

    public static synchronized String getActiveId() {
        if (cache == null) load();
        return cache.activeId;
    }

    public static synchronized Profile getActiveProfile() {
        Profile p = find(getActiveId());
        return p != null ? p : cache.profiles.get(0);
    }

    private static Profile find(String id) {
        if (id == null) return null;
        for (Profile p : cache.profiles) if (id.equals(p.getId())) return p;
        return null;
    }

    // ------------------------------------------------------------------ mutations

    /** Creates a new (empty) profile and returns it. Does not switch to it. */
    public static synchronized Profile createProfile(String name) {
        if (cache == null) load();
        String id = UUID.randomUUID().toString().replace("-", "");
        Profile p = new Profile(id, name == null || name.isBlank() ? "Profile" : name.trim());
        cache.profiles.add(p);
        save();
        return p;
    }

    public static synchronized void renameProfile(String id, String name) {
        Profile p = find(id);
        if (p != null && name != null && !name.isBlank()) {
            p.setName(name.trim());
            save();
        }
    }

    /**
     * Deletes a profile and its database file. The {@code default} profile, the active profile, and
     * the last remaining profile cannot be deleted.
     *
     * @return true if deleted.
     */
    public static synchronized boolean deleteProfile(String id) {
        if (cache == null) load();
        if (DEFAULT_ID.equals(id)) return false;
        if (id == null || id.equals(cache.activeId)) return false;
        if (cache.profiles.size() <= 1) return false;
        Profile p = find(id);
        if (p == null) return false;
        cache.profiles.remove(p);
        save();
        // Best-effort removal of the profile's database file.
        try {
            String safe = id.replaceAll("[^a-zA-Z0-9_-]", "");
            File dbFile = new File(StorageManager.getDataDirectory(), "tasktracker_" + safe + ".db");
            if (dbFile.exists()) dbFile.delete();
        } catch (Exception ignore) { }
        return true;
    }

    /** Records {@code id} as active and repoints {@link StorageManager} at its database. */
    public static synchronized void setActive(String id) {
        if (cache == null) load();
        if (find(id) == null) return;
        cache.activeId = id;
        save();
        StorageManager.useProfile(id);
    }
}
