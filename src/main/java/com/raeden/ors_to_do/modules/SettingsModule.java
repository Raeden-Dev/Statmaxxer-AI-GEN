package com.raeden.ors_to_do.modules;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.settings.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class SettingsModule extends StackPane {

    public SettingsModule(AppStats appStats, List<TaskItem> globalDatabase, Runnable refreshCallback, java.util.function.Consumer<String> onSwitchProfile) {
        setStyle("-fx-background-color: #1E1E1E;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        scrollPane.setBorder(Border.EMPTY);

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
        CloudSyncPanel cloudSyncPanel = new CloudSyncPanel(appStats);

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
                cloudSyncPanel,
                dangerPanel
        );

        scrollPane.setContent(contentBox);

        // --- Floating, icon-based quick-jump nav (overlaid on the right edge) ---
        List<NavTarget> targets = new ArrayList<>();
        targets.add(new NavTarget("❓", "Help & About", helpPanel));
        targets.add(new NavTarget("👤", "Profiles", profilePanel));
        targets.add(new NavTarget("⚙", "General", generalPanel));
        targets.add(new NavTarget("🗂", "Dynamic Sections", sectionWrap));
        targets.add(new NavTarget("📋", "Templates", templatePanel));
        targets.add(new NavTarget("📊", "Stat Configuration", statsWrap));
        targets.add(new NavTarget("🚩", "Priorities", priorityWrap));
        targets.add(new NavTarget("💾", "Data Management", dataPanel));
        targets.add(new NavTarget("☁", "Cloud Sync", cloudSyncPanel));
        targets.add(new NavTarget("⚠", "Danger Zone", dangerPanel));

        Region floatingNav = buildFloatingNav(scrollPane, contentBox, targets);
        StackPane.setAlignment(floatingNav, Pos.BOTTOM_CENTER);
        StackPane.setMargin(floatingNav, new Insets(0, 0, 11, 0));

        getChildren().addAll(scrollPane, floatingNav);
    }

    private Region buildFloatingNav(ScrollPane scrollPane, VBox contentBox, List<NavTarget> targets) {
        // Horizontal pill anchored at the bottom-centre of the page (25% smaller than the original
        // right-edge column).
        HBox nav = new HBox(5);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(6, 5, 6, 5));
        nav.setMaxHeight(Region.USE_PREF_SIZE);
        nav.setMaxWidth(Region.USE_PREF_SIZE);
        nav.setStyle("-fx-background-color: rgba(37,37,38,0.92); -fx-background-radius: 16; "
                + "-fx-border-color: #3E3E42; -fx-border-radius: 16; -fx-border-width: 1;");
        // Let clicks on the page pass through the gaps around the pill.
        nav.setPickOnBounds(false);

        String idle = "-fx-background-color: transparent; -fx-text-fill: #DCDCDC; -fx-font-size: 12px; "
                + "-fx-cursor: hand; -fx-min-width: 26; -fx-min-height: 26; -fx-background-radius: 13;";
        String hover = "-fx-background-color: #0E639C; -fx-text-fill: white; -fx-font-size: 12px; "
                + "-fx-cursor: hand; -fx-min-width: 26; -fx-min-height: 26; -fx-background-radius: 13;";

        for (NavTarget t : targets) {
            Button btn = new Button(t.icon);
            btn.setStyle(idle);
            btn.setTooltip(new Tooltip(t.tooltip));
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e -> btn.setStyle(idle));
            btn.setOnAction(e -> scrollToNode(scrollPane, contentBox, t.node));
            nav.getChildren().add(btn);
        }
        return nav;
    }

    /** Scrolls the settings ScrollPane so that {@code target} sits near the top of the viewport. */
    private void scrollToNode(ScrollPane scrollPane, VBox contentBox, Node target) {
        // Ensure layout is current so bounds are accurate before we read them.
        scrollPane.applyCss();
        scrollPane.layout();

        double targetY = target.getBoundsInParent().getMinY();
        double contentHeight = contentBox.getBoundsInLocal().getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double scrollable = contentHeight - viewportHeight;

        double v = scrollable <= 0 ? 0 : targetY / scrollable;
        scrollPane.setVvalue(Math.max(0, Math.min(1, v)));
    }

    private static final class NavTarget {
        final String icon;
        final String tooltip;
        final Node node;
        NavTarget(String icon, String tooltip, Node node) {
            this.icon = icon;
            this.tooltip = tooltip;
            this.node = node;
        }
    }
}
