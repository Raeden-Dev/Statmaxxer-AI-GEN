package com.raeden.ors_to_do.modules.dependencies.ui.dialogs;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.StatLedgerEntry;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Stat-page History: a chronological ledger of every Custom Stat / Global Score point/EXP change
 * and its source (task completion, miss penalty, reward purchase, calendar mark, focus session …).
 * Reads {@link AppStats#getStatLedger()} newest-first. The ledger is recorded going forward, so it
 * shows activity from the point the feature was added onward.
 */
public class StatHistoryDialog {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /** {@code globalDatabase} is retained for call-site compatibility; the ledger is the source now. */
    public static void show(AppStats appStats, List<TaskItem> globalDatabase) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Stat History");
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(8);
        content.setPadding(new Insets(10));

        List<StatLedgerEntry> ledger = appStats.getStatLedger();

        if (ledger.isEmpty()) {
            Label empty = new Label("No stat changes recorded yet.\nRewards and penalties to your stats and points will appear here as you earn them.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #858585; -fx-font-style: italic;");
            content.getChildren().add(empty);
        } else {
            // Newest first.
            for (int i = ledger.size() - 1; i >= 0; i--) {
                content.getChildren().add(buildRow(ledger.get(i)));
            }
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(480, 500);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        scroll.setBorder(Border.EMPTY);

        String scrollCss = ".scroll-bar:vertical { -fx-background-color: transparent; -fx-pref-width: 5; } " +
                ".scroll-bar:vertical .track { -fx-background-color: transparent; -fx-border-color: transparent; } " +
                ".scroll-bar:vertical .thumb { -fx-background-color: #555555; -fx-background-radius: 5; }";
        scroll.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(scrollCss.getBytes()));

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static Region buildRow(StatLedgerEntry e) {
        boolean isCap = "Max Cap".equals(e.getUnit());
        boolean gain = e.isGain();

        // Colour: caps purple, gains teal, losses red.
        String accent = isCap ? "#C586C0" : (gain ? "#4EC9B0" : "#E06666");
        String badgeBg = isCap ? "#2D1E2D" : (gain ? "#1A332E" : "#4A1A1A");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: #2D2D30; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1; -fx-border-color: " + accent + ";");

        // Badge: signed amount + unit + stat name.
        Label badge = new Label(formatBadge(e));
        badge.setMinWidth(Region.USE_PREF_SIZE);
        badge.setStyle("-fx-text-fill: " + accent + "; -fx-background-color: " + badgeBg + "; -fx-padding: 2 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");

        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        Label source = new Label(e.getSource());
        source.setWrapText(true);
        source.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 12px;");
        Label date = new Label(e.getDate() != null ? e.getDate().format(FMT) : "");
        date.setStyle("-fx-text-fill: #858585; -fx-font-size: 10px;");
        textBox.getChildren().addAll(source, date);

        row.getChildren().addAll(badge, textBox);
        return row;
    }

    private static String formatBadge(StatLedgerEntry e) {
        String sign = e.getAmount() > 0 ? "+" : "";
        String amount = sign + e.getAmount();
        if ("Max Cap".equals(e.getUnit())) {
            return "▲ " + amount + " Max " + e.getStatName();
        }
        if (e.isGlobalScore()) {
            return amount + " pts";
        }
        return amount + " " + e.getStatName() + " " + e.getUnit();
    }
}
