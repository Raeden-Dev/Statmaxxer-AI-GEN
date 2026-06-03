package com.raeden.ors_to_do.modules.dependencies.ui.utils;

import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.i18n.Lang;
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
     * Decides whether a hooked dependency task counts as "satisfied" for the purpose of
     * unlocking a Perk or Challenge that depends on it.
     *
     * <p>Counter cards (repeatable tasks that tick a counter toward a target) are special:
     * they have no stat requirements and no dependencies of their own, so the old
     * "no gates -> automatically satisfied" rule wrongly treated them as complete the moment
     * they were created. A counter card is only satisfied once its counter has reached its
     * configured maximum (or it has otherwise been flagged finished).</p>
     *
     * @param depTask the dependency task being evaluated (may be null)
     * @return true if the dependency should be considered unlocked/satisfied
     */
    public static boolean isDependencyUnlocked(TaskItem depTask) {
        if (depTask == null) return false;

        // Counter cards must actually reach their target before they count as done.
        if (depTask.isCounterMode()) {
            return depTask.isFinished()
                    || (depTask.getMaxCount() > 0 && depTask.getCurrentCount() >= depTask.getMaxCount());
        }

        boolean hasNoGates = depTask.getStatRequirements().isEmpty()
                && (depTask.getDependsOnTaskIds() == null || depTask.getDependsOnTaskIds().isEmpty());

        return depTask.getPerkLevel() > 0 || depTask.isFinished() || hasNoGates;
    }
}
