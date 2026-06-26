package com.raeden.ors_to_do.modules.dependencies.ui.components;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CategoryStyle;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.CategoryStyleDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Renders a list of tasks as collapsible category groups, similar to how sub-tasks expand under
 * a parent card. Groups are determined by {@link TaskItem#getCategoryName()}; null/blank values
 * are bucketed under {@link Lang#CATEGORY_UNCATEGORIZED}.
 *
 * <p>Per-section collapse state is persisted via {@link AppStats#getCollapsedCategories()} so
 * returning to a section restores each category's expand/collapse state. Per-category visual
 * customization (background / border / text / icon) lives on
 * {@link SectionConfig#getCategoryStyles()} and is applied here.</p>
 */
public final class CategoryGroupRenderer {

    private static final String DEFAULT_BG = "#252526";
    private static final String DEFAULT_BORDER = "#3E3E42";
    private static final String DEFAULT_TEXT = "#DCDCAA";

    private CategoryGroupRenderer() { }

    /**
     * Drops {@code tasks} into the {@code parent} VBox grouped by category, with collapse headers.
     *
     * @param parent          target container that gets the headers + bodies appended
     * @param tasks           tasks to bucket (already filtered to the section)
     * @param globalDatabase  full task list — needed by the customize dialog so a category rename
     *                        can rewrite every matching task in the section
     * @param config          owning section (for category styles + id)
     * @param appStats        global state (for collapse persistence)
     * @param cardFactory     turns a TaskItem into the section-appropriate card node
     * @param onCollapseChange optional callback fired after each collapse/expand
     */
    public static void render(VBox parent,
                              List<TaskItem> tasks,
                              List<TaskItem> globalDatabase,
                              SectionConfig config,
                              AppStats appStats,
                              Function<TaskItem, Node> cardFactory,
                              Runnable onCollapseChange) {

        String sectionId = config.getId();
        Map<String, List<TaskItem>> grouped = groupByCategory(tasks);

        // Include categories that have been explicitly created (have a style) but hold no tasks yet,
        // so a freshly created empty category still renders and can receive cards.
        for (CategoryStyle cs : config.getCategoryStyles()) {
            String n = cs.getName();
            if (n != null && !n.trim().isEmpty()) grouped.putIfAbsent(n.trim(), new ArrayList<>());
        }

        for (Map.Entry<String, List<TaskItem>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<TaskItem> bucket = entry.getValue();
            boolean collapsedInitially = appStats.isCategoryCollapsed(sectionId, category);

            VBox bodyBox = new VBox(8);
            bodyBox.setPadding(new Insets(4, 0, 8, 12));
            bodyBox.setVisible(!collapsedInitially);
            bodyBox.setManaged(!collapsedInitially);

            HBox header = buildHeader(category, bucket.size(), config, globalDatabase, appStats, () -> {
                // Read the *current* visible state and flip it. The previous version inverted the
                // polarity and double-fired (button + row both ran the toggle), which made clicks
                // a no-op. This is the canonical "flip visibility" pattern.
                boolean newlyVisible = !bodyBox.isVisible();
                bodyBox.setVisible(newlyVisible);
                bodyBox.setManaged(newlyVisible);
                appStats.setCategoryCollapsed(sectionId, category, !newlyVisible);
                StorageManager.saveStats(appStats);
                if (onCollapseChange != null) onCollapseChange.run();
            }, () -> {
                // Style-change callback: persist the section and re-render the whole list so the
                // new colours / renamed buckets show immediately.
                StorageManager.saveStats(appStats);
                if (onCollapseChange != null) onCollapseChange.run();
            }, collapsedInitially);

            for (TaskItem t : bucket) bodyBox.getChildren().add(cardFactory.apply(t));

            parent.getChildren().addAll(header, bodyBox);
        }
    }

    /** Returns a LinkedHashMap (preserving insertion order) of category -> tasks. */
    static Map<String, List<TaskItem>> groupByCategory(List<TaskItem> tasks) {
        Map<String, List<TaskItem>> grouped = new LinkedHashMap<>();
        for (TaskItem t : tasks) {
            String cat = t.getCategoryName();
            if (cat == null || cat.trim().isEmpty()) cat = Lang.CATEGORY_UNCATEGORIZED.get();
            grouped.computeIfAbsent(cat, k -> new ArrayList<>()).add(t);
        }
        return grouped;
    }

    /** Returns the unique non-empty category names across {@code tasks}, in first-seen order. */
    public static List<String> uniqueCategories(List<TaskItem> tasks) {
        List<String> out = new ArrayList<>();
        for (TaskItem t : tasks) {
            String c = t.getCategoryName();
            if (c == null || c.trim().isEmpty()) continue;
            if (!out.contains(c)) out.add(c);
        }
        return out;
    }

    private static HBox buildHeader(String name,
                                    int count,
                                    SectionConfig config,
                                    List<TaskItem> globalDatabase,
                                    AppStats appStats,
                                    Runnable onToggle,
                                    Runnable onStyleChanged,
                                    boolean collapsed) {

        CategoryStyle style = config.findCategoryStyle(name);
        String bg = style != null && style.getBackgroundColor() != null ? style.getBackgroundColor() : DEFAULT_BG;
        String border = style != null && style.getBorderColor() != null ? style.getBorderColor() : DEFAULT_BORDER;
        String text = style != null && style.getTextColor() != null ? style.getTextColor() : DEFAULT_TEXT;

        // Chevron is a Label (not a Button) so the row's mouse handler is the only click target —
        // no more double-fire that cancels out the toggle.
        Label chevron = new Label(collapsed ? "▶" : "▼");
        chevron.setStyle("-fx-text-fill: " + text + "; -fx-font-weight: bold;");
        chevron.setMinWidth(Region.USE_PREF_SIZE);

        HBox titleBox = new HBox(6);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        if (style != null && style.getIconSymbol() != null && !"None".equals(style.getIconSymbol())) {
            Label icon = new Label(style.getIconSymbol());
            String iconColor = style.getIconColor() != null ? style.getIconColor() : text;
            icon.setStyle("-fx-text-fill: " + iconColor + "; -fx-font-size: 14px;");
            titleBox.getChildren().add(icon);
        }
        Label title = new Label(Lang.CATEGORY_HEADER_COUNT.get(name, count));
        title.setStyle("-fx-text-fill: " + text + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        titleBox.getChildren().add(title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button gearBtn = new Button("⚙");
        gearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + text + "; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 0 4;");
        gearBtn.setTooltip(new Tooltip(Lang.CATEGORY_EDIT_TOOLTIP.get()));
        Runnable openStyleEditor = () -> {
            if (CategoryStyleDialog.show(config, name, globalDatabase, appStats) && onStyleChanged != null) onStyleChanged.run();
        };
        gearBtn.setOnAction(e -> openStyleEditor.run());
        // Swallow the bubbling MouseEvent so the row handler doesn't fire a collapse-toggle when
        // the user clicks the gear.
        gearBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        HBox row = new HBox(6, chevron, titleBox, spacer, gearBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 8, 4, 4));
        String rowStyle = "-fx-background-color: " + bg + "; -fx-background-radius: 4; -fx-border-color: " + border + "; -fx-border-radius: 4; -fx-cursor: hand;";
        row.setStyle(rowStyle);

        // --- Drag a card onto this header to move it into this category. ---
        // Cards put their task id on the dragboard (see TaskCardStyleHelper#setupDragAndDrop).
        final boolean isUncategorized = name.equals(Lang.CATEGORY_UNCATEGORIZED.get());
        row.setOnDragOver(e -> {
            if (e.getGestureSource() != row && e.getDragboard().hasString()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
            }
            e.consume();
        });
        row.setOnDragEntered(e -> {
            if (e.getDragboard().hasString()) row.setStyle(rowStyle + " -fx-border-color: #569CD6; -fx-border-width: 2;");
        });
        row.setOnDragExited(e -> row.setStyle(rowStyle));
        row.setOnDragDropped(e -> {
            javafx.scene.input.Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String draggedId = db.getString();
                TaskItem dragged = null;
                for (TaskItem t : globalDatabase) {
                    if (t.getId().equals(draggedId)) { dragged = t; break; }
                }
                if (dragged != null && config.getId().equals(dragged.getSectionId())) {
                    dragged.setCategoryName(isUncategorized ? null : name);
                    // Mirror the "Move to Category" menu: auto-style the card if the section opts in.
                    com.raeden.ors_to_do.modules.dependencies.ui.menus.TaskContextMenu.applyCategoryAutoStyle(dragged, config);
                    StorageManager.saveTasks(globalDatabase);
                    success = true;
                    if (onStyleChanged != null) onStyleChanged.run();
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });

        row.setOnMouseClicked(e -> {
            // Only a left-click collapses/expands — right-click is reserved for the context menu.
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            // Update chevron locally so the visual matches the new state on the next paint.
            chevron.setText("▶".equals(chevron.getText()) ? "▼" : "▶");
            onToggle.run();
        });

        // Right-click → context menu mirroring the gear action.
        ContextMenu ctx = new ContextMenu();
        MenuItem customizeItem = new MenuItem(Lang.CATEGORY_CUSTOMIZE_MENU.get());
        customizeItem.setOnAction(e -> openStyleEditor.run());

        // "Sync Style" re-applies this category's style to every card in the category.
        MenuItem syncStyleItem = new MenuItem("Sync Style");
        syncStyleItem.setDisable(style == null);
        syncStyleItem.setOnAction(e -> {
            CategoryStyle catStyle = config.findCategoryStyle(name);
            if (catStyle == null) return;
            for (TaskItem t : globalDatabase) {
                if (!config.getId().equals(t.getSectionId())) continue;
                String c = t.getCategoryName();
                if (c != null && c.trim().equals(name)) {
                    com.raeden.ors_to_do.modules.dependencies.ui.menus.TaskContextMenu.applyCategoryStyleTo(t, catStyle);
                }
            }
            StorageManager.saveTasks(globalDatabase);
            if (onStyleChanged != null) onStyleChanged.run();
        });

        ctx.getItems().addAll(customizeItem, syncStyleItem);
        row.setOnContextMenuRequested(e -> {
            ctx.show(row, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        return row;
    }
}
