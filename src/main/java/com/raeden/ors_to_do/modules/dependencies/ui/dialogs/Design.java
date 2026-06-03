package com.raeden.ors_to_do.modules.dependencies.ui.dialogs;

import com.raeden.ors_to_do.i18n.Lang;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Single home for the application's "long string" dialogs and design dialogs.
 *
 * <p>Rather than building, wording and styling {@link Alert}s inline in every UI class,
 * the cards/menus/services ask {@code Design} for a ready-made dialog. All copy comes from
 * {@link Lang} and all dark-theme styling comes from {@link TaskDialogs#styleDialog}, so the
 * look and the wording stay consistent in one place.</p>
 */
public final class Design {

    private Design() { }

    /**
     * Shows a styled YES/NO confirmation and returns the user's choice.
     *
     * @param headerKey title/header line
     * @param bodyKey   body message
     * @param bodyArgs  placeholder values for the body message
     * @return the chosen {@link ButtonType} (YES/NO), or empty if dismissed
     */
    public static Optional<ButtonType> confirmYesNo(Lang headerKey, Lang bodyKey, Object... bodyArgs) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, bodyKey.get(bodyArgs), ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(headerKey.get());
        TaskDialogs.styleDialog(confirm);
        return confirm.showAndWait();
    }

    /** Returns true only when the user explicitly answered YES to a styled confirmation. */
    public static boolean confirmedYes(Lang headerKey, Lang bodyKey, Object... bodyArgs) {
        return confirmYesNo(headerKey, bodyKey, bodyArgs).filter(r -> r == ButtonType.YES).isPresent();
    }

    /** Shows a styled, non-blocking warning dialog. */
    public static void warn(Lang headerKey, Lang bodyKey, Object... bodyArgs) {
        Alert alert = new Alert(Alert.AlertType.WARNING, bodyKey.get(bodyArgs));
        alert.setHeaderText(headerKey.get());
        TaskDialogs.styleDialog(alert);
        alert.show();
    }

    /** Shows a styled, non-blocking error dialog. */
    public static void error(Lang headerKey, Lang bodyKey, Object... bodyArgs) {
        Alert alert = new Alert(Alert.AlertType.ERROR, bodyKey.get(bodyArgs));
        alert.setHeaderText(headerKey.get());
        TaskDialogs.styleDialog(alert);
        alert.show();
    }
}
