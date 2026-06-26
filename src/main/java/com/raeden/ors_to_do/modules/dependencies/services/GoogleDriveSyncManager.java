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
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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
 * <p><b>Two-way, with safety nets.</b> A per-file sync marker remembers the local + remote modified
 * times at the last successful sync, so a full sync can tell whether the local copy, the remote copy,
 * or both changed and pull / push accordingly. <b>Before any local database is overwritten by a
 * download, a timestamped backup is written to {@code cloud_sync_backups/}</b> — so a wrong-way sync
 * never loses data permanently. Save-triggered syncs are <b>push-only</b> (they never replace your
 * open database mid-session); pulls happen only on connect, startup, and the explicit "Sync Now".
 *
 * <p><b>Credentials are not bundled.</b> A "Desktop app" OAuth client must be created in a Google
 * Cloud project and its {@code client_secret} JSON placed at
 * {@code %APPDATA%/TaskTracker/google_client_secret.json}. See {@code google_drive_setup.txt}.
 */
public final class GoogleDriveSyncManager {

    public static final String DATA_FOLDER_NAME = "statmaxxer-data";
    private static final String APPLICATION_NAME = "Statmaxxer";
    private static final String CLIENT_SECRET_FILE = "google_client_secret.json";
    private static final String TOKENS_DIR = "google_tokens";
    private static final String BACKUP_DIR = "cloud_sync_backups";
    private static final String LAST_SYNC_FILE = "google_last_sync";
    private static final String SYNC_STATE_FILE = "google_sync_state.properties";
    private static final int MAX_BACKUPS_PER_FILE = 15;
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
    /**
     * Set true once an initial (bidirectional) sync has completed this run. Until then, save-triggered
     * pushes are suppressed so a freshly-opened/empty database can't overwrite good remote data before
     * we've had a chance to pull it down.
     */
    private static volatile boolean initialSyncComplete = false;

    /** Applies downloaded data on the UI thread (swap DB files + reload). Set by the app at startup. */
    private static volatile Consumer<List<PendingDownload>> reloadHandler;

    private GoogleDriveSyncManager() { }

    /** A remote file that has been downloaded to a temp path and is waiting to replace the local DB. */
    public static final class PendingDownload {
        public final java.io.File localFile;
        public final java.io.File tempFile;
        public final long remoteModifiedMillis;
        PendingDownload(java.io.File localFile, java.io.File tempFile, long remoteModifiedMillis) {
            this.localFile = localFile;
            this.tempFile = tempFile;
            this.remoteModifiedMillis = remoteModifiedMillis;
        }
    }

    // ------------------------------------------------------------------
    // Configuration / connection state
    // ------------------------------------------------------------------

    private static java.io.File appDir() {
        return com.raeden.ors_to_do.dependencies.storage.StorageManager.getDataDirectory();
    }

    private static java.io.File clientSecretFile() { return new java.io.File(appDir(), CLIENT_SECRET_FILE); }
    private static java.io.File tokensDir() { return new java.io.File(appDir(), TOKENS_DIR); }

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

    /** Registers the UI-thread handler that swaps in downloaded data and reloads the app. */
    public static void setReloadHandler(Consumer<List<PendingDownload>> handler) {
        reloadHandler = handler;
    }

    // ------------------------------------------------------------------
    // Connect / disconnect
    // ------------------------------------------------------------------

    /**
     * Runs the OAuth sign-in on the sync thread, stores the email on success, then performs a full
     * bidirectional sync (which will pull remote data down onto a fresh device).
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
                fullSyncBlocking(drive);
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
            initialSyncComplete = false;
            deleteRecursively(tokensDir());
            onDone.run();
        });
    }

    // ------------------------------------------------------------------
    // Sync operations
    // ------------------------------------------------------------------

    /**
     * Save-triggered, <b>push-only</b> sync. Never downloads (so it can't replace the database you're
     * actively editing). Suppressed until the initial bidirectional sync for this run has finished.
     */
    public static void onDataChanged() {
        if (!isConfigured() || !isConnected() || !initialSyncComplete) return;
        EXECUTOR.submit(() -> {
            try {
                pushAllBlocking(buildService());
            } catch (Exception e) {
                System.err.println("[GoogleDriveSync] upload failed: " + e.getMessage());
            }
        });
    }

    /** Full bidirectional sync once at startup (async, no-op when not connected). */
    public static void syncOnStartup() {
        if (!isConfigured() || !isConnected()) return;
        EXECUTOR.submit(() -> {
            try {
                fullSyncBlocking(buildService());
                System.out.println("[GoogleDriveSync] startup sync complete.");
            } catch (Exception e) {
                System.err.println("[GoogleDriveSync] startup sync failed: " + e.getMessage());
            }
        });
    }

