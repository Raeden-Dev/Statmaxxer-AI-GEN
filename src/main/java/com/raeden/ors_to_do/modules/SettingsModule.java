package com.raeden.ors_to_do.modules;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.settings.*;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.VBox;
import java.util.List;

public class SettingsModule extends ScrollPane {

    public SettingsModule(AppStats appStats, List<TaskItem> globalDatabase, Runnable refreshCallback, java.util.function.Consumer<String> onSwitchProfile) {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        setBorder(Border.EMPTY);

        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(20));

        HelpAboutPanel helpPanel = new HelpAboutPanel(appStats);

        Label header = new Label("Control Center");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        StatsManagerPanel statsManagerPanel = new StatsManagerPanel(appStats, refreshCallback);

        Runnable wrappedRefreshCallback = () -> {
            // --- FIXED: Call the internal refresh method instead of overriding ---
            statsManagerPanel.refreshState();
            refreshCallback.run();
        };

        GeneralSettingsPanel generalPanel = new GeneralSettingsPanel(appStats, wrappedRefreshCallback);
        TemplateManagerPanel templatePanel = new TemplateManagerPanel(appStats, globalDatabase, wrappedRefreshCallback);
        PriorityManagerPanel priorityPanel = new PriorityManagerPanel(appStats, wrappedRefreshCallback);
        DataManagementPanel dataPanel = new DataManagementPanel(appStats, globalDatabase, wrappedRefreshCallback);
        DangerZonePanel dangerPanel = new DangerZonePanel(appStats, globalDatabase, wrappedRefreshCallback);

        Runnable onSectionChanged = () -> {
            templatePanel.refreshSectionSelector();
            dangerPanel.refreshDangerZone();
            wrappedRefreshCallback.run();
        };
        SectionManagerPanel sectionPanel = new SectionManagerPanel(appStats, globalDatabase, wrappedRefreshCallback, onSectionChanged);

        // --- Collapsible (eye-toggle) wrappers for the three big sections so the settings page can
        // be kept compact. Collapse state persists in AppStats. ---
        CollapsibleSettingsSection sectionWrap = new CollapsibleSettingsSection(
                "Dynamic Sections", sectionPanel, appStats.isSettingsPanelCollapsed("sections"),
                collapsed -> { appStats.setSettingsPanelCollapsed("sections", collapsed); StorageManager.saveStats(appStats); });
        CollapsibleSettingsSection statsWrap = new CollapsibleSettingsSection(
                "Stat Configuration", statsManagerPanel, appStats.isSettingsPanelCollapsed("stats"),
                collapsed -> { appStats.setSettingsPanelCollapsed("stats", collapsed); StorageManager.saveStats(appStats); });
        CollapsibleSettingsSection priorityWrap = new CollapsibleSettingsSection(
                "Priorities", priorityPanel, appStats.isSettingsPanelCollapsed("priorities"),
                collapsed -> { appStats.setSettingsPanelCollapsed("priorities", collapsed); StorageManager.saveStats(appStats); });

        ProfileManagerPanel profilePanel = new ProfileManagerPanel(onSwitchProfile);

        contentBox.getChildren().addAll(
                helpPanel,
                header,
                profilePanel,
                generalPanel,
                sectionWrap,
                templatePanel,
                statsWrap,
                priorityWrap,
                dataPanel,
                dangerPanel
        );

        setContent(contentBox);
    }
}