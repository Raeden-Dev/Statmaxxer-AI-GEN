package com.raeden.ors_to_do.modules.dependencies.ui.layout;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CalendarTask;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.Debuff;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.services.AnalyticsExporter;
import com.raeden.ors_to_do.modules.dependencies.ui.components.DependencyMenuBuilder;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.ColorUtil;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskActionHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Renders a {@link SectionConfig} that is in Calendar Page mode. A dashboard strip (matching other
 * pages) carries month navigation, Today, and Export; a filter row carries the "Show All" task
 * filter (in the same spot as the Order dropdown elsewhere). Below the month grid sits a "Task
 * List" of cards: an add-card plus one card per {@link CalendarTask}. Selecting a card makes it the
 * "brush" — clicking a day then marks/unmarks it (colouring the day and, if the section grants
 * rewards, awarding stat XP / score / debuffs and triggering hooked cards).
 */
public class CalendarPage extends BorderPane {

    private final SectionConfig config;
    private final AppStats appStats;
    private final List<TaskItem> globalDatabase;
    private final Runnable syncCallback;

    private YearMonth currentMonth = YearMonth.now();
    private CalendarTask brushTask;
    private CalendarTask displayFilter;

    private final Label titleLabel = new Label();
    private final Label monthLabel = new Label();
    private final Label hintLabel = new Label();
    private final GridPane grid = new GridPane();
    private final VBox taskListBox = new VBox(8);
    private final ComboBox<CalendarTask> filterBox = new ComboBox<>();

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String COMBO_CSS =
            ".combo-box { -fx-background-color: #2D2D30; -fx-border-color: #555555; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; } " +
            ".combo-box .list-cell { -fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: transparent; } " +
            ".combo-box-popup .list-view { -fx-background-color: #2D2D30; -fx-border-color: #555555; } " +
            ".combo-box-popup .list-view .list-cell { -fx-background-color: #2D2D30; -fx-text-fill: white; } " +
            ".combo-box-popup .list-view .list-cell:filled:hover, .combo-box-popup .list-view .list-cell:filled:selected { -fx-background-color: #569CD6; } " +
            ".combo-box .arrow-button { -fx-background-color: transparent; } .combo-box .arrow { -fx-background-color: #AAAAAA; } " +
            ".custom-menu-btn { -fx-background-color: #2D2D30; -fx-border-color: #555555; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; } " +
            ".custom-menu-btn .label { -fx-text-fill: white; } " +
            ".context-menu { -fx-background-color: #2D2D30; -fx-border-color: #555555; } " +
            ".menu-item .label { -fx-text-fill: white; } .menu-item:focused { -fx-background-color: #3E3E42; }";

    public CalendarPage(SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable syncCallback) {
        this.config = config;
        this.appStats = appStats;
        this.globalDatabase = globalDatabase;
        this.syncCallback = syncCallback;

        // No outer padding: the hosting BorderPane (DynamicModule.mainContent) already insets by
        // 15, matching every other page. Adding our own here would double it and offset the page.
        setStyle("-fx-background-color: #1E1E1E;");

        VBox top = new VBox(10, buildDashboardStrip(), buildFilterRow());
        setTop(top);
        setCenter(buildBody());

        refreshFilterBox();
        renderCalendar();
        renderTaskList();
    }

    // ------------------------------------------------------------------ header

    private HBox buildDashboardStrip() {
        HBox strip = new HBox(15);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(15));
        strip.setStyle("-fx-background-color: #2D2D30; -fx-border-color: #3E3E42; -fx-border-radius: 8; -fx-background-radius: 8;");