    /** Explicit "Sync Now" — full bidirectional sync. */
    public static void syncNow(Runnable onDone, Consumer<String> onError) {
        EXECUTOR.submit(() -> {
            try {
                fullSyncBlocking(buildService());
                onDone.run();
            } catch (Exception e) {
                onError.accept("Sync failed: " + e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------
    // Last-sync marker
    // ------------------------------------------------------------------

    public static long getLastSyncMillis() {
        if (lastSyncMillis < 0) {
            java.io.File f = new java.io.File(appDir(), LAST_SYNC_FILE);
            if (f.exists()) {
                try {
                    String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
                    if (!s.isEmpty()) lastSyncMillis = Long.parseLong(s);
                } catch (Exception ignore) { /* treat as never-synced */ }
            }
        }
        return lastSyncMillis;
    }

    private static void recordSyncTimestamp() {
        lastSyncMillis = System.currentTimeMillis();
        try {
            Files.write(new java.io.File(appDir(), LAST_SYNC_FILE).toPath(),
                    Long.toString(lastSyncMillis).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignore) { /* non-fatal */ }
    }

    /**
     * Called by the reload handler (UI thread) after it has swapped a downloaded file into place, so
     * the per-file marker reflects the just-applied content (its checksum is now the agreed baseline).
     */
    public static void recordSynced(java.io.File local, long remoteModifiedMillis) {
        writeMarker(local.getName(), md5(local), remoteModifiedMillis);
    }

    // ------------------------------------------------------------------
    // Core sync
    // ------------------------------------------------------------------

    /**
     * Uploads every local data file whose <b>content actually changed</b> since the last sync (by MD5,
     * not timestamp). Never downloads. Skips a file when its remote copy <b>also</b> has new content
     * we haven't pulled yet (another device pushed) — overwriting would lose it; the next full sync
     * pulls it down instead.
     */
    private static void pushAllBlocking(Drive drive) throws Exception {
        String folderId = ensureDataFolderId(drive);
        java.io.File[] dbFiles = appDir().listFiles((dir, name) -> name.endsWith(".db"));
        if (dbFiles == null) return;

        java.util.Map<String, File> remote = remoteIndex(drive, folderId);
        Properties state = loadState();

        for (java.io.File local : dbFiles) {
            String name = local.getName();
            String localMd5 = md5(local);
            String syncedMd5 = state.getProperty(name + ".md5");

            // No real content change since the last sync → nothing to push.
            if (localMd5 != null && localMd5.equals(syncedMd5)) continue;

            File rf = remote.get(name);
            String remoteMd5 = rf != null ? rf.getMd5Checksum() : null;

            // Identical content already on Drive → just adopt the marker, no upload.
            if (localMd5 != null && localMd5.equals(remoteMd5)) {
                writeMarker(name, localMd5, modifiedMillis(rf));
                continue;
            }
            // Remote also changed since our last sync → don't clobber; a full sync will reconcile.
            if (rf != null && remoteMd5 != null && !remoteMd5.equals(syncedMd5)) {
                System.out.println("[GoogleDriveSync] skipping push of " + name + " — remote has new content; a full sync will pull it.");
                continue;
            }

            long remoteMod = upsertFileBlocking(drive, folderId, local);
            writeMarker(name, localMd5, remoteMod);
        }
        recordSyncTimestamp();
    }

    /** Indexes the Drive folder's data files by name, including modifiedTime, size and md5Checksum. */
    private static java.util.Map<String, File> remoteIndex(Drive drive, String folderId) throws Exception {
        FileList fl = drive.files().list()
                .setQ("'" + folderId + "' in parents and trashed=false")
                .setSpaces("drive")
                .setFields("files(id,name,modifiedTime,size,md5Checksum)")
                .execute();
        java.util.Map<String, File> m = new java.util.HashMap<>();
        if (fl.getFiles() != null) {
            for (File f : fl.getFiles()) m.put(f.getName(), f);
        }
        return m;
    }

    private static long modifiedMillis(File f) {
        return (f != null && f.getModifiedTime() != null) ? f.getModifiedTime().getValue() : System.currentTimeMillis();
    }

    /**
     * The deliberate "fetch, compare, then push or pull" sync. For each data file it compares the
     * <b>content checksum</b> of the local copy and the remote copy (Drive's md5Checksum) against the
     * checksum agreed at the last sync, so it acts only when content actually differs — not merely
     * because a timestamp moved:
     * <ul>
     *   <li>local md5 == remote md5 → already identical, do nothing;</li>
     *   <li>only the local copy changed → push it up;</li>
     *   <li>only the remote copy changed → pull it down (and reload);</li>
     *   <li>both changed (a real conflict) → remote wins, local backed up first;</li>
     *   <li>first sync with no baseline → larger database wins.</li>
     * </ul>
     * Downloads are written to temp files and handed to {@link #reloadHandler} to be swapped in (and
     * the app reloaded) on the UI thread; the replaced local file is always backed up first.
     */
    private static void fullSyncBlocking(Drive drive) throws Exception {
        String folderId = ensureDataFolderId(drive);

        java.util.Map<String, File> remoteByName = remoteIndex(drive, folderId);

        Set<String> names = new LinkedHashSet<>();
        java.io.File[] localDbs = appDir().listFiles((dir, name) -> name.endsWith(".db"));
        if (localDbs != null) for (java.io.File f : localDbs) names.add(f.getName());
        for (String n : remoteByName.keySet()) if (n.endsWith(".db")) names.add(n);

        Properties state = loadState();
        List<PendingDownload> pendings = new ArrayList<>();

        for (String name : names) {
            java.io.File local = new java.io.File(appDir(), name);
            File remote = remoteByName.get(name);

            boolean localExists = local.exists() && local.length() > 0;
            String localMd5 = localExists ? md5(local) : null;
            String remoteMd5 = remote != null ? remote.getMd5Checksum() : null;
            String syncedMd5 = state.getProperty(name + ".md5");
            long remoteMod = modifiedMillis(remote);

            if (remote == null) {
                // Only local has it → push up.
                if (localExists) {
                    long newRemoteMod = upsertFileBlocking(drive, folderId, local);
                    writeMarker(name, localMd5, newRemoteMod);
                }
            } else if (!localExists) {
                // Only remote has it (fresh device) → pull down.
                pendings.add(prepareDownload(drive, remote, local));
            } else if (localMd5 != null && localMd5.equals(remoteMd5)) {
                // Content is byte-for-byte identical → nothing to do, just record the baseline.
                writeMarker(name, localMd5, remoteMod);
            } else if (syncedMd5 == null || syncedMd5.isBlank()) {
                // No agreed baseline yet (first sync on this device with both copies present and
                // different content). Resolve by size — the larger database wins — so a fresh/empty
                // device pulls the real data down and a device that still holds the real data pushes
                // it up over a wrongly-overwritten remote. The replaced side is backed up first.
                long remoteSize = remote.getSize() != null ? remote.getSize() : -1;
                if (remoteSize > local.length()) {
                    pendings.add(prepareDownload(drive, remote, local));
                } else {
                    long newRemoteMod = upsertFileBlocking(drive, folderId, local);
                    writeMarker(name, localMd5, newRemoteMod);
                }
            } else {
                boolean localChanged = !java.util.Objects.equals(localMd5, syncedMd5);
                boolean remoteChanged = !java.util.Objects.equals(remoteMd5, syncedMd5);

                if (remoteChanged) {
                    // Remote has new content (and may differ from local). Pull wins so other devices'
                    // changes are never lost; local is backed up before the swap. If local ALSO has
                    // genuine un-pushed changes this is a true conflict — remote wins, local in backup.
                    pendings.add(prepareDownload(drive, remote, local));
                } else if (localChanged) {
                    // Only local has new content → push it up.
                    long newRemoteMod = upsertFileBlocking(drive, folderId, local);
                    writeMarker(name, localMd5, newRemoteMod);
                } else {
                    // Neither side changed since the baseline — refresh the marker and move on.
                    writeMarker(name, syncedMd5, remoteMod);
                }
            }
        }

        recordSyncTimestamp();

        if (!pendings.isEmpty()) {
            Consumer<List<PendingDownload>> handler = reloadHandler;
            if (handler != null) {
                // Leave initialSyncComplete false until the UI thread has actually swapped the files
                // in and reloaded — markInitialSyncComplete() is called from there. This keeps
                // save-triggered pushes suppressed during the swap so the old/empty DB can't be
                // re-uploaded over the data we just pulled.
                handler.accept(pendings);
            } else {
                // No UI yet (shouldn't normally happen) — apply directly so data still lands.
                applyDownloadsDirect(pendings);
                initialSyncComplete = true;
            }
        } else {
            initialSyncComplete = true;
        }
    }

    /** Marks the initial bidirectional sync complete (called by the reload handler after it swaps). */
    public static void markInitialSyncComplete() {
        initialSyncComplete = true;
    }

    /** Backs up the current local file (if any) and downloads the remote into a temp file. */
    private static PendingDownload prepareDownload(Drive drive, File remote, java.io.File local) throws Exception {
        if (local.exists() && local.length() > 0) backupLocal(local);

        java.io.File tmp = new java.io.File(appDir(), local.getName() + ".incoming");
        try (OutputStream out = new java.io.FileOutputStream(tmp)) {
            drive.files().get(remote.getId()).executeMediaAndDownloadTo(out);
        }
        long remoteMod = (remote.getModifiedTime() != null) ? remote.getModifiedTime().getValue() : System.currentTimeMillis();
        return new PendingDownload(local, tmp, remoteMod);
    }

    /** Fallback swap when there is no UI reload handler available. */
    private static void applyDownloadsDirect(List<PendingDownload> pendings) {
        com.raeden.ors_to_do.dependencies.storage.StorageManager.close();
        for (PendingDownload p : pendings) {
            try {
                Files.move(p.tempFile.toPath(), p.localFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                recordSynced(p.localFile, p.remoteModifiedMillis);
            } catch (Exception e) {
                System.err.println("[GoogleDriveSync] could not apply downloaded file: " + e.getMessage());
            }
        }
    }

    /** Upserts a local file into the Drive folder by name; returns the resulting modifiedTime (millis). */
    private static long upsertFileBlocking(Drive drive, String folderId, java.io.File local) throws Exception {
        FileContent media = new FileContent("application/octet-stream", local);

        FileList existing = drive.files().list()
                .setQ("name='" + local.getName() + "' and '" + folderId + "' in parents and trashed=false")
                .setSpaces("drive")
                .setFields("files(id,name)")
                .execute();

        File result;
        if (existing.getFiles() != null && !existing.getFiles().isEmpty()) {
            String fileId = existing.getFiles().get(0).getId();
            result = drive.files().update(fileId, new File(), media).setFields("id,modifiedTime").execute();
        } else {
            File metadata = new File();
            metadata.setName(local.getName());
            metadata.setParents(Collections.singletonList(folderId));
            result = drive.files().create(metadata, media).setFields("id,modifiedTime").execute();
        }
        return (result.getModifiedTime() != null) ? result.getModifiedTime().getValue() : System.currentTimeMillis();
    }

    /** Copies the current local DB into the timestamped backup folder, pruning old backups. */
    private static void backupLocal(java.io.File local) {
        try {
            java.io.File dir = new java.io.File(appDir(), BACKUP_DIR);
            if (!dir.exists() && !dir.mkdirs()) return;
            String stamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            java.io.File backup = new java.io.File(dir, local.getName() + "." + stamp + ".bak");
            Files.copy(local.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            pruneBackups(dir, local.getName());
        } catch (Exception e) {
            System.err.println("[GoogleDriveSync] backup failed for " + local.getName() + ": " + e.getMessage());
        }
    }

    private static void pruneBackups(java.io.File dir, String dbName) {
        java.io.File[] backups = dir.listFiles((d, n) -> n.startsWith(dbName + ".") && n.endsWith(".bak"));
        if (backups == null || backups.length <= MAX_BACKUPS_PER_FILE) return;
        java.util.Arrays.sort(backups, java.util.Comparator.comparingLong(java.io.File::lastModified));
        for (int i = 0; i < backups.length - MAX_BACKUPS_PER_FILE; i++) {
            //noinspection ResultOfMethodCallIgnored
            backups[i].delete();
        }
    }

    // ------------------------------------------------------------------
    // Sync-state persistence (per-file marker)
    // ------------------------------------------------------------------

    private static Properties loadState() {
        Properties p = new Properties();
        java.io.File f = new java.io.File(appDir(), SYNC_STATE_FILE);
        if (f.exists()) {
            try (java.io.InputStream in = new java.io.FileInputStream(f)) {
                p.load(in);
            } catch (Exception ignore) { /* start fresh */ }
        }
        return p;
    }

    /**
     * Records the content checksum agreed at the last sync for {@code name} (the baseline both sides
     * share) plus the remote modifiedTime. {@code md5} may be null (checksum unavailable) — then only
     * the timestamp is updated.
     */
    private static synchronized void writeMarker(String name, String md5, long remoteMod) {
        Properties p = loadState();
        if (md5 != null && !md5.isBlank()) p.setProperty(name + ".md5", md5);
        p.setProperty(name + ".remote", Long.toString(remoteMod));
        try (OutputStream out = new java.io.FileOutputStream(new java.io.File(appDir(), SYNC_STATE_FILE))) {
            p.store(out, "Statmaxxer Google Drive sync markers");
        } catch (Exception ignore) { /* non-fatal */ }
    }

    /** Lower-case hex MD5 of a file's bytes (matches Drive's md5Checksum). Null if unreadable. */
    private static String md5(java.io.File f) {
        if (f == null || !f.exists()) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            try (java.io.InputStream in = new java.io.FileInputStream(f)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Drive service / OAuth
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

    private static void deleteRecursively(java.io.File f) {
        if (f == null || !f.exists()) return;
        java.io.File[] children = f.listFiles();
        if (children != null) for (java.io.File c : children) deleteRecursively(c);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
