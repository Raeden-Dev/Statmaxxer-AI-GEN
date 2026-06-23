package com.raeden.ors_to_do.modules.dependencies.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Google Drive sync for all Statmaxxer data files (the SQLite databases under the app data folder),
 * mirrored into a {@code statmaxxer-data/} folder in the connected account's Drive.
 *
 * <p><b>Async, single-writer:</b> every Drive operation runs on a single-thread executor, so uploads
 * and downloads are serialized — there can never be two simultaneous writes to the same remote file.
 *
 * <p><b>Credentials are not bundled.</b> A "Desktop app" OAuth client must be created in a Google
 * Cloud project and its {@code client_secret} JSON placed at
 * {@code %APPDATA%/TaskTracker/google_client_secret.json}. Until that file exists the manager reports
 * {@link #isConfigured()} = false and the Settings UI shows a "not configured" state; the rest of the
 * app is unaffected. See {@code google_drive_setup.txt} bundled in resources for the steps.
 */
public final class GoogleDriveSyncManager {

    public static final String DATA_FOLDER_NAME = "statmaxxer-data";
    private static final String APPLICATION_NAME = "Statmaxxer";
    private static final String CLIENT_SECRET_FILE = "google_client_secret.json";
    private static final String TOKENS_DIR = "google_tokens";
    // Last-sync time is kept in a small local file (NOT in the synced DB) so recording a sync can't
    // mark the data dirty and trigger another upload.
    private static final String LAST_SYNC_FILE = "google_last_sync";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    /** Serializes all Drive I/O so writes never overlap. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "gdrive-sync");
        t.setDaemon(true);
        return t;
    });

    private static volatile Drive driveService;
    /** Epoch millis of the last successful sync; -1 = unknown/never (lazily loaded from disk). */
    private static volatile long lastSyncMillis = -1;

    private GoogleDriveSyncManager() { }

    // ------------------------------------------------------------------
    // Configuration / connection state
    // ------------------------------------------------------------------

    /** The app data directory (same one the SQLite DBs live in). */
    private static java.io.File appDir() {
        return com.raeden.ors_to_do.dependencies.storage.StorageManager.getDataDirectory();
    }

    private static java.io.File clientSecretFile() {
        return new java.io.File(appDir(), CLIENT_SECRET_FILE);
    }

    private static java.io.File tokensDir() {
        return new java.io.File(appDir(), TOKENS_DIR);
    }

    /** True when the user has supplied their OAuth client_secret.json. */
    public static boolean isConfigured() {
        java.io.File f = clientSecretFile();
        return f.exists() && f.length() > 0;
    }

    /** True when a credential has been stored (the user completed the Google sign-in). */
    public static boolean isConnected() {
        java.io.File store = new java.io.File(tokensDir(), "StoredCredential");
        return store.exists() && store.length() > 0;
    }

    // ------------------------------------------------------------------
    // Connect / disconnect
    // ------------------------------------------------------------------

    /**
     * Runs the OAuth desktop sign-in flow on the sync thread, stores the user's email on success,
     * and performs an initial upload. All callbacks fire on the sync thread; callers that touch the
     * UI must marshal back to the FX thread themselves.
     *
     * @param onEmail   receives the connected account email on success
     * @param onError   receives a human-readable message on failure / when not configured
     */
    public static void connect(Consumer<String> onEmail, Consumer<String> onError) {
        if (!isConfigured()) {
            onError.accept("Google Drive is not configured. Add your OAuth client_secret.json first.");
            return;
        }
        EXECUTOR.submit(() -> {
            try {
                Drive drive = buildService();
                About about = drive.about().get().setFields("user").execute();
                String email = about.getUser() != null ? about.getUser().getEmailAddress() : "(unknown account)";
                ensureDataFolderId(drive);
                uploadAllDataFilesBlocking(drive);
                onEmail.accept(email);
            } catch (Exception e) {
                onError.accept("Google Drive sign-in failed: " + e.getMessage());
            }
        });
    }

    /** Clears the stored credential so the next connect re-prompts. Runs on the sync thread. */
    public static void disconnect(Runnable onDone) {
        EXECUTOR.submit(() -> {
            driveService = null;
            deleteRecursively(tokensDir());
            onDone.run();
        });
    }

    // ------------------------------------------------------------------
    // Sync operations
    // ------------------------------------------------------------------

    /** Notifies the manager that local data changed; schedules an async upload when connected. */
    public static void onDataChanged() {
        if (!isConfigured() || !isConnected()) return;
        EXECUTOR.submit(() -> {
            try {
                uploadAllDataFilesBlocking(buildService());
            } catch (Exception e) {
                System.err.println("[GoogleDriveSync] upload failed: " + e.getMessage());
            }
        });
    }

    /**
     * Pushes the current local data to Drive once at app startup (async, only when connected). Lets
     * the user reopen the app without having to click "Sync Now" to get the latest local state up.
     */
    public static void syncOnStartup() {
        if (!isConfigured() || !isConnected()) return;
        EXECUTOR.submit(() -> {
            try {
                uploadAllDataFilesBlocking(buildService());
                System.out.println("[GoogleDriveSync] startup sync complete.");
            } catch (Exception e) {
                System.err.println("[GoogleDriveSync] startup sync failed: " + e.getMessage());
            }
        });
    }

    /** Explicit "Sync Now" — uploads all local data files. */
    public static void syncNow(Runnable onDone, Consumer<String> onError) {
        EXECUTOR.submit(() -> {
            try {
                uploadAllDataFilesBlocking(buildService());
                onDone.run();
            } catch (Exception e) {
                onError.accept("Sync failed: " + e.getMessage());
            }
        });
    }

    /**
     * Epoch millis of the last successful sync, or -1 if there has never been one. Lazily loaded
     * from the on-disk marker so it survives restarts.
     */
    public static long getLastSyncMillis() {
        if (lastSyncMillis < 0) {
            java.io.File f = new java.io.File(appDir(), LAST_SYNC_FILE);
            if (f.exists()) {
                try {
                    String s = new String(java.nio.file.Files.readAllBytes(f.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8).trim();
                    if (!s.isEmpty()) lastSyncMillis = Long.parseLong(s);
                } catch (Exception ignore) { /* treat as never-synced */ }
            }
        }
        return lastSyncMillis;
    }

    /** Records "now" as the last successful sync time, both in memory and on disk. */
    private static void recordSync() {
        lastSyncMillis = System.currentTimeMillis();
        try {
            java.nio.file.Files.write(new java.io.File(appDir(), LAST_SYNC_FILE).toPath(),
                    Long.toString(lastSyncMillis).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignore) { /* non-fatal: the timestamp is a convenience only */ }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static synchronized Drive buildService() throws Exception {
        if (driveService != null) return driveService;
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(transport);
        driveService = new Drive.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        return driveService;
    }

    private static Credential authorize(NetHttpTransport transport) throws Exception {
        try (Reader reader = new InputStreamReader(new java.io.FileInputStream(clientSecretFile()))) {
            GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    transport, JSON_FACTORY, secrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(tokensDir()))
                    .setAccessType("offline")
                    .build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }

    /** Finds or creates the {@value #DATA_FOLDER_NAME} folder and returns its id. */
    private static String ensureDataFolderId(Drive drive) throws Exception {
        FileList result = drive.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + DATA_FOLDER_NAME
                        + "' and trashed=false")
                .setSpaces("drive")
                .setFields("files(id,name)")
                .execute();
        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }
        File metadata = new File();
        metadata.setName(DATA_FOLDER_NAME);
        metadata.setMimeType("application/vnd.google-apps.folder");
        return drive.files().create(metadata).setFields("id").execute().getId();
    }

    /** Uploads every local data file (the SQLite DBs) into the Drive folder, upserting by name. */
    private static void uploadAllDataFilesBlocking(Drive drive) throws Exception {
        String folderId = ensureDataFolderId(drive);
        java.io.File[] dbFiles = appDir().listFiles((dir, name) -> name.endsWith(".db"));
        if (dbFiles == null) return;
        for (java.io.File local : dbFiles) {
            upsertFileBlocking(drive, folderId, local);
        }
        recordSync();
    }

    private static void upsertFileBlocking(Drive drive, String folderId, java.io.File local) throws Exception {
        FileContent media = new FileContent("application/octet-stream", local);

        FileList existing = drive.files().list()
                .setQ("name='" + local.getName() + "' and '" + folderId + "' in parents and trashed=false")
                .setSpaces("drive")
                .setFields("files(id,name)")
                .execute();

        if (existing.getFiles() != null && !existing.getFiles().isEmpty()) {
            String fileId = existing.getFiles().get(0).getId();
            drive.files().update(fileId, new File(), media).execute();
        } else {
            File metadata = new File();
            metadata.setName(local.getName());
            metadata.setParents(Collections.singletonList(folderId));
            drive.files().create(metadata, media).setFields("id").execute();
        }
    }

    private static void deleteRecursively(java.io.File f) {
        if (f == null || !f.exists()) return;
        java.io.File[] children = f.listFiles();
        if (children != null) {
            for (java.io.File c : children) deleteRecursively(c);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
