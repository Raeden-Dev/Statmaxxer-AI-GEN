package com.raeden.ors_to_do;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
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
    public static final String APP_VERSION = "v1.45";

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

    @Override
    public void init() throws Exception {
        taskDatabase = StorageManager.loadTasks();
        appStats = StorageManager.loadStats();

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

        Runnable syncUI = () -> {
            if (currentDynamicPanel != null) currentDynamicPanel.refreshList();
            if (focusHubPanel != null) focusHubPanel.refreshTasks();
            if (analyticsPanel != null) analyticsPanel.refreshData();
            if (sidebarManager != null) sidebarManager.refreshSidebar();
        };

        quickCaptureManager = new QuickCaptureManager(appStats, taskDatabase, syncUI);
        quickCaptureManager.register();

        globalSearchBar = new GlobalSearchBar((query) -> {
            if (query == null || query.trim().isEmpty()) {
                switchModule(sidebarManager.getActiveModule());
            } else {
                SearchModule searchModule = new SearchModule(query.trim(), taskDatabase, appStats, syncUI, this::navigateToModule);
                rootLayout.setCenter(searchModule);
            }
        });

        sidebarManager = new SidebarManager(appStats, taskDatabase, globalSearchBar, this::navigateToModule);

        focusHubPanel = new FocusHubModule(appStats, taskDatabase, syncUI);
        analyticsPanel = new AnalyticsModule(appStats, taskDatabase);
        archivedPanel = new ArchivedModule(taskDatabase, appStats, syncUI);
        settingsPanel = new SettingsModule(appStats, taskDatabase, syncUI);

        rootLayout.setLeft(sidebarManager);

        // --- FIXED: Widened the base resolution of the app ---
        Scene scene = new Scene(rootLayout, 1005, 700);

        java.net.URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

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

        if (!appStats.getSections().isEmpty()) navigateToModule(appStats.getSections().get(0).getId());
        else navigateToModule("SETTINGS");

        NotificationManager.start(appStats, taskDatabase, primaryStage);
        GlobalActivityTracker.init();
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