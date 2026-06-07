package com.raeden.ors_to_do.modules.dependencies.services;

import javafx.application.Platform;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;

public class SystemTrayManager {
    private static TrayIcon trayIcon;
    private static Stage mainStage;

    public static void setupSystemTray(Stage stage, Runnable onExit) {
        mainStage = stage;
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported on this OS.");
            return;
        }

        try {
            // --- FIXED: Load the custom icon.png from resources as an AWT Image ---
            Image awtImage = null;
            InputStream iconStream = SystemTrayManager.class.getResourceAsStream("/icon.png");

            if (iconStream != null) {
                awtImage = ImageIO.read(iconStream);
            } else {
                // Fallback to a blank image if icon.png is missing to prevent crashes
                awtImage = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                System.out.println("Warning: /icon.png not found for System Tray.");
            }

            SystemTray tray = SystemTray.getSystemTray();

            // Set the image and ensure it scales correctly to the taskbar size
            trayIcon = new TrayIcon(awtImage, "Task Tracker");
            trayIcon.setImageAutoSize(true);

            // Double-click to restore the app — use the restorer for the same reason as below.
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                        WindowRestorer.surface(mainStage);
                    }
                }
            });

            // Create Right-Click popup menu
            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Open Task Tracker");
            // The bare show()/toFront() sequence is unreliable on Windows because of the
            // foreground-window lock; WindowRestorer applies the alwaysOnTop-flip workaround.
            openItem.addActionListener(e -> WindowRestorer.surface(mainStage));

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                tray.remove(trayIcon);
                Platform.runLater(onExit);
            });

            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);
            tray.add(trayIcon);

        } catch (Exception e) {
            System.out.println("Error initializing System Tray: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void pushNotification(String title, String message) {
        if (trayIcon != null) {
            // Use standard INFO message type for the notification
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }
}