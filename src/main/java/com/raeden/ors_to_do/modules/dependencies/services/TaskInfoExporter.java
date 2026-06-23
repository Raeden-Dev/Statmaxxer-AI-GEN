package com.raeden.ors_to_do.modules.dependencies.services;

import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.SubTask;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.models.TaskLink;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports a single section's tasks (with sub-tasks, sub-task links, and descriptions) to a plain
 * text file chosen via the OS save dialog. The default filename is
 * {@code [section_name]_task_information.txt}.
 */
public final class TaskInfoExporter {

    private TaskInfoExporter() { }

    /**
     * Prompts for a save location and writes an organized text report of every (non-archived) task
     * in {@code section}. Returns silently if the user cancels the file picker.
     */
    public static void exportSection(SectionConfig section, List<TaskItem> globalDatabase, Window owner) {
        if (section == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Task Information");
        chooser.setInitialFileName(safeFileName(section.getName()) + "_task_information.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text File", "*.txt"));

        File target = chooser.showSaveDialog(owner);
        if (target == null) return; // user cancelled

        String report = buildReport(section, globalDatabase);
        try {
            Files.write(target.toPath(), report.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not write the export file:\n" + e.getMessage());
            alert.setHeaderText("Export Failed");
            alert.showAndWait();
        }
    }

    /** Builds the full text report for a section. Package-private so it can be unit-tested. */
    static String buildReport(SectionConfig section, List<TaskItem> globalDatabase) {
        StringBuilder sb = new StringBuilder();
        String title = "Task Information — " + nullToBlank(section.getName());
        sb.append(title).append('\n');
        sb.append("=".repeat(Math.max(title.length(), 1))).append('\n');
        sb.append("Exported: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append('\n');

        int count = 0;
        StringBuilder body = new StringBuilder();
        if (globalDatabase != null) {
            int index = 1;
            for (TaskItem task : globalDatabase) {
                if (!section.getId().equals(task.getSectionId())) continue;
                if (task.isArchived()) continue;
                count++;
                appendTask(body, index++, task);
            }
        }

        sb.append("Total tasks: ").append(count).append('\n');
        sb.append('\n');
        if (count == 0) {
            sb.append("(No tasks in this section.)\n");
        } else {
            sb.append(body);
        }
        return sb.toString();
    }

    private static void appendTask(StringBuilder sb, int index, TaskItem task) {
        String status = task.isFinished() ? "[x]" : "[ ]";
        sb.append('\n').append(index).append(". ").append(status).append(' ')
                .append(nullToBlank(task.getTextContent())).append('\n');

        if (task.getCategoryName() != null && !task.getCategoryName().trim().isEmpty()) {
            sb.append("    Category: ").append(task.getCategoryName().trim()).append('\n');
        }
        if (task.getPrefix() != null && !task.getPrefix().trim().isEmpty()) {
            sb.append("    Prefix: ").append(task.getPrefix().trim()).append('\n');
        }
        if (task.getTaskType() != null && !task.getTaskType().trim().isEmpty()) {
            sb.append("    Type: ").append(task.getTaskType().trim()).append('\n');
        }
        if (task.isLinkCard() && task.getLinkActionPath() != null && !task.getLinkActionPath().trim().isEmpty()) {
            sb.append("    Link: ").append(task.getLinkActionPath().trim()).append('\n');
        }
        if (task.isDescriptionCard() && !task.getDescriptionContent().trim().isEmpty()) {
            sb.append("    Description:\n");
            for (String line : task.getDescriptionContent().split("\\R")) {
                sb.append("      ").append(line).append('\n');
            }
        }

        List<TaskLink> links = task.getTaskLinks();
        if (links != null && !links.isEmpty()) {
            sb.append("    Links:\n");
            for (TaskLink link : links) {
                String name = link.getName() != null && !link.getName().trim().isEmpty() ? link.getName().trim() : "Link";
                sb.append("      - ").append(name).append(": ").append(nullToBlank(link.getUrl())).append('\n');
            }
        }

        List<SubTask> subTasks = task.getSubTasks();
        if (subTasks != null && !subTasks.isEmpty()) {
            sb.append("    Sub-tasks:\n");
            for (SubTask sub : subTasks) {
                String subStatus = sub.isFinished() ? "[x]" : "[ ]";
                sb.append("      ").append(subStatus).append(' ').append(nullToBlank(sub.getTextContent())).append('\n');
            }
        }
    }

    private static String safeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "section";
        return name.trim().replaceAll("[^a-zA-Z0-9-_ ]", "").replaceAll("\\s+", "_");
    }

    private static String nullToBlank(String s) {
        return s == null ? "" : s;
    }
}
