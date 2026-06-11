package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.Profile;
import com.raeden.ors_to_do.dependencies.storage.ProfileManager;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Settings section for managing data {@link Profile}s. Each profile is a fully separate world (own
 * tasks, sections, stats). Switching is delegated to the app via {@code onSwitchProfile}, which
 * reloads the chosen profile's data and rebuilds the UI. The active profile persists across
 * launches.
 */
public class ProfileManagerPanel extends VBox {

    private final Consumer<String> onSwitchProfile;
    private final ComboBox<Profile> profileBox = new ComboBox<>();
    private final Label activeLabel = new Label();

    public ProfileManagerPanel(Consumer<String> onSwitchProfile) {
        super(12);
        this.onSwitchProfile = onSwitchProfile;
        setStyle("-fx-border-color: #3E3E42; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");

        Label header = new Label("Profiles");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        Label desc = new Label(Lang.PROFILE_DESC.get());
        desc.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;");
        desc.setWrapText(true);

        activeLabel.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold;");

        profileBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(profileBox, Priority.ALWAYS);
        profileBox.setCellFactory(lv -> profileCell());
        profileBox.setButtonCell(profileCell());
        profileBox.setStyle("-fx-background-color: #2D2D30; -fx-border-color: #555555; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");

        Button switchBtn = button("Switch", "#0E639C");
        switchBtn.setOnAction(e -> doSwitch());

        Button newBtn = button("New", "#22543D");
        newBtn.setOnAction(e -> doNew());

        Button renameBtn = button("Rename", "#3E3E42");
        renameBtn.setOnAction(e -> doRename());

        Button deleteBtn = button("Delete", "#5A1D1D");
        deleteBtn.setOnAction(e -> doDelete());

        HBox controls = new HBox(8, profileBox, switchBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(8, newBtn, renameBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, desc, activeLabel, controls, actions);
        refresh();
    }

    private void refresh() {
        profileBox.getItems().setAll(ProfileManager.getProfiles());
        String activeId = ProfileManager.getActiveId();
        for (Profile p : profileBox.getItems()) {
            if (p.getId().equals(activeId)) { profileBox.setValue(p); break; }
        }
        activeLabel.setText(Lang.PROFILE_ACTIVE_LABEL.get(ProfileManager.getActiveProfile().getName()));
    }

    private void doSwitch() {
        Profile sel = profileBox.getValue();
        if (sel == null || sel.getId().equals(ProfileManager.getActiveId())) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                Lang.PROFILE_SWITCH_BODY.get(sel.getName()),
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(Lang.PROFILE_SWITCH_HEADER.get());
        TaskDialogs.styleDialog(confirm);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK && onSwitchProfile != null) {
            onSwitchProfile.accept(sel.getId());
        }
    }

    private void doNew() {
        TextInputDialog d = new TextInputDialog(Lang.PROFILE_NEW_HEADER.get());
        d.setTitle(Lang.PROFILE_NEW_HEADER.get());
        d.setHeaderText(Lang.PROFILE_NEW_PROMPT.get());
        TaskDialogs.styleDialog(d);
        d.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            Profile created = ProfileManager.createProfile(name.trim());
            refresh();
            // Offer to switch into the freshly created (empty) profile.
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    Lang.PROFILE_CREATED_SWITCH_BODY.get(created.getName()),
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(Lang.PROFILE_CREATED_SWITCH_HEADER.get());
            TaskDialogs.styleDialog(confirm);
            confirm.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES && onSwitchProfile != null) onSwitchProfile.accept(created.getId());
            });
        });
    }

    private void doRename() {
        Profile sel = profileBox.getValue();
        if (sel == null) return;
        TextInputDialog d = new TextInputDialog(sel.getName());
        d.setTitle(Lang.PROFILE_RENAME_HEADER.get());
        d.setHeaderText(Lang.PROFILE_RENAME_PROMPT.get(sel.getName()));
        TaskDialogs.styleDialog(d);
        d.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            ProfileManager.renameProfile(sel.getId(), name.trim());
            refresh();
        });
    }

    private void doDelete() {
        Profile sel = profileBox.getValue();
        if (sel == null) return;
        if (ProfileManager.DEFAULT_ID.equals(sel.getId())) {
            info(Lang.PROFILE_CANT_DELETE_DEFAULT.get());
            return;
        }
        if (sel.getId().equals(ProfileManager.getActiveId())) {
            info(Lang.PROFILE_CANT_DELETE_ACTIVE.get());
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.WARNING,
                Lang.PROFILE_DELETE_BODY.get(sel.getName()),
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(Lang.PROFILE_DELETE_HEADER.get());
        TaskDialogs.styleDialog(confirm);
        confirm.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {
                if (ProfileManager.deleteProfile(sel.getId())) refresh();
                else info(Lang.PROFILE_DELETE_FAILED.get());
            }
        });
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        TaskDialogs.styleDialog(a);
        a.showAndWait();
    }

    private ListCell<Profile> profileCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Profile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
                setStyle("-fx-text-fill: white;");
            }
        };
    }

    private Button button(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
        return b;
    }
}
