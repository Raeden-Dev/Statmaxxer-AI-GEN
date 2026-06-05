package com.raeden.ors_to_do.modules.dependencies.ui.utils;

import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.services.ProgressionService;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.Design;
import javafx.application.Platform;
import java.io.File;
import java.net.URI;

public class TaskLinkUtil {
    public static void openActionPath(String path) {
        if (path == null || path.isEmpty()) return;
        new Thread(() -> {
            try {
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    java.awt.Desktop.getDesktop().browse(new URI(path));
                } else {
                    File file = new File(path);
                    if (file.exists()) java.awt.Desktop.getDesktop().open(file);
                    else Runtime.getRuntime().exec(path);
                }
            } catch (Exception e) {
                Platform.runLater(() -> Design.error(Lang.ERR_EXECUTION_HEADER, Lang.ERR_OPEN_PATH, path));
            }
        }).start();
    }

    /**
     * Whether a hooked dependency task counts as "satisfied" for unlocking a Perk/Challenge.
     * Thin delegate to {@link ProgressionService#isDependencyUnlocked(TaskItem)} (kept here so
     * existing UI callers don't need to change).
     */
    public static boolean isDependencyUnlocked(TaskItem depTask) {
        return ProgressionService.isDependencyUnlocked(depTask);
    }
}
