package com.raeden.ors_to_do.modules.dependencies.ui.utils;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Helpers for making non-editable, displayed text copyable. JavaFX {@code Label}/{@code Hyperlink}
 * text can't be selected, so this either attaches a right-click "Copy" menu, copies a value directly,
 * or builds a read-only but <b>selectable</b> text control (so the user can highlight and Ctrl+C).
 */
public final class TextCopyUtil {

    private TextCopyUtil() { }

    // Styles a read-only TextArea so it reads like a plain label: transparent, no border, no focus
    // ring, and no visible scrollbars.
    private static final String SELECTABLE_CSS =
            ".selectable-text, .selectable-text .content { -fx-background-color: transparent; -fx-background-insets: 0; }"
                    + ".selectable-text { -fx-padding: 0; -fx-background-radius: 0; -fx-border-width: 0; -fx-faint-focus-color: transparent; -fx-focus-color: transparent; }"
                    + ".selectable-text .scroll-bar:vertical, .selectable-text .scroll-bar:horizontal { -fx-pref-width: 0; -fx-pref-height: 0; -fx-opacity: 0; }";

    /**
     * Builds a read-only, selectable text node that looks like a wrapping label but supports mouse
     * selection, Ctrl+A / Ctrl+C and the native Copy context menu. Auto-sizes its height to the text
     * (roughly) so it doesn't scroll for typical content.
     */
    public static TextArea selectableArea(String text, String textFillHex, int fontSize, boolean italic) {
        TextArea ta = new TextArea(text == null ? "" : text);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.getStyleClass().add("selectable-text");
        ta.setStyle("-fx-text-fill: " + textFillHex + "; -fx-font-size: " + fontSize + "px;"
                + (italic ? " -fx-font-style: italic;" : ""));
        ta.getStylesheets().add("data:text/css;base64,"
                + Base64.getEncoder().encodeToString(SELECTABLE_CSS.getBytes(StandardCharsets.UTF_8)));

        // Approximate the number of (wrapped) rows so the box fits its content without scrolling.
        int rows = 0;
        for (String para : ta.getText().split("\n", -1)) rows += Math.max(1, (int) Math.ceil(para.length() / 45.0));
        ta.setPrefRowCount(Math.max(1, Math.min(20, rows)));
        return ta;
    }

    /** Adds a right-click "Copy" context menu to a label/hyperlink that copies its visible text. */
    public static void makeCopyable(Labeled node) {
        if (node == null) return;
        ContextMenu menu = new ContextMenu();
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(e -> copyToClipboard(node.getText()));
        menu.getItems().add(copy);
        node.setContextMenu(menu);
    }

    /** Puts {@code text} on the system clipboard (null becomes empty). */
    public static void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