        String titleColor = appStats.isMatchTitleColor() ? config.getSidebarColor() : "#569CD6";
        titleLabel.setText(config.getName());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
        VBox titleBox = new VBox(2, titleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button prev = pill("◀");
        prev.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); renderCalendar(); });
        Button next = pill("▶");
        next.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1); renderCalendar(); });
        // Month/year sits between the prev/next arrows.
        monthLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        monthLabel.setAlignment(Pos.CENTER);
        monthLabel.setMinWidth(130);
        HBox monthNav = new HBox(8, prev, monthLabel, next);
        monthNav.setAlignment(Pos.CENTER);

        Button today = pill("Today");
        today.setOnAction(e -> { currentMonth = YearMonth.now(); renderCalendar(); });
        Button export = pill("📊 Export");
        export.setStyle("-fx-background-color: #0E639C; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15; -fx-background-radius: 15; -fx-border-color: #569CD6; -fx-border-radius: 15;");
        export.setOnAction(e -> AnalyticsExporter.exportCalendarAnalytics(config));

        FlowPane badges = new FlowPane(10, 10);
        badges.setAlignment(Pos.CENTER_RIGHT);
        badges.setPrefWrapLength(440);
        badges.getChildren().addAll(monthNav, today, export);

        strip.getChildren().addAll(titleBox, spacer, badges);
        return strip;
    }

    private HBox buildFilterRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 4, 0));

        hintLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 11px; -fx-font-style: italic;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterBox.setPromptText("Show All");
        filterBox.getStylesheets().add(css(COMBO_CSS));
        filterBox.setCellFactory(lv -> taskListCell());
        filterBox.setButtonCell(taskListCell());
        filterBox.setOnAction(e -> { displayFilter = filterBox.getValue(); renderCalendar(); });

        row.getChildren().addAll(hintLabel, spacer, filterBox);
        return row;
    }

    private ScrollPane buildBody() {
        grid.setHgap(6);
        grid.setVgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        Label listTitle = new Label("Task List");
        listTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox content = new VBox(14, grid, new Separator(), listTitle, taskListBox);
        content.setPadding(new Insets(10, 0, 10, 0));

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        sp.setBorder(Border.EMPTY);
        return sp;
    }

    // ------------------------------------------------------------------ calendar grid

    private void renderCalendar() {
        grid.getChildren().clear();
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int c = 0; c < 7; c++) {
            Label dh = new Label(days[c]);
            dh.setMaxWidth(Double.MAX_VALUE);
            dh.setAlignment(Pos.CENTER);
            dh.setStyle("-fx-text-fill: #AAAAAA; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4;");
            grid.add(dh, c, 0);
        }

        LocalDate first = currentMonth.atDay(1);
        int lead = first.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int row = 1, col = lead;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            grid.add(buildDayCell(date, date.equals(today)), col, row);
            if (++col > 6) { col = 0; row++; }
        }
    }

    private Region buildDayCell(LocalDate date, boolean isToday) {
        String iso = date.format(ISO);
        List<String> completed = filteredCompletions(iso);

        VBox cell = new VBox(2);
        cell.setMinHeight(72);
        cell.setPadding(new Insets(4));
        cell.setAlignment(Pos.TOP_RIGHT);

        Label num = new Label(String.valueOf(date.getDayOfMonth()));
        num.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        cell.getChildren().add(num);

        if (config.isCalendarShowDots() && !completed.isEmpty()) {
            FlowPane marks = new FlowPane(3, 3);
            marks.setAlignment(Pos.BOTTOM_LEFT);
            Region grow = new Region(); VBox.setVgrow(grow, Priority.ALWAYS);
            for (String tid : completed) {
                CalendarTask t = config.findCalendarTask(tid);
                if (t == null) continue;
                if (t.hasIcon()) {
                    // Icon replaces the plain dot as this task's indicator on the day.
                    Label ico = new Label(t.getIconSymbol());
                    ico.setStyle("-fx-text-fill: " + t.getIconColor() + "; -fx-font-size: 12px;");
                    marks.getChildren().add(ico);
                } else {
                    marks.getChildren().add(new Circle(4, Color.web(t.getColorHex())));
                }
            }
            cell.getChildren().addAll(grow, marks);
        }

        String bg = (config.isCalendarShowSegments() && !completed.isEmpty()) ? buildSegmentGradient(completed) : "#252526";
        String border = isToday ? "-fx-border-color: #4EC9B0; -fx-border-width: 2;" : "-fx-border-color: #3E3E42; -fx-border-width: 1;";
        String base = "-fx-background-color: " + bg + "; -fx-background-radius: 5; -fx-border-radius: 5; " + border + " -fx-cursor: hand;";
        cell.setStyle(base);
        cell.setOnMouseEntered(e -> cell.setStyle(base + " -fx-effect: dropshadow(gaussian, rgba(86,156,214,0.5), 6, 0, 0, 0);"));
        cell.setOnMouseExited(e -> cell.setStyle(base));
        cell.setOnMouseClicked(e -> onDayClicked(date));
        return cell;
    }

    private String buildSegmentGradient(List<String> ids) {
        StringBuilder sb = new StringBuilder("linear-gradient(to right");
        int n = ids.size();
        for (int i = 0; i < n; i++) {
            CalendarTask t = config.findCalendarTask(ids.get(i));
            String col = t != null ? t.getColorHex() : "#569CD6";
            long start = Math.round(i * 100.0 / n);
            long end = Math.round((i + 1) * 100.0 / n);
            sb.append(", ").append(col).append(" ").append(start).append("%, ").append(col).append(" ").append(end).append("%");
        }
        return sb.append(")").toString();
    }

    private List<String> filteredCompletions(String iso) {
        List<String> all = config.getCompletedTaskIds(iso);
        if (displayFilter == null) return all;
        return all.contains(displayFilter.getId()) ? List.of(displayFilter.getId()) : List.of();
    }

    // ------------------------------------------------------------------ interaction & rewards

    private void onDayClicked(LocalDate date) {
        if (brushTask == null) {
            hintLabel.setText("Select a task card below first, then click a day to mark it.");
            return;
        }
        LocalDate today = LocalDate.now();
        if (!config.isAllowCalendarManipulation() && !date.equals(today)) {
            hintLabel.setText("This calendar only allows marking today. Enable \"Allow Calendar Manipulation\" in Edit Section to change other days.");
            return;
        }
        String iso = date.format(ISO);
        boolean nowCompleted = config.toggleCompletion(iso, brushTask.getId());
        applyRewards(brushTask, nowCompleted);
        StorageManager.saveStats(appStats);
        hintLabel.setText("");
        renderCalendar();
        if (syncCallback != null) syncCallback.run();
    }

    /**
     * Applies (or partially reverses) a calendar task's rewards. Stat XP and score are reversed on
     * un-mark; debuffs and hooked-card triggers only fire on mark (they aren't auto-undone).
     */
    private void applyRewards(CalendarTask task, boolean gained) {
        if (!config.isCalendarGrantsXp()) return;

        // Stat XP (multiple stats) — reversible.
        for (var entry : task.getStatRewards().entrySet()) {
            for (CustomStat stat : appStats.getCustomStats()) {
                if (stat.getId().equals(entry.getKey())) {
                    int delta = gained ? entry.getValue() : -entry.getValue();
                    stat.setCurrentAmount(Math.max(0, stat.getCurrentAmount() + delta));
                    break;
                }
            }
        }

        // Stat Max-Cap rewards (multiple stats) — reversible.
        for (var entry : task.getStatCapRewards().entrySet()) {
            for (CustomStat stat : appStats.getCustomStats()) {
                if (stat.getId().equals(entry.getKey())) {
                    int delta = gained ? entry.getValue() : -entry.getValue();
                    stat.setMaxCap(Math.max(0, stat.getMaxCap() + delta));
                    break;
                }
            }
        }

        // Global score — reversible.
        if (task.getRewardPoints() != 0) {
            int delta = gained ? task.getRewardPoints() : -task.getRewardPoints();
            appStats.setGlobalScore(Math.max(0, appStats.getGlobalScore() + delta));
        }

        if (!gained) return; // debuffs & hooks only fire on mark

        // Debuffs — reuse the standard inflict path via a throwaway carrier task.
        if (!task.getInflictedDebuffIds().isEmpty()) {
            TaskItem carrier = new TaskItem("", null, (String) null);
            carrier.setInflictedDebuffIds(new ArrayList<>(task.getInflictedDebuffIds()));
            TaskActionHandler.applyInflictedDebuffs(carrier, appStats);
        }

        // Hooked cards — complete normal cards, increment counter/repeating cards.
        boolean tasksChanged = false;
        for (String hookedId : task.getHookedTaskIds()) {
            TaskItem t = findTask(hookedId);
            if (t == null) continue;
            if (t.isCounterMode()) {
                int max = t.getMaxCount();
                int next = t.getCurrentCount() + 1;
                if (max > 0) next = Math.min(next, max);
                t.setCurrentCount(next);
                if (max > 0 && next >= max) t.setFinished(true);
                tasksChanged = true;
            } else if (t.isRepeatingMode()) {
                t.setRepetitionCount(t.getRepetitionCount() + 1);
                tasksChanged = true;
            } else if (!t.isFinished()) {
                t.setFinished(true);
                tasksChanged = true;
            }
        }
        if (tasksChanged) StorageManager.saveTasks(globalDatabase);
    }

    private TaskItem findTask(String id) {
        if (id == null || globalDatabase == null) return null;
        for (TaskItem t : globalDatabase) if (id.equals(t.getId())) return t;
        return null;
    }

    // ------------------------------------------------------------------ task list

    private void renderTaskList() {
        taskListBox.getChildren().clear();
        taskListBox.getChildren().add(buildAddCard());
        for (CalendarTask t : config.getCalendarTasks()) {
            taskListBox.getChildren().add(buildTaskCard(t));
        }
    }

    private Region buildAddCard() {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #232323; -fx-border-color: #555555; -fx-border-style: segments(6, 6, 6, 6); -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        Label plus = new Label("＋ Add Task");
        plus.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold; -fx-font-size: 14px;");
        card.getChildren().add(plus);
        card.setOnMouseClicked(e -> showTaskEditDialog(null));
        return card;
    }

    private Region buildTaskCard(CalendarTask t) {
        boolean selected = brushTask != null && brushTask.getId().equals(t.getId());

        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        String base = "-fx-background-color: #2D2D30; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: " + (selected ? 2 : 1)
                + "; -fx-border-color: " + (selected ? t.getColorHex() : "#3E3E42") + "; -fx-cursor: hand;";
        card.setStyle(base);

        StackPane swatch = new StackPane(new Circle(9, Color.web(t.getColorHex())));
        if (t.hasIcon()) {
            Label ico = new Label(t.getIconSymbol());
            ico.setStyle("-fx-text-fill: " + t.getIconColor() + "; -fx-font-size: 12px;");
            swatch.getChildren().add(ico);
        }

        Label name = new Label(t.getName());
        name.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        String summary = rewardSummary(t);
        VBox textBox = new VBox(2, name);
        if (!summary.isEmpty()) {
            Label sub = new Label(summary);
            sub.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;");
            textBox.getChildren().add(sub);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button gear = new Button("⚙");
        gear.setStyle("-fx-background-color: transparent; -fx-text-fill: #AAAAAA; -fx-font-size: 15px; -fx-cursor: hand;");
        Tooltip.install(gear, new Tooltip("Configure / edit this task"));
        gear.setOnAction(e -> showTaskEditDialog(t));
        // Stop the gear's click from bubbling to the card (which would toggle the brush). The
        // button's ActionEvent fires on release, before the CLICKED event we consume here.
        gear.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        card.getChildren().addAll(swatch, textBox, spacer, gear);

        // Left-click selects the brush (click again to deselect). Right-click is reserved for the
        // context menu, so ignore any non-primary button here.
        card.setOnMouseClicked(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            brushTask = selected ? null : t;
            hintLabel.setText(brushTask == null ? "" : "Marking with \"" + t.getName() + "\" — click a day above.");
            renderTaskList();
        });

        // Right-click: Edit (top) + Permanently Remove.
        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Task");
        editItem.setOnAction(e -> showTaskEditDialog(t));
        MenuItem removeItem = new MenuItem("Permanently Remove Task");
        removeItem.setOnAction(e -> deleteTask(t));
        menu.getItems().addAll(editItem, removeItem);
        card.setOnContextMenuRequested(e -> menu.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private String rewardSummary(CalendarTask t) {
        List<String> parts = new ArrayList<>();
        java.util.Set<String> statIds = new java.util.HashSet<>(t.getStatRewards().keySet());
        statIds.addAll(t.getStatCapRewards().keySet());
        int statCount = statIds.size();
        if (statCount > 0) parts.add(statCount + " stat" + (statCount > 1 ? "s" : ""));
        if (t.getRewardPoints() > 0) parts.add("+" + t.getRewardPoints() + " pts");
        int d = t.getInflictedDebuffIds().size();
        if (d > 0) parts.add(d + " debuff" + (d > 1 ? "s" : ""));
        int h = t.getHookedTaskIds().size();
        if (h > 0) parts.add(h + " hook" + (h > 1 ? "s" : ""));
        return String.join(" · ", parts);
    }

    private void deleteTask(CalendarTask t) {
        config.getCalendarTasks().removeIf(x -> x.getId().equals(t.getId()));
        config.getCalendarCompletions().values().forEach(ids -> ids.remove(t.getId()));
        config.getCalendarCompletions().entrySet().removeIf(en -> en.getValue().isEmpty());
        if (brushTask != null && brushTask.getId().equals(t.getId())) brushTask = null;
        if (displayFilter != null && displayFilter.getId().equals(t.getId())) displayFilter = null;
        StorageManager.saveStats(appStats);
        refreshFilterBox(); renderTaskList(); renderCalendar();
    }

    private void refreshFilterBox() {
        filterBox.getItems().setAll(new ArrayList<>());
        filterBox.getItems().add(null); // "Show All"
        filterBox.getItems().addAll(config.getCalendarTasks());
        filterBox.setValue(displayFilter);
    }

    // ------------------------------------------------------------------ add / edit dialog

    private void showTaskEditDialog(CalendarTask existing) {
        boolean isNew = existing == null;
        CalendarTask task = isNew ? new CalendarTask("") : existing;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Add Calendar Task" : "Edit Calendar Task");
        TaskDialogs.styleDialog(dialog);

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(10));
        ColumnConstraints c1 = new ColumnConstraints(); c1.setMinWidth(150);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c1, c2);
        int[] r = {0};

        TextField nameField = new TextField(task.getName());
        nameField.setPromptText("e.g. Gym, 8 Hour Work");
        addRow(g, r, "Name:", nameField);

        ColorPicker colorPicker = new ColorPicker(Color.web(task.getColorHex()));
        addRow(g, r, "Color:", colorPicker);

        ComboBox<String> iconBox = new ComboBox<>();
        iconBox.getItems().addAll(TaskDialogs.ICON_LIST);
        iconBox.setValue(task.getIconSymbol());
        iconBox.getStylesheets().add(css(COMBO_CSS));
        ColorPicker iconColor = new ColorPicker(Color.web(task.getIconColor()));
        HBox iconRow = new HBox(10, iconBox, iconColor);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        addRow(g, r, "Icon & Color:", iconRow);

        Button randomBtn = new Button("🎲 Randomize Style");
        randomBtn.setMaxWidth(Double.MAX_VALUE);
        randomBtn.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-cursor: hand;");
        randomBtn.setOnAction(e -> {
            Random rand = new Random();
            double hue = rand.nextDouble() * 360.0;
            colorPicker.setValue(Color.hsb(hue, 0.7, 0.55));
            iconBox.setValue(TaskDialogs.ICON_LIST[rand.nextInt(TaskDialogs.ICON_LIST.length - 1) + 1]);
            iconColor.setValue(Color.hsb(hue, 0.5, 0.95));
        });
        g.add(randomBtn, 1, r[0]); r[0]++;

        // --- Rewards (only meaningful when the section grants rewards on completion) ---
        boolean statsAvailable = appStats.isGlobalStatsEnabled() && !appStats.getCustomStats().isEmpty();
        boolean debuffsAvailable = appStats.getDebuffTemplates() != null && !appStats.getDebuffTemplates().isEmpty();

        g.add(new Separator(), 0, r[0], 2, 1); r[0]++;
        Label rewardsHeader = new Label("Rewards on completion");
        rewardsHeader.setStyle("-fx-text-fill: #B5CEA8; -fx-font-weight: bold;");
        g.add(rewardsHeader, 0, r[0], 2, 1); r[0]++;
        if (!config.isCalendarGrantsXp()) {
            Label off = new Label("Note: enable \"Grant rewards on completion\" in Edit Section for these to apply.");
            off.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 11px;"); off.setWrapText(true);
            g.add(off, 0, r[0], 2, 1); r[0]++;
        }

        TextField scoreField = new TextField(task.getRewardPoints() > 0 ? String.valueOf(task.getRewardPoints()) : "");
        scoreField.setPromptText("+ Global score points");
        addRow(g, r, "Score Reward:", scoreField);

        java.util.Map<String, TextField> statFields = new java.util.HashMap<>();
        java.util.Map<String, TextField> statCapFields = new java.util.HashMap<>();
        if (statsAvailable) {
            Label statHeader = new Label("Stat Rewards");
            statHeader.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold; -fx-font-size: 12px;");
            g.add(statHeader, 0, r[0], 2, 1); r[0]++;

            Label rLbl = new Label("+ XP"); rLbl.setPrefWidth(80); rLbl.setStyle("-fx-text-fill: #4EC9B0;");
            Label cpLbl = new Label("▲ Max Cap"); cpLbl.setPrefWidth(80); cpLbl.setStyle("-fx-text-fill: #C586C0;");
            HBox colHeaders = new HBox(10, rLbl, cpLbl);
            g.add(colHeaders, 1, r[0]); r[0]++;

            for (CustomStat stat : appStats.getCustomStats()) {
                TextField f = new TextField();
                f.setPrefWidth(80);
                int existingVal = task.getStatRewards().getOrDefault(stat.getId(), 0);
                if (existingVal > 0) f.setText(String.valueOf(existingVal));
                f.setPromptText("+ XP");
                statFields.put(stat.getId(), f);

                TextField cap = new TextField();
                cap.setPrefWidth(80);
                int existingCap = task.getStatCapRewards().getOrDefault(stat.getId(), 0);
                if (existingCap > 0) cap.setText(String.valueOf(existingCap));
                cap.setPromptText("▲ Cap");
                statCapFields.put(stat.getId(), cap);

                HBox fieldRow = new HBox(10, f, cap);
                fieldRow.setAlignment(Pos.CENTER_LEFT);
                addRow(g, r, stat.getName() + ":", fieldRow);
            }
        }

        MenuButton debuffMenu = null;
        java.util.List<String> selectedDebuffs = new ArrayList<>(task.getInflictedDebuffIds());
        if (debuffsAvailable) {
            debuffMenu = new MenuButton(debuffLabel(selectedDebuffs.size()));
            debuffMenu.getStyleClass().add("custom-menu-btn");
            debuffMenu.setMaxWidth(Double.MAX_VALUE);
            final MenuButton dm = debuffMenu;
            for (Debuff d : appStats.getDebuffTemplates()) {
                String icon = (d.getIconSymbol() != null && !d.getIconSymbol().equals("None")) ? d.getIconSymbol() + " " : "⚠ ";
                CheckBox cb = new CheckBox(icon + d.getName());
                cb.setStyle("-fx-text-fill: white;");
                cb.setSelected(selectedDebuffs.contains(d.getId()));
                cb.setOnAction(e -> {
                    if (cb.isSelected()) { if (!selectedDebuffs.contains(d.getId())) selectedDebuffs.add(d.getId()); }
                    else selectedDebuffs.remove(d.getId());
                    dm.setText(debuffLabel(selectedDebuffs.size()));
                });
                CustomMenuItem mi = new CustomMenuItem(cb);
                mi.setHideOnClick(false);
                dm.getItems().add(mi);
            }
            addRow(g, r, "Inflict Debuffs:", debuffMenu);
        }

        // --- Hooked cards ---
        List<String> hookedIds = new ArrayList<>(task.getHookedTaskIds());
        MenuButton hookMenu = null;
        if (globalDatabase != null && !globalDatabase.isEmpty()) {
            g.add(new Separator(), 0, r[0], 2, 1); r[0]++;
            TaskItem dummyOwner = new TaskItem("", null, (String) null);
            hookMenu = DependencyMenuBuilder.build(dummyOwner, appStats, globalDatabase, hookedIds);
            addRow(g, r, "Hook Cards:", hookMenu);
            Label hookDesc = new Label("Completing this calendar task will complete hooked cards and +1 to hooked counter/repeating cards.");
            hookDesc.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;"); hookDesc.setWrapText(true);
            g.add(hookDesc, 1, r[0]); r[0]++;
        }

        for (javafx.scene.Node n : g.getChildren()) {
            if (n instanceof Label && ((Label) n).getStyle().isEmpty()) ((Label) n).setStyle("-fx-text-fill: #DDDDDD;");
        }

        ScrollPane sp = new ScrollPane(g);
        sp.setFitToWidth(true);
        sp.setPrefSize(560, 520);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        sp.setBorder(Border.EMPTY);
        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final MenuButton debuffMenuF = debuffMenu;
        final MenuButton hookMenuF = hookMenu;
        dialog.showAndWait().ifPresent(res -> {
            if (res != ButtonType.OK) return;
            String nm = nameField.getText() == null ? "" : nameField.getText().trim();
            if (nm.isEmpty()) return;
            task.setName(nm);
            task.setColorHex(ColorUtil.toHex(colorPicker.getValue()));
            task.setIconSymbol(iconBox.getValue());
            task.setIconColor(ColorUtil.toHex(iconColor.getValue()));
            task.setRewardPoints(parseInt(scoreField.getText()));

            java.util.Map<String, Integer> rewards = new java.util.HashMap<>();
            for (var en : statFields.entrySet()) {
                int v = parseInt(en.getValue().getText());
                if (v > 0) rewards.put(en.getKey(), v);
            }
            task.setStatRewards(rewards);

            java.util.Map<String, Integer> capRewards = new java.util.HashMap<>();
            for (var en : statCapFields.entrySet()) {
                int v = parseInt(en.getValue().getText());
                if (v > 0) capRewards.put(en.getKey(), v);
            }
            task.setStatCapRewards(capRewards);

            if (debuffMenuF != null) task.setInflictedDebuffIds(selectedDebuffs);
            if (hookMenuF != null) task.setHookedTaskIds(DependencyMenuBuilder.stripStale(hookedIds, globalDatabase));

            if (isNew) config.getCalendarTasks().add(task);
            StorageManager.saveStats(appStats);
            refreshFilterBox(); renderTaskList(); renderCalendar();
        });
    }

    // ------------------------------------------------------------------ helpers

    private void addRow(GridPane g, int[] r, String label, javafx.scene.Node control) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #DDDDDD;");
        g.add(l, 0, r[0]);
        g.add(control, 1, r[0]);
        r[0]++;
    }

    private String debuffLabel(int n) { return n == 0 ? "Select Debuffs to Inflict" : "Debuffs to Inflict (" + n + ")"; }

    private int parseInt(String s) {
        try { return Math.max(0, Integer.parseInt(s.trim())); } catch (Exception e) { return 0; }
    }

    private static String css(String raw) {
        return "data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(raw.getBytes());
    }

    private ListCell<CalendarTask> taskListCell() {
        return new ListCell<>() {
            @Override protected void updateItem(CalendarTask item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "Show All" : item.getName()));
                setStyle("-fx-text-fill: white;");
            }
        };
    }

    private Button pill(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #1E1E1E; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 15; -fx-border-color: #555555; -fx-border-radius: 15;");
        return b;
    }
}
