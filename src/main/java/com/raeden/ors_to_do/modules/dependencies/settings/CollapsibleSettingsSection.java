package com.raeden.ors_to_do.modules.dependencies.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Wraps a settings panel with a hide/show toggle. When expanded, the panel is shown with a small
 * "🙈 Hide" eye pinned to its top-right corner. When collapsed, the panel is replaced by a
 * full-width, task-card-height "👁 Show &lt;title&gt;" button so the Settings page keeps a
 * consistent card-like style. Collapse state is reported back via {@code onToggle} for persistence.
 */
public class CollapsibleSettingsSection extends VBox {

    public CollapsibleSettingsSection(String title, Region body, boolean collapsed, Consumer<Boolean> onToggle) {
        // --- Expanded view: the panel body with a small eye toggle in its top-right corner. ---
        Button hideBtn = new Button(com.raeden.ors_to_do.i18n.Lang.SETTINGS_HIDE_SECTION.get());
        hideBtn.setFocusTraversable(false);
        hideBtn.setStyle("-fx-background-color: #2D2D30; -fx-border-color: #555555; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 2 8;");
        Tooltip.install(hideBtn, new Tooltip(com.raeden.ors_to_do.i18n.Lang.SETTINGS_HIDE_TOOLTIP.get()));
        StackPane.setAlignment(hideBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(hideBtn, new Insets(8, 8, 0, 0));

        StackPane expanded = new StackPane(body, hideBtn);

        // --- Collapsed view: a full-width, card-height button to bring the section back. ---
        Button showBtn = new Button(com.raeden.ors_to_do.i18n.Lang.SETTINGS_SHOW_SECTION.get(title));
        showBtn.setFocusTraversable(false);
        showBtn.setMaxWidth(Double.MAX_VALUE);
        showBtn.setMinHeight(48);
        showBtn.setPrefHeight(48);
        showBtn.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-border-color: #3E3E42; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        Tooltip.install(showBtn, new Tooltip(com.raeden.ors_to_do.i18n.Lang.SETTINGS_SHOW_TOOLTIP.get()));

        getChildren().addAll(expanded, showBtn);

        boolean[] state = { collapsed };
        Runnable apply = () -> {
            boolean hidden = state[0];
            expanded.setVisible(!hidden);
            expanded.setManaged(!hidden);
            showBtn.setVisible(hidden);
            showBtn.setManaged(hidden);
        };
        apply.run();

        hideBtn.setOnAction(e -> {
            state[0] = true;
            apply.run();
            if (onToggle != null) onToggle.accept(true);
        });
        showBtn.setOnAction(e -> {
            state[0] = false;
            apply.run();
            if (onToggle != null) onToggle.accept(false);
        });
    }
}
