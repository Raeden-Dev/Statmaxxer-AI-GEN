package com.raeden.ors_to_do.modules.dependencies.ui.cards;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class StatCard extends VBox {

    public StatCard(CustomStat stat, AppStats appStats, Runnable onUpdate) {
        super();

        // Dynamic Styling from Config
        String bgColor = stat.getBackgroundColor() != null && !stat.getBackgroundColor().equals("transparent") ? stat.getBackgroundColor() : "#2D2D30";
        String txtColor = stat.getTextColor() != null && !stat.getTextColor().equals("transparent") ? stat.getTextColor() : "#FFFFFF";

        // Main Row Container
        HBox mainRow = new HBox(10);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setPadding(new Insets(10, 15, 10, 10));

        // Apply background and border styling
        String defaultStyle = "-fx-background-color: " + bgColor + "; -fx-background-radius: 5; -fx-border-color: " + txtColor + "44; -fx-border-radius: 5;";
        String hoverStyle = "-fx-background-color: " + bgColor + "; -fx-background-radius: 5; -fx-border-color: " + txtColor + "; -fx-border-radius: 5;";
        mainRow.setStyle(defaultStyle);

        // 1. Icon
        if (stat.getIconSymbol() != null && !stat.getIconSymbol().equals("None") && !stat.getIconSymbol().isEmpty()) {
            Label iconLabel = new Label(stat.getIconSymbol());
            iconLabel.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-size: 14px;");
            mainRow.getChildren().add(iconLabel);
        }

        // 2. Left Side Color Bar
        Rectangle sideBar = new Rectangle(4, 20, Color.web(txtColor));
        sideBar.setArcWidth(3);
        sideBar.setArcHeight(3);
        mainRow.getChildren().add(sideBar);

        // 3. Name Label
        Label nameLabel = new Label(stat.getName());
        nameLabel.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        mainRow.getChildren().add(nameLabel);

        // 4. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        mainRow.getChildren().add(spacer);

        // 5. Help "?"
        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: #1E1E1E; -fx-text-fill: " + txtColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: " + txtColor + "88; -fx-border-radius: 50; -fx-background-radius: 50; -fx-min-width: 20px; -fx-min-height: 20px; -fx-max-width: 20px; -fx-max-height: 20px; -fx-padding: 0;");
        helpBtn.setOnAction(e -> showStatDescriptionDialog(stat, txtColor, bgColor));

        // 6. Right Side: Amount / Max Label
        Label amountLabel = new Label(String.valueOf(stat.getCurrentAmount()));
        amountLabel.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        int effectiveMax = stat.getEffectiveMaxCap(appStats.getActiveDebuffs());
        Label maxLabel = new Label(effectiveMax > 0 ? "/ " + effectiveMax : "");
        maxLabel.setStyle("-fx-text-fill: #858585; -fx-font-size: 12px; -fx-padding: 3 0 0 0;");

        // 6b. EXP toggle chevron (only for EXP-enabled stats)
        boolean barVisible = stat.isUseExp() && stat.isExpBarVisible(appStats.isShowStatExpBars());
        if (stat.isUseExp()) {
            Button chevron = new Button(barVisible ? "▾" : "▸");
            chevron.setStyle("-fx-background-color: transparent; -fx-text-fill: " + txtColor + "; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0 4;");
            Tooltip.install(chevron, new Tooltip(com.raeden.ors_to_do.i18n.Lang.STAT_EXP_BAR_TOOLTIP.get()));
            chevron.setOnAction(e -> {
                stat.toggleExpBar(appStats.isShowStatExpBars());
                StorageManager.saveStats(appStats);
                if (onUpdate != null) onUpdate.run();
            });
            mainRow.getChildren().add(chevron);
        }

        mainRow.getChildren().addAll(helpBtn, amountLabel, maxLabel);
        getChildren().add(mainRow);

        // EXP progress bar (Stat page) — rendered as a small card: dark-gray fill, rounded edges,
        // a bright outline matching the stat colour, and the level/EXP text wrapped inside it.
        if (barVisible) {
            int per = stat.getExpPerLevel();
            int cur = Math.max(0, Math.min(stat.getCurrentExp(), per));
            double frac = per > 0 ? (double) cur / per : 0.0;

            ProgressBar bar = new ProgressBar(frac);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(7);
            String pbCss = ".progress-bar { -fx-accent: " + txtColor + "; } " +
                    ".progress-bar > .track { -fx-background-color: #1E1E1E; -fx-background-radius: 5; -fx-border-color: " + txtColor + "33; -fx-border-radius: 5; } " +
                    ".progress-bar > .bar { -fx-background-radius: 5; -fx-background-insets: 1; }";
            bar.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(pbCss.getBytes()));

            Label expLabel = new Label(com.raeden.ors_to_do.i18n.Lang.STAT_EXP_BAR_LABEL.get(stat.getCurrentAmount(), stat.getCurrentExp(), per));
            expLabel.setStyle("-fx-text-fill: #DDDDDD; -fx-font-size: 10px; -fx-font-weight: bold;");

            // Same width as the stat card row above (no side margins), just a compact box.
            VBox expBox = new VBox(4, bar, expLabel);
            expBox.setPadding(new Insets(6, 10, 6, 10));
            expBox.setStyle("-fx-background-color: #2A2A2A; -fx-background-radius: 5; -fx-background-insets: 0; "
                    + "-fx-border-color: " + txtColor + "; -fx-border-width: 1; -fx-border-radius: 5;");
            VBox.setMargin(expBox, new Insets(3, 0, 0, 0));
            getChildren().add(expBox);
        }

        // Subtle hover effect
        mainRow.setOnMouseEntered(e -> mainRow.setStyle(hoverStyle));
        mainRow.setOnMouseExited(e -> mainRow.setStyle(defaultStyle));
    }

    private void showStatDescriptionDialog(CustomStat stat, String txtColor, String bgColor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Stat Info");
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_LEFT);
        content.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + txtColor + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");

        content.setPrefWidth(500);
        content.setMinWidth(500);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(stat.getIconSymbol());
        icon.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-size: 24px;");

        Label title = new Label(stat.getName());
        title.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        title.setWrapText(true);
        title.setPrefWidth(410);

        header.getChildren().addAll(icon, title);

        // --- BULLETPROOF TEXT WRAPPING FIX ---
        // Using a Label and forcing its MinHeight ensures JavaFX doesn't cut it off early
        Label descLabel = new Label(stat.getDescription() != null && !stat.getDescription().isEmpty() ? stat.getDescription() : "No description provided.");
        descLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
        descLabel.setWrapText(true);
        descLabel.setPrefWidth(440);
        descLabel.setMinHeight(Region.USE_PREF_SIZE); // Forces the label to calculate its full height

        ScrollPane descScroll = new ScrollPane(descLabel);
        descScroll.setFitToWidth(true);
        descScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        descScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        descScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        descScroll.setBorder(Border.EMPTY);
        descScroll.setMaxHeight(400); // Increased so the dialog has much more vertical room before scrolling

        String scrollCss = ".scroll-bar:vertical { -fx-background-color: transparent; -fx-pref-width: 5; } " +
                ".scroll-bar:vertical .track { -fx-background-color: transparent; -fx-border-color: transparent; } " +
                ".scroll-bar:vertical .thumb { -fx-background-color: " + txtColor + "88; -fx-background-radius: 5; }";
        descScroll.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(scrollCss.getBytes()));

        VBox lifetimeBox = new VBox(5);
        lifetimeBox.setStyle("-fx-background-color: #1E1E1E99; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: " + txtColor + "44; -fx-border-radius: 5;");

        Label earned = new Label("▲ Lifetime Earned: " + stat.getLifetimeEarned());
        earned.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label lost = new Label("▼ Lifetime Lost: " + stat.getLifetimeLost());
        lost.setStyle("-fx-text-fill: #FF6666; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label peak = new Label("⭐ Max Level Reached: " + stat.getMaxLevelReached());
        peak.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 13px;");

        lifetimeBox.getChildren().addAll(earned, lost, peak);

        content.getChildren().addAll(header, new Separator(), descScroll, lifetimeBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Allow the dialog pane to calculate its height automatically based on the label
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setMinWidth(500);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1E1E;");
        dialog.showAndWait();
    }
}