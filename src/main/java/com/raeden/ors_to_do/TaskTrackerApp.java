package com.raeden.ors_to_do;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.storage.ProfileManager;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.modules.*;
import com.raeden.ors_to_do.modules.dependencies.*;

import com.raeden.ors_to_do.modules.dependencies.services.*;
import com.raeden.ors_to_do.modules.dependencies.ui.layout.GlobalSearchBar;
import com.raeden.ors_to_do.modules.dependencies.ui.layout.SidebarManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class TaskTrackerApp extends Application {
    public static final String APP_VERSION = "v1.485";

    private List<TaskItem> taskDatabase;
    private AppStats appStats;
    private BorderPane rootLayout;

    private FocusHubModule focusHubPanel;
    private AnalyticsModule analyticsPanel;
    private ArchivedModule archivedPanel;
    private SettingsModule settingsPanel;
    private DynamicModule currentDynamicPanel;

    private boolean isFirstMinimize = true;
    public static Stage MAIN_STAGE; // --- FIXED: Expose stage for dialogs ---

    private GlobalSearchBar globalSearchBar;
    private SidebarManager sidebarManager;
    private QuickCaptureManager quickCaptureManager;

    /** Shared UI-refresh callback; an instance field so detached pop-out windows can reuse it. */
    private Runnable syncUI;

    /** True on a genuinely fresh install (no data yet) so the setup wizard runs once. */
    private boolean firstLaunch = false;

    @Override
    public void init() throws Exception {
        // Point storage at the previously-active profile before any data is loaded.
        ProfileManager.init();

        taskDatabase = StorageManager.loadTasks();
        appStats = StorageManager.loadStats();

        // Detect a brand-new install BEFORE migration creates the default sections.
        firstLaunch = !appStats.isSetupCompleted() && appStats.getSections().isEmpty() && taskDatabase.isEmpty();

        runSilentDataMigration();
        DailyRolloverManager.processDailyRollover(appStats, taskDatabase);
    }

    private void runSilentDataMigration() {
        boolean needsSave = false;

        if (appStats.getSections().isEmpty()) {
            SectionConfig quick = new SectionConfig("QUICK", appStats.getNavQuickText());
            quick.setShowPriority(true); quick.setEnableSubTasks(true); quick.setAutoArchive(true); quick.setShowDate(true);

            SectionConfig daily = new SectionConfig("DAILY", appStats.getNavDailyText());
            daily.setResetIntervalHours(24);
            daily.setHasStreak(true); daily.setShowPrefix(true); daily.setAutoArchive(true);

            SectionConfig work = new SectionConfig("WORK", appStats.getNavWorkText());
            work.setShowAnalytics(true); work.setEnableSubTasks(true); work.setShowPriority(true); work.setShowTaskType(true);
            work.setTrackTime(true); work.setAutoArchive(true); work.setShowTags(true); work.setShowDate(true);

            appStats.getSections().addAll(List.of(quick, daily, work));
            needsSave = true;
        }

        if (!appStats.getBaseDailies().isEmpty()) {
            Optional<SectionConfig> dailyConfig = appStats.getSections().stream().filter(s -> "DAILY".equals(s.getId())).findFirst();
            if (dailyConfig.isPresent() && dailyConfig.get().getAutoAddTemplates().isEmpty()) {
                dailyConfig.get().getAutoAddTemplates().addAll(appStats.getBaseDailies());
                appStats.getBaseDailies().clear();
                needsSave = true;
            }
        }

        for (TaskItem task : taskDatabase) {
            if (task.getSectionId() == null && task.getLegacyOriginModule() != null) {
                task.setSectionId(task.getLegacyOriginModule().name());
                needsSave = true;
            }
        }

        // --- One-time migration: "Prevent Editing (Hours)" moved from global to per-section. ---
        // If the user had a global value set, copy it onto every section that still has 0 so
        // existing behaviour is preserved, then zero out the global value to retire it.
        int legacyGlobalLock = appStats.getPreventEditingHours();
        if (legacyGlobalLock > 0) {
            for (SectionConfig sc : appStats.getSections()) {
                if (sc.getPreventEditingHours() == 0) {
                    sc.setPreventEditingHours(legacyGlobalLock);
                }
            }
            appStats.setPreventEditingHours(0);
            needsSave = true;
        }

        if (needsSave) {
            StorageManager.saveStats(appStats);
            StorageManager.saveTasks(taskDatabase);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        MAIN_STAGE = primaryStage;

        if (!SingleInstanceManager.registerInstance(APP_VERSION, primaryStage)) {
            System.out.println("An instance of " + APP_VERSION + " is already running. Exiting.");
            Platform.exit();
            System.exit(0);
            return;
        }
        Platform.setImplicitExit(false);
        SystemTrayManager.setupSystemTray(primaryStage, this::shutdownApp);

        primaryStage.setOnCloseRequest(event -> {
            DailyRolloverManager.autoArchiveTasks(appStats, taskDatabase);
            StorageManager.saveTasks(taskDatabase);
            StorageManager.saveStats(appStats);

            if (appStats.isRunInBackground() && java.awt.SystemTray.isSupported()) {
                event.consume();
                primaryStage.hide();

                if (isFirstMinimize) {
                    SystemTrayManager.pushNotification("Running in Background", "Task Tracker is still running. Double-click the tray icon to restore.");
                    isFirstMinimize = false;
                }
            } else {
                shutdownApp();
            }
        });

        rootLayout = new BorderPane();

        // --- FIXED: Widened the base resolution of the app ---
        Scene scene = new Scene(rootLayout, 1005, 700);

        java.net.URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Register bundled fonts (Retro / Pixel Art / Matrix) and apply the user's chosen family.
        com.raeden.ors_to_do.modules.dependencies.ui.utils.FontManager.registerFonts();
        com.raeden.ors_to_do.modules.dependencies.ui.utils.FontManager.apply(scene, appStats.getTaskFontFamily());

        primaryStage.setTitle("Task-Tracker");
        primaryStage.setAlwaysOnTop(appStats.isAlwaysOnTop());

        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            System.out.println("Error loading window icon.");
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        // Show a brief loading screen, then build the UI on the next pulse so it actually renders
        // before the (potentially heavy) session build runs.
        showLoading(com.raeden.ors_to_do.i18n.Lang.LOADING_TASKS.get());
        Platform.runLater(() -> {
            buildSession(primaryStage);
            GlobalActivityTracker.init();
            // Mirror every local data write to Google Drive (no-op until an account is connected).
            StorageManager.setChangeListener(GoogleDriveSyncManager::onDataChanged);
            // When a sync pulls newer remote data down, swap it in and rebuild the UI (on the FX thread).
            GoogleDriveSyncManager.setReloadHandler(pendings -> Platform.runLater(() -> applyDownloadedData(pendings)));
            // Full bidirectional sync once on launch (async; no-op when not connected).
            GoogleDriveSyncManager.syncOnStartup();
            if (firstLaunch) {
                com.raeden.ors_to_do.modules.dependencies.settings.SetupWizard.show(appStats, () -> {
                    com.raeden.ors_to_do.modules.dependencies.ui.utils.FontManager.apply(scene, appStats.getTaskFontFamily());
                    buildSession(primaryStage);
                });
            }
        });
    }

    /** Replaces the main content with a simple branded loading view. */
    private void showLoading(String message) {
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(18);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: #1E1E1E;");

        Label title = new Label("Statmaxxer");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #569CD6;");
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(48, 48);
        spinner.setStyle("-fx-progress-color: #569CD6;");
        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 13px;");

        box.getChildren().addAll(title, spinner, msg);
        rootLayout.setLeft(null);
        rootLayout.setCenter(box);
    }

    /**
     * (Re)builds everything that is bound to the current {@code taskDatabase}/{@code appStats}: the
     * sidebar, all modules, the global search, quick-capture hook, and notifications. Called once at
     * startup and again whenever the active profile changes (so the new profile's data is shown).
     */
    private void buildSession(Stage stage) {
        // Close any pop-out windows from a previous session before rebuilding (e.g. on profile switch),
        // since they are bound to the now-stale data references.
        if (sidebarManager != null) sidebarManager.closeAllPopOuts();

        syncUI = () -> {
            if (currentDynamicPanel != null) currentDynamicPanel.refreshList();
            if (focusHubPanel != null) focusHubPanel.refreshTasks();
            if (analyticsPanel != null) analyticsPanel.refreshData();
            if (sidebarManager != null) sidebarManager.refreshSidebar();
        };

        // Register the global hotkey hook once for the app's lifetime. On profile switch we only
        // swap its data references — re-registering JNativeHook crashes its (terminated) dispatch
        // pool with a RejectedExecutionException.
        if (quickCaptureManager == null) {
            quickCaptureManager = new QuickCaptureManager(appStats, taskDatabase, syncUI);
            quickCaptureManager.register();
        } else {
            quickCaptureManager.updateContext(appStats, taskDatabase, syncUI);
        }

        globalSearchBar = new GlobalSearchBar((query) -> {
            if (query == null || query.trim().isEmpty()) {
                switchModule(sidebarManager.getActiveModule());
            } else {
                SearchModule searchModule = new SearchModule(query.trim(), taskDatabase, appStats, syncUI, this::navigateToModule);
                rootLayout.setCenter(searchModule);
            }
        });

        sidebarManager = new SidebarManager(appStats, taskDatabase, globalSearchBar, this::navigateToModule, this::createPopOutView);

        focusHubPanel = new FocusHubModule(appStats, taskDatabase, syncUI);
        analyticsPanel = new AnalyticsModule(appStats, taskDatabase);
        archivedPanel = new ArchivedModule(taskDatabase, appStats, syncUI);
        settingsPanel = new SettingsModule(appStats, taskDatabase, syncUI, this::switchProfile);

        rootLayout.setLeft(sidebarManager);

        if (!appStats.getSections().isEmpty()) navigateToModule(appStats.getSections().get(0).getId());
        else navigateToModule("SETTINGS");

        NotificationManager.start(appStats, taskDatabase, stage);
    }

    /**
     * Switches to a different data profile: persists the current profile, repoints storage at the
     * target profile's database, reloads its data, and rebuilds the whole UI session.
     */
    private void switchProfile(String newProfileId) {
        if (newProfileId == null || newProfileId.equals(ProfileManager.getActiveId())) return;

        // Persist the current profile before swapping databases.
        DailyRolloverManager.autoArchiveTasks(appStats, taskDatabase);
        StorageManager.saveTasks(taskDatabase);
        StorageManager.saveStats(appStats);

        // Show the loading screen, then do the (possibly heavy) reload + rebuild on the next pulse.
        showLoading(com.raeden.ors_to_do.i18n.Lang.LOADING_SWITCHING_PROFILE.get());
        Platform.runLater(() -> {
            ProfileManager.setActive(newProfileId);
            taskDatabase = StorageManager.loadTasks();
            appStats = StorageManager.loadStats();
            runSilentDataMigration();
            DailyRolloverManager.processDailyRollover(appStats, taskDatabase);

            buildSession(MAIN_STAGE);

            if (MAIN_STAGE.getScene() != null) {
                com.raeden.ors_to_do.modules.dependencies.ui.utils.FontManager.apply(MAIN_STAGE.getScene(), appStats.getTaskFontFamily());
            }
            MAIN_STAGE.setAlwaysOnTop(appStats.isAlwaysOnTop());
        });
    }

    /**
     * Applies data pulled from Google Drive: closes the live DB, swaps each downloaded file in over
     * the local copy (the manager has already backed up what's being replaced), then reloads and
     * rebuilds the UI from the new data. Runs on the FX thread so no save can interleave with the
     * file swap.
     */
    private void applyDownloadedData(java.util.List<GoogleDriveSyncManager.PendingDownload> pendings) {
        if (pendings == null || pendings.isEmpty()) return;

        // Suppress save-triggered pushes while we swap files out from under the storage layer.
        StorageManager.setChangeListener(null);
        showLoading("Loading synced data from Google Drive…");

        StorageManager.close();
        for (GoogleDriveSyncManager.PendingDownload p : pendings) {
            try {
                // Drop any stale SQLite sidecar journals so they can't corrupt the swapped-in DB.
                for (String suffix : new String[]{"-wal", "-shm", "-journal"}) {
                    java.io.File sidecar = new java.io.File(p.localFile.getParentFile(), p.localFile.getName() + suffix);
                    if (sidecar.exists()) //noinspection ResultOfMethodCallIgnored
                        sidecar.delete();
                }
                java.nio.file.Files.move(p.tempFile.toPath(), p.localFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                GoogleDriveSyncManager.recordSynced(p.localFile, p.remoteModifiedMillis);
            } catch (Exception ex) {
                System.err.println("[Sync] failed to apply downloaded file " + p.localFile.getName() + ": " + ex.getMessage());
            }
        }

        // Reload the active profile's data from the freshly-pulled DB and rebuild everything.
        taskDatabase = StorageManager.loadTasks();
        appStats = StorageManager.loadStats();
        runSilentDataMigration();
        DailyRolloverManager.processDailyRollover(appStats, taskDatabase);

        buildSession(MAIN_STAGE);
        if (MAIN_STAGE.getScene() != null) {
            com.raeden.ors_to_do.modules.dependencies.ui.utils.FontManager.apply(MAIN_STAGE.getScene(), appStats.getTaskFontFamily());
        }

        // Re-arm the change listener (buildSession does not set it) and only now allow push-on-save,
        // since the freshly-pulled data is in place.
        StorageManager.setChangeListener(GoogleDriveSyncManager::onDataChanged);
        GoogleDriveSyncManager.markInitialSyncComplete();
    }

    private void shutdownApp() {
        try {
            stop();
            Platform.exit();
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void navigateToModule(String internalId) {
        sidebarManager.setActiveModule(internalId);

        if (!globalSearchBar.isEmpty()) {
            globalSearchBar.clear();
        } else {
            switchModule(internalId);
        }

        if (internalId.equals("ARCHIVE")) archivedPanel.refreshList();
    }

    private void switchModule(String internalId) {
        Node activePane = null;

        if (internalId.equals("FOCUS")) {
            activePane = focusHubPanel;
            focusHubPanel.refreshTasks();
            currentDynamicPanel = null;
        } else if (internalId.equals("ANALYTICS")) {
            activePane = analyticsPanel;
            analyticsPanel.refreshData();
            currentDynamicPanel = null;
        } else if (internalId.equals("ARCHIVE")) {
            activePane = archivedPanel;
            currentDynamicPanel = null;
        } else if (internalId.equals("SETTINGS")) {
            activePane = settingsPanel;
            currentDynamicPanel = null;
        } else {
            Optional<SectionConfig> matchedConfig = appStats.getSections().stream()
                    .filter(c -> c.getId().equals(internalId))
                    .findFirst();

            if (matchedConfig.isPresent()) {
                Runnable syncUI = () -> {
                    if (currentDynamicPanel != null) currentDynamicPanel.refreshList();
                    if (focusHubPanel != null) focusHubPanel.refreshTasks();
                    sidebarManager.refreshSidebar();
                };

                currentDynamicPanel = new DynamicModule(matchedConfig.get(), taskDatabase, appStats, syncUI);
                activePane = currentDynamicPanel;
            } else {
                activePane = new VBox(new Label("Error: Section Configuration Not Found for " + internalId));
                currentDynamicPanel = null;
            }
        }

        if (activePane != null) rootLayout.setCenter(activePane);
    }

    /**
     * Builds a fresh, standalone view for the given module id, used by the sidebar's pop-out windows.
     * Returns an independent instance (not the in-place {@code currentDynamicPanel}) bound to the
     * same {@code appStats}/{@code taskDatabase}, so edits in a pop-out stay in sync via {@code syncUI}.
     */
    private Node createPopOutView(String internalId) {
        if (internalId.equals("FOCUS")) {
            FocusHubModule m = new FocusHubModule(appStats, taskDatabase, syncUI);
            m.refreshTasks();
            return m;
        } else if (internalId.equals("ANALYTICS")) {
            AnalyticsModule m = new AnalyticsModule(appStats, taskDatabase);
            m.refreshData();
            return m;
        } else if (internalId.equals("ARCHIVE")) {
            ArchivedModule m = new ArchivedModule(taskDatabase, appStats, syncUI);
            m.refreshList();
            return m;
        } else if (internalId.equals("SETTINGS")) {
            return new SettingsModule(appStats, taskDatabase, syncUI, this::switchProfile);
        }

        Optional<SectionConfig> matchedConfig = appStats.getSections().stream()
                .filter(c -> c.getId().equals(internalId))
                .findFirst();
        if (matchedConfig.isPresent()) {
            return new DynamicModule(matchedConfig.get(), taskDatabase, appStats, syncUI);
        }
        return null;
    }

    @Override
    public void stop() throws Exception {
        DailyRolloverManager.autoArchiveTasks(appStats, taskDatabase);
        StorageManager.saveTasks(taskDatabase);
        StorageManager.saveStats(appStats);

        if (quickCaptureManager != null) quickCaptureManager.unregister();
    }

    public static void main(String[] args) {
        launch(args);
    }
}