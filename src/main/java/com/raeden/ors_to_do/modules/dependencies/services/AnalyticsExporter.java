package com.raeden.ors_to_do.modules.dependencies.services;

import com.raeden.ors_to_do.dependencies.models.CalendarTask;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AnalyticsExporter {

    /**
     * Exports an HTML report for a Calendar Page section: per-task completion totals plus a
     * month-by-month breakdown table. Written to the Desktop and announced via an alert, mirroring
     * {@link #exportSectionAnalytics}.
     */
    public static void exportCalendarAnalytics(SectionConfig config) {
        try {
            File exportFile = new File(System.getProperty("user.home") + "/Desktop/"
                    + config.getName().replaceAll(" ", "_") + "_Calendar_Analytics.html");

            // taskId -> total days completed
            Map<String, Integer> totals = new HashMap<>();
            // "yyyy-MM" -> (taskId -> count)
            Map<String, Map<String, Integer>> byMonth = new TreeMap<>();
            int totalMarks = 0;

            for (Map.Entry<String, List<String>> day : config.getCalendarCompletions().entrySet()) {
                String month = day.getKey().length() >= 7 ? day.getKey().substring(0, 7) : day.getKey();
                for (String tid : day.getValue()) {
                    totals.merge(tid, 1, Integer::sum);
                    byMonth.computeIfAbsent(month, k -> new HashMap<>()).merge(tid, 1, Integer::sum);
                    totalMarks++;
                }
            }

            StringBuilder cards = new StringBuilder();
            StringBuilder labels = new StringBuilder();
            StringBuilder data = new StringBuilder();
            StringBuilder colors = new StringBuilder();
            for (CalendarTask t : config.getCalendarTasks()) {
                int count = totals.getOrDefault(t.getId(), 0);
                cards.append("<div class=\"stat-box\" style=\"border-color:").append(t.getColorHex())
                     .append("\"><h3>").append(escape(t.getName())).append("</h3><p style=\"color:")
                     .append(t.getColorHex()).append("\">").append(count).append("</p><span>days</span></div>");
                labels.append("'").append(escape(t.getName())).append("',");
                data.append(count).append(",");
                colors.append("'").append(t.getColorHex()).append("',");
            }

            StringBuilder monthRows = new StringBuilder();
            for (Map.Entry<String, Map<String, Integer>> m : byMonth.entrySet()) {
                StringBuilder cells = new StringBuilder();
                for (CalendarTask t : config.getCalendarTasks()) {
                    cells.append("<td>").append(m.getValue().getOrDefault(t.getId(), 0)).append("</td>");
                }
                monthRows.append("<tr><td><b>").append(m.getKey()).append("</b></td>").append(cells).append("</tr>");
            }
            StringBuilder monthHeader = new StringBuilder("<th>Month</th>");
            for (CalendarTask t : config.getCalendarTasks()) {
                monthHeader.append("<th style=\"color:").append(t.getColorHex()).append("\">").append(escape(t.getName())).append("</th>");
            }

            // --- Journal & favorites section ---
            StringBuilder journalSection = new StringBuilder();
            java.util.List<String> favs = new java.util.ArrayList<>(config.getCalendarFavoriteDays());
            java.util.Collections.sort(favs);
            if (!favs.isEmpty()) {
                journalSection.append("<p class=\"favs\"><b style=\"color:#FFD700\">★ Favorite days:</b> ")
                        .append(String.join(", ", favs)).append("</p>");
            }
            Map<String, java.util.List<com.raeden.ors_to_do.dependencies.models.CalendarEntry>> entries = config.getCalendarEntries();
            if (!entries.isEmpty()) {
                java.util.List<String> keys = new java.util.ArrayList<>(entries.keySet());
                java.util.Collections.sort(keys);
                journalSection.append("<h2 style=\"color:#C586C0;\">Journal &amp; Events</h2>");
                for (String k : keys) {
                    java.util.List<com.raeden.ors_to_do.dependencies.models.CalendarEntry> dayEntries = entries.get(k);
                    if (dayEntries == null || dayEntries.isEmpty()) continue;
                    journalSection.append("<div class=\"journal\"><div class=\"jdate\">").append(k);
                    if (config.isFavoriteDay(k)) journalSection.append(" ★");
                    journalSection.append("</div>");
                    for (var en : dayEntries) {
                        String tag = en.isEvent() ? "<span style=\"color:#C586C0;font-size:11px;\">[event] </span>" : "";
                        journalSection.append("<div class=\"jtext\">").append(tag).append(htmlText(en.getText())).append("</div>");
                    }
                    journalSection.append("</div>");
                }
            }

            String html = """
            <!DOCTYPE html>
            <html lang="en"><head><meta charset="UTF-8">
            <title>%s Calendar Analytics</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <style>
                body { background:#1E1E1E; color:#E0E0E0; font-family:'Segoe UI',sans-serif; padding:40px; }
                .header { text-align:center; margin-bottom:40px; border-bottom:2px solid #3E3E42; padding-bottom:20px; }
                h1 { color:#C586C0; }
                .stats-container { display:flex; flex-wrap:wrap; gap:16px; justify-content:center; margin-bottom:40px; }
                .stat-box { background:#2D2D30; padding:18px 24px; border-radius:10px; text-align:center; border:1px solid #3E3E42; min-width:120px; }
                .stat-box p { font-size:30px; font-weight:bold; margin:6px 0 0; }
                .stat-box span { color:#888; font-size:12px; }
                .chart-container { background:#2D2D30; padding:30px; border-radius:10px; border:1px solid #3E3E42; margin-bottom:40px; }
                table { width:100%%; border-collapse:collapse; background:#2D2D30; border-radius:10px; overflow:hidden; }
                th,td { padding:10px 14px; text-align:center; border-bottom:1px solid #3E3E42; }
                th { background:#252526; }
                .favs { background:#2D2D30; padding:12px 16px; border-radius:8px; border:1px solid #3E3E42; margin:24px 0; }
                .journal { background:#2D2D30; padding:14px 18px; border-radius:8px; border:1px solid #3E3E42; margin:12px 0; }
                .jdate { color:#569CD6; font-weight:bold; margin-bottom:6px; }
                .jtext { color:#DDDDDD; white-space:pre-wrap; }
            </style></head>
            <body>
                <div class="header"><h1>%s — Calendar Tracker</h1>
                <p style="color:#AAAAAA;">Generated on %s · %d total marks across %d tracked days</p></div>
                <div class="stats-container">%s</div>
                <div class="chart-container"><canvas id="c" height="100"></canvas></div>
                <h2 style="color:#569CD6;">Monthly Breakdown</h2>
                <table><thead><tr>%s</tr></thead><tbody>%s</tbody></table>
                %s
                <script>
                    Chart.defaults.color = '#AAAAAA';
                    new Chart(document.getElementById('c').getContext('2d'), {
                        type:'bar',
                        data:{ labels:[%s], datasets:[{ label:'Days Completed', data:[%s], backgroundColor:[%s], borderRadius:5 }] },
                        options:{ plugins:{ legend:{ display:false } } }
                    });
                </script>
            </body></html>
            """.formatted(
                    config.getName(), config.getName(),
                    LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    totalMarks, config.getCalendarCompletions().size(),
                    cards.toString(), monthHeader.toString(), monthRows.toString(),
                    journalSection.toString(),
                    labels.toString(), data.toString(), colors.toString()
            );

            try (FileWriter writer = new FileWriter(exportFile)) {
                writer.write(html);
            }

            try { java.awt.Desktop.getDesktop().browse(exportFile.toURI()); } catch (Exception ignore) { }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Calendar analytics exported to Desktop:\n" + exportFile.getName());
            alert.setHeaderText("Export Successful");
            alert.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'");
    }

    /** HTML-escaping for body text (keeps quotes literal; preserves line breaks via the CSS). */
    private static String htmlText(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static void exportSectionAnalytics(SectionConfig config, List<TaskItem> globalDatabase) {
        try {
            File exportFile = new File(System.getProperty("user.home") + "/Desktop/" + config.getName().replaceAll(" ", "_") + "_Analytics.html");
            FileWriter writer = new FileWriter(exportFile);

            int totalTasks = 0;
            int completedTasks = 0;
            int totalTimeSeconds = 0;

            Map<String, Integer> categoryMap = new HashMap<>();

            for (TaskItem task : globalDatabase) {
                if (task.getSectionId() != null && task.getSectionId().equals(config.getId())) {
                    totalTasks++;
                    if (task.isFinished()) completedTasks++;
                    totalTimeSeconds += task.getTimeSpentSeconds();

                    String key = "Uncategorized";
                    if (config.isShowTaskType() && task.getTaskType() != null && !task.getTaskType().isEmpty()) key = task.getTaskType();
                    else if (config.isShowPrefix() && task.getPrefix() != null && !task.getPrefix().isEmpty()) key = task.getPrefix();
                    else if (config.isShowPriority() && task.getPriority() != null) key = task.getPriority().getName();

                    // If tracking time, chart is Time-Based. If not, chart is Volume-Based.
                    int valueToAdd = config.isTrackTime() ? task.getTimeSpentSeconds() / 60 : 1;
                    categoryMap.put(key, categoryMap.getOrDefault(key, 0) + valueToAdd);
                }
            }

            StringBuilder labels = new StringBuilder();
            StringBuilder data = new StringBuilder();
            for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
                labels.append("'").append(entry.getKey()).append("',");
                data.append(entry.getValue()).append(",");
            }

            double completionRate = totalTasks == 0 ? 0 : ((double) completedTasks / totalTasks) * 100;
            String chartLabel = config.isTrackTime() ? "Time Spent (Minutes)" : "Task Volume";

            String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s Analytics</title>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <style>
                    body { background-color: #1E1E1E; color: #E0E0E0; font-family: 'Segoe UI', sans-serif; padding: 40px; }
                    .header { text-align: center; margin-bottom: 40px; border-bottom: 2px solid #3E3E42; padding-bottom: 20px; }
                    h1 { color: #569CD6; }
                    .stats-container { display: flex; justify-content: space-around; margin-bottom: 40px; }
                    .stat-box { background-color: #2D2D30; padding: 20px; border-radius: 10px; width: 30%%; text-align: center; border: 1px solid #3E3E42; }
                    .stat-box p { font-size: 32px; font-weight: bold; color: #4EC9B0; margin:0;}
                    .chart-container { background-color: #2D2D30; padding: 30px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.3); border: 1px solid #3E3E42;}
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>%s Dashboard</h1>
                    <p style="color: #AAAAAA;">Generated on %s</p>
                </div>
                
                <div class="stats-container">
                    <div class="stat-box"><h3>Completion Rate</h3><p>%s%%</p></div>
                    <div class="stat-box"><h3>Total Tasks</h3><p>%d</p></div>
                    <div class="stat-box"><h3>Total Time Tracked</h3><p>%dh %dm</p></div>
                </div>

                <div class="chart-container">
                    <canvas id="mainChart" height="100"></canvas>
                </div>

                <script>
                    Chart.defaults.color = '#AAAAAA';
                    new Chart(document.getElementById('mainChart').getContext('2d'), {
                        type: 'bar',
                        data: {
                            labels: [%s],
                            datasets: [{
                                label: '%s',
                                data: [%s],
                                backgroundColor: ['#4EC9B0', '#569CD6', '#E06666', '#FF8C00', '#C586C0'],
                                borderRadius: 5
                            }]
                        }
                    });
                </script>
            </body>
            </html>
            """.formatted(
                    config.getName(), config.getName(), LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    String.format("%.1f", completionRate), totalTasks,
                    (totalTimeSeconds / 3600), ((totalTimeSeconds % 3600) / 60),
                    labels.toString(), chartLabel, data.toString()
            );

            writer.write(htmlContent);
            writer.flush();
            writer.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Analytics Exported to Desktop:\n" + exportFile.getName());
            alert.setHeaderText("Export Successful");
            alert.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}