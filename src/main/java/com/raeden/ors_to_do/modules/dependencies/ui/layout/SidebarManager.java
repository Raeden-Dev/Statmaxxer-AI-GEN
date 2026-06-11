package com.raeden.ors_to_do.modules.dependencies.ui.layout;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class SidebarManager extends BorderPane {
    private AppStats appStats;
    private GlobalSearchBar searchBar;
    private Consumer<String> onNavigate;
    private String currentActiveModule = "QUICK";
    private List<TaskItem> globalDatabase;

    private boolean isStaticExpanded = true;

    // --- FIXED: Make ScrollPane a permanent class variable ---
    private ScrollPane dynamicScrollPane;

    public SidebarManager(AppStats appStats, List<TaskItem> globalDatabase, GlobalSearchBar searchBar, Consumer<String> onNavigate) {
        this.appStats = appStats;
        this.searchBar = searchBar;
        this.onNavigate = onNavigate;
        this.globalDatabase = globalDatabase;

        getStyleClass().add("sidebar");
        setPrefWidth(220);

        // Initialize the ScrollPane once
        dynamicScrollPane = new ScrollPane();
        dynamicScrollPane.setFitToWidth(true);
        dynamicScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dynamicScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        dynamicScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        String scrollCss = ".scroll-pane { -fx-background-color: transparent; -fx-padding: 0; } " +
                ".scroll-pane > .viewport { -fx-background-color: transparent; } " +
                ".scroll-bar:vertical { -fx-background-color: transparent; -fx-pref-width: 5; } " +
                ".scroll-bar:vertical .thumb { -fx-background-color: #3E3E42; -fx-background-radius: 5; } " +
                ".scroll-bar:vertical .thumb:hover { -fx-background-color: #555555; }";
        dynamicScrollPane.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(scrollCss.getBytes()));

        refreshSidebar();
    }

    public void refreshSidebar() {
        // --- FIXED: Capture current scroll position before rebuilding ---
        double currentScrollPos = dynamicScrollPane.getVvalue();

        setTop(null);
        setBottom(null);

        VBox topBox = new VBox();
        topBox.setPadding(new Insets(0, 0, 10, 0));
        topBox.getChildren().add(searchBar);
        setTop(topBox);

        VBox dynamicSectionsBox = new VBox();
        dynamicSectionsBox.setStyle("-fx-background-color: transparent;");

        // Tracks the most-recent separator's collapse state so buttons under it can be skipped
        // until the next separator (or the end of the list).
        boolean retractableEnabled = appStats.isSidebarSeparatorsCollapsible();
        boolean currentSeparatorCollapsed = false;

        for (SectionConfig config : appStats.getSections()) {

            if (config.isSeparator()) {
                // When retractability is off, separators become plain static dividers and never hide
                // their sections — every section button stays visible. Only when the feature is on
                // do we honour each separator's persisted collapse state.
                currentSeparatorCollapsed = retractableEnabled && appStats.isSeparatorCollapsed(config.getId());
                dynamicSectionsBox.getChildren().add(buildSeparatorRow(config, currentSeparatorCollapsed, retractableEnabled));
                continue;
            }

            // Skip rendering this button: it lives under a collapsed separator.
            if (currentSeparatorCollapsed) continue;

            int activeTaskCount = 0;

            if (appStats.isShowSidebarTaskCount() && globalDatabase != null) {
                for (TaskItem task : globalDatabase) {
                    if (config.getId().equals(task.getSectionId()) && !task.isFinished() && !task.isArchived()) {
                        activeTaskCount++;
                    }
                }
            }

            dynamicSectionsBox.getChildren().add(createSidebarButton(config.getName(), config.getId(), config.getSidebarColor(), activeTaskCount));
        }

        // Apply new content to existing ScrollPane
        dynamicScrollPane.setContent(dynamicSectionsBox);
        setCenter(dynamicScrollPane);

        // Restore scroll position after the JavaFX UI thread updates the content height
        javafx.application.Platform.runLater(() -> dynamicScrollPane.setVvalue(currentScrollPos));

        VBox bottomBox = new VBox();
        bottomBox.setStyle("-fx-background-color: transparent;");

        VBox separatorArea = new VBox(2);
        separatorArea.setAlignment(Pos.CENTER);
        separatorArea.setCursor(Cursor.HAND);
        separatorArea.setStyle("-fx-background-color: transparent;");

        Label arrowLabel = new Label(isStaticExpanded ? "▼" : "▲");
        arrowLabel.setStyle("-fx-text-fill: #858585; -fx-font-size: 10px;");
        arrowLabel.setOpacity(0.0);

        Separator sep = new Separator();
        sep.setPadding(new Insets(2, 0, 8, 0));

        separatorArea.getChildren().addAll(arrowLabel, sep);
        separatorArea.setOnMouseEntered(e -> arrowLabel.setOpacity(1.0));
        separatorArea.setOnMouseExited(e -> arrowLabel.setOpacity(0.0));

        VBox staticModulesBox = new VBox();
        staticModulesBox.setMinHeight(0);

        staticModulesBox.getChildren().addAll(
                createSidebarButton(appStats.getNavFocusText(), "FOCUS", appStats.getNavFocusColor(), -1),
                createSidebarButton(appStats.getNavAnalyticsText(), "ANALYTICS", appStats.getNavAnalyticsColor(), -1),
                createSidebarButton(appStats.getNavArchiveText(), "ARCHIVE", appStats.getNavArchiveColor(), -1),
                createSidebarButton(appStats.getNavSettingsText(), "SETTINGS", appStats.getNavSettingsColor(), -1)
        );

        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(staticModulesBox.widthProperty());
        clipRect.heightProperty().bind(staticModulesBox.maxHeightProperty());
        staticModulesBox.setClip(clipRect);

        if (!isStaticExpanded) {
            staticModulesBox.setMaxHeight(0);
            staticModulesBox.setOpacity(0);
            staticModulesBox.setManaged(false);
            staticModulesBox.setVisible(false);
        } else {
            staticModulesBox.setMaxHeight(200);
            staticModulesBox.setOpacity(1);
        }

        separatorArea.setOnMouseClicked(e -> {
            isStaticExpanded = !isStaticExpanded;
            arrowLabel.setText(isStaticExpanded ? "▼" : "▲");

            if (isStaticExpanded) {
                staticModulesBox.setManaged(true);
                staticModulesBox.setVisible(true);

                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(staticModulesBox.maxHeightProperty(), 0),
                                new KeyValue(staticModulesBox.opacityProperty(), 0)
                        ),
                        new KeyFrame(Duration.millis(250),
                                new KeyValue(staticModulesBox.maxHeightProperty(), 200),
                                new KeyValue(staticModulesBox.opacityProperty(), 1)
                        )
                );
                tl.play();
            } else {
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(staticModulesBox.maxHeightProperty(), staticModulesBox.getHeight()),
                                new KeyValue(staticModulesBox.opacityProperty(), 1)
                        ),
                        new KeyFrame(Duration.millis(250),
                                new KeyValue(staticModulesBox.maxHeightProperty(), 0),
                                new KeyValue(staticModulesBox.opacityProperty(), 0)
                        )
                );
                tl.setOnFinished(evt -> {
                    staticModulesBox.setManaged(false);
                    staticModulesBox.setVisible(false);
                });
                tl.play();
            }
        });

        bottomBox.getChildren().addAll(separatorArea, staticModulesBox);
        setBottom(bottomBox);
    }

    private Button createSidebarButton(String displayText, String internalId, String hexColor, int taskCount) {
        Button btn = new Button(displayText);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        Rectangle rect = new Rectangle(5, 20);
        rect.setArcWidth(3); rect.setArcHeight(3);
        rect.setFill(Color.web(hexColor != null ? hexColor : "#FFFFFF"));

        HBox graphicContainer = new HBox(8);
        graphicContainer.setAlignment(Pos.CENTER_LEFT);

        if (taskCount > 0 && appStats.isShowSidebarTaskCount()) {
            String displayCount = String.format("%02d", taskCount);
            Label countLabel = new Label(displayCount);
            countLabel.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px; -fx-font-weight: bold;");
            graphicContainer.getChildren().add(countLabel);
        }

        graphicContainer.getChildren().add(rect);

        btn.setGraphic(graphicContainer);
        btn.setGraphicTextGap(10);

        if (currentActiveModule.equals(internalId)) {
            btn.getStyleClass().add("active");
        }

        btn.setOnAction(e -> onNavigate.accept(internalId));

        return btn;
    }

    public void setActiveModule(String internalId) {
        this.currentActiveModule = internalId;
        refreshSidebar();
    }

    public String getActiveModule() {
        return currentActiveModule;
    }

    /**
     * Builds a separator row. When {@code retractable} is true the row is clickable, the chevron
     * fades in on hover, and a collapsed separator gets a distinct visual treatment so the user
     * can see at a glance which sections are hidden. When {@code retractable} is false the row is
     * static (no hover/click) and just shows the dim divider — the user disabled the feature.
     */
    private VBox buildSeparatorRow(SectionConfig config, boolean collapsed, boolean retractable) {
        // Tightened padding: was Insets(15, 10, 5, 10) — section buttons now sit closer to the
        // separator above them.
        VBox sepBox = new VBox(1);
        sepBox.setAlignment(Pos.CENTER);
        sepBox.setPadding(new Insets(6, 10, 1, 10));

        if (retractable) sepBox.setCursor(Cursor.HAND);

        // Header row: name + chevron. Chevron always visible when collapsed (so the user
        // recognises the retracted state from any angle); hover-only when expanded.
        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER);

        // Distinct treatment for collapsed separators: brighter title text + accent colour so
        // they're visually different from expanded separators of the same name.
        String titleColor = collapsed ? "#DCDCAA" : "#858585";

        Label sepName = null;
        if (config.getName() != null && !config.getName().isEmpty()) {
            sepName = new Label(config.getName().toUpperCase());
            sepName.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1.5px;");
            sepName.setAlignment(Pos.CENTER);
            titleRow.getChildren().add(sepName);
        }

        Label chevron = new Label(collapsed ? "▲" : "▼");
        chevron.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-size: 9px;");
        // Collapsed → always visible (no hover required). Expanded + retractable → hover only.
        // Disabled → never visible (the row is static).
        chevron.setOpacity(collapsed && retractable ? 1.0 : 0.0);
        titleRow.getChildren().add(chevron);

        sepBox.getChildren().add(titleRow);

        Separator line = new Separator();
        // Subtle highlight on the divider when collapsed so the whole row reads as "retracted".
        line.setOpacity(collapsed ? 0.45 : 0.15);
        sepBox.getChildren().add(line);

        if (retractable) {
            // Hover-only chevron when expanded; collapsed already shows it. The mouseExited rule
            // also has to honour the "always-on while collapsed" state, otherwise the chevron
            // would fade out on every mouseExit even from a collapsed separator.
            sepBox.setOnMouseEntered(e -> chevron.setOpacity(1.0));
            sepBox.setOnMouseExited(e -> chevron.setOpacity(collapsed ? 1.0 : 0.0));

            sepBox.setOnMouseClicked(e -> {
                boolean newlyCollapsed = !appStats.isSeparatorCollapsed(config.getId());
                appStats.setSeparatorCollapsed(config.getId(), newlyCollapsed);
                StorageManager.saveStats(appStats);
                refreshSidebar();
            });
        }

        return sepBox;
    }
}