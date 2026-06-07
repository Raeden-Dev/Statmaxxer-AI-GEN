package com.raeden.ors_to_do.modules.dependencies.services;

import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Robust "show me the main window now" helper used by both {@link SystemTrayManager} (tray
 * double-click / Open menu) and {@link SingleInstanceManager} (second-launch wake-up).
 *
 * <p>Windows enforces a "foreground lock" that prevents a background process from stealing focus
 * with a plain {@code Stage.toFront()}; the call succeeds silently but the window stays buried
 * behind whatever the user is currently on. The widely-used JavaFX workaround is to temporarily
 * flip {@code alwaysOnTop} — the OS treats that as a permitted Z-order change. We also poke
 * {@code setIconified(false)} twice and run the whole sequence on the JavaFX thread.</p>
 *
 * <p>This fixes the reported behaviour where launching the app a second time (via taskbar/icon)
 * while it was already alive in the tray did nothing, forcing the user to kill the tray process
 * first.</p>
 */
public final class WindowRestorer {

    private WindowRestorer() { }

    /** Forces {@code stage} to surface above other windows, marshalling onto the JavaFX thread. */
    public static void surface(Stage stage) {
        if (stage == null) return;
        Platform.runLater(() -> surfaceOnFxThread(stage));
    }

    private static void surfaceOnFxThread(Stage stage) {
        try {
            // Un-iconify if minimised to the taskbar.
            if (stage.isIconified()) stage.setIconified(false);

            // show() is idempotent — safe to call even if the stage was just hidden.
            stage.show();

            // Toggle iconified to nudge the WM into giving us a Z-order change.
            stage.setIconified(true);
            stage.setIconified(false);

            // The reliable foreground trick: briefly request always-on-top, then revert. Windows
            // permits this Z-order change even from a background process.
            boolean wasAlwaysOnTop = stage.isAlwaysOnTop();
            stage.setAlwaysOnTop(true);
            stage.toFront();
            stage.requestFocus();
            stage.setAlwaysOnTop(wasAlwaysOnTop);
        } catch (Exception ignore) {
            // Best-effort: never let a windowing hiccup crash the listener or tray click.
        }
    }
}
