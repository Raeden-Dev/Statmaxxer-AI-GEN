package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.services.GoogleDriveSyncManager;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Settings card for Google Drive sync: connect / disconnect a Google account and trigger a manual
 * sync. All data files are mirrored to a {@code statmaxxer-data/} folder in the connected account's
 * Drive. Shows a clear "not configured" state until the user supplies their OAuth client secret.
 */
public class CloudSyncPanel extends VBox {

    private final AppStats appStats;
    private final Label statusLabel = new Label();
    private final Label accountLabel = new Label();
    private final Label lastSyncLabel = new Label();
    private final Button connectBtn = new Button("Connect Google Account");
    private final Button disconnectBtn = new Button("Disconnect");
    private final Button syncBtn = new Button("Sync Now");

    public CloudSyncPanel(AppStats appStats) {
        super(12);
        this.appStats = appStats;
        setStyle("-fx-border-color: #4285F4; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");

        Label header = new Label("Cloud Sync (Google Drive)");
        header.setStyle("-fx-text-fill: #8AB4F8; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label desc = new Label("Save and sync all your data to a \"" + GoogleDriveSyncManager.DATA_FOLDER_NAME
                + "\" folder in your Google Drive. Syncing is asynchronous — no USB or manual file transfer needed.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");

        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px;");

        accountLabel.setWrapText(true);
        accountLabel.setStyle("-fx-text-fill: #DCDCDC; -fx-font-size: 12px;");
        lastSyncLabel.setWrapText(true);
        lastSyncLabel.setStyle("-fx-text-fill: #858585; -fx-font-size: 12px;");

        connectBtn.setStyle("-fx-background-color: #1a3a66; -fx-text-fill: #8AB4F8; -fx-border-color: #4285F4; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 8 15;");
        disconnectBtn.setStyle("-fx-background-color: #4d1a1a; -fx-text-fill: #FF8A8A; -fx-border-color: #B04545; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 8 15;");
        syncBtn.setStyle("-fx-background-color: #1a4d33; -fx-text-fill: #4EC9B0; -fx-border-color: #4EC9B0; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 8 15;");
        HBox.setHgrow(connectBtn, Priority.ALWAYS); connectBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(disconnectBtn, Priority.ALWAYS); disconnectBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(syncBtn, Priority.ALWAYS); syncBtn.setMaxWidth(Double.MAX_VALUE);

        connectBtn.setOnAction(e -> {
            setBusy(true, "Opening Google sign-in in your browser…");
            GoogleDriveSyncManager.connect(
                    email -> Platform.runLater(() -> {
                        appStats.setGoogleDriveEmail(email);
                        StorageManager.saveStats(appStats);
                        setBusy(false, null);
                        refreshState();
                    }),
                    err -> Platform.runLater(() -> {
                        setBusy(false, null);
                        refreshState();
                        showError(err);
                    }));
        });

        disconnectBtn.setOnAction(e -> {
            setBusy(true, "Disconnecting…");
            GoogleDriveSyncManager.disconnect(() -> Platform.runLater(() -> {
                appStats.setGoogleDriveEmail(null);
                StorageManager.saveStats(appStats);
                setBusy(false, null);
                refreshState();
            }));
        });

        syncBtn.setOnAction(e -> {
            setBusy(true, "Syncing to Google Drive…");
            GoogleDriveSyncManager.syncNow(
                    () -> Platform.runLater(() -> { setBusy(false, null); refreshState(); }),
                    err -> Platform.runLater(() -> { setBusy(false, null); refreshState(); showError(err); }));
        });

        HBox buttons = new HBox(12, connectBtn, syncBtn, disconnectBtn);
        buttons.setAlignment(Pos.CENTER);

        Label setupHint = new Label("Not configured? Place your Google \"Desktop app\" OAuth client_secret.json as "
                + "\"google_client_secret.json\" in the data folder (Data Management → Open Data Folder), then click Connect. "
                + "See google_drive_setup.txt in the app resources for full steps.");
        setupHint.setWrapText(true);
        setupHint.setStyle("-fx-text-fill: #6A6A6A; -fx-font-size: 11px; -fx-font-style: italic;");

        getChildren().addAll(header, desc, statusLabel, accountLabel, lastSyncLabel, buttons, setupHint);
        refreshState();
    }

    private void setBusy(boolean busy, String message) {
        connectBtn.setDisable(busy);
        disconnectBtn.setDisable(busy);
        syncBtn.setDisable(busy);
        if (busy && message != null) statusLabel.setText("⏳ " + message);
    }

    private void refreshState() {
        boolean configured = GoogleDriveSyncManager.isConfigured();
        boolean connected = configured && GoogleDriveSyncManager.isConnected();

        if (!configured) {
            statusLabel.setText("⚠ Not configured — add your OAuth client secret to enable Google Drive sync.");
            statusLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px;");
            connectBtn.setDisable(false);
            connectBtn.setText("Connect Google Account");
            disconnectBtn.setDisable(true);
            syncBtn.setDisable(true);
        } else if (!connected) {
            statusLabel.setText("🔌 Configured but not connected. Click Connect to sign in.");
            statusLabel.setStyle("-fx-text-fill: #8AB4F8; -fx-font-size: 12px;");
            connectBtn.setDisable(false);
            connectBtn.setText("Connect Google Account");
            disconnectBtn.setDisable(true);
            syncBtn.setDisable(true);
        } else {
            statusLabel.setText("✅ Connected — data syncs to \"" + GoogleDriveSyncManager.DATA_FOLDER_NAME + "\".");
            statusLabel.setStyle("-fx-text-fill: #4EC9B0; -fx-font-size: 12px;");
            connectBtn.setDisable(true);
            disconnectBtn.setDisable(false);
            syncBtn.setDisable(false);
        }

        // Connected-account + last-sync lines (shown only once an account is connected).
        accountLabel.setManaged(connected);
        accountLabel.setVisible(connected);
        lastSyncLabel.setManaged(connected);
        lastSyncLabel.setVisible(connected);
        if (connected) {
            accountLabel.setText("👤 Account: " + safeEmail());
            lastSyncLabel.setText("🕓 Last synced: " + formatLastSync());
        }
    }

    private String safeEmail() {
        String e = appStats.getGoogleDriveEmail();
        return (e == null || e.isBlank()) ? "your Google account" : e;
    }

    private String formatLastSync() {
        long ms = GoogleDriveSyncManager.getLastSyncMillis();
        if (ms < 0) return "Never (no sync yet)";
        return java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ms), java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Google Drive Sync");
        alert.showAndWait();
    }
}
