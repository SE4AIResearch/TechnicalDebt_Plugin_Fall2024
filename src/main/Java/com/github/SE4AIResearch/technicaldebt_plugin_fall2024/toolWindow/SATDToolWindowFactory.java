package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.toolWindow;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.PathManager;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

public class SATDToolWindowFactory implements ToolWindowFactory, DumbAware {
        @Override
        public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
            JPanel toolWindowPanel = new JBPanel<>();
            toolWindowPanel.setLayout(new BorderLayout());
            JTabbedPane tabbedPane = new JTabbedPane();

            JBLabel label = new JBLabel("Click the button to connect to SATD database...");
            toolWindowPanel.add(label, BorderLayout.NORTH);

            // Create a button to start the process
            JButton button = new JButton("Fetch SATD Data");
            toolWindowPanel.add(button, BorderLayout.SOUTH);

            // Create a table model with column names
            DefaultTableModel tableModel = new DefaultTableModel();
            tableModel.addColumn("File ID");
            tableModel.addColumn("Comment");
            tableModel.addColumn("Path");
            tableModel.addColumn("Start Line");
            tableModel.addColumn("End Line");
            tableModel.addColumn("Containing Class");
            tableModel.addColumn("Containing Method");
            tableModel.addColumn("SATD Type");

            // Create the JTable with the model
            JTable table = new JTable(tableModel);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            // Set preferred widths for the columns
            table.getColumnModel().getColumn(0); // Comment Number
            table.getColumnModel().getColumn(1).setPreferredWidth(500); // Comment
            table.getColumnModel().getColumn(2); //Path
            table.getColumnModel().getColumn(3); //Start Line
            table.getColumnModel().getColumn(4); //End Line
            table.getColumnModel().getColumn(5); //Containing Class
            table.getColumnModel().getColumn(6); //Containing Method

            //Makes it so table cannot be edited
            table.setEnabled(false);

            // Set custom renderer for the columns to allow text wrapping
            table.getColumnModel().getColumn(1).setCellRenderer(new TextAreaRenderer());

            // TEST: Create the scroll pane and add to the tabbed pane
            JBScrollPane scrollPane = new JBScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tabbedPane.addTab("SATD In File", scrollPane);

            //Create 2nd Table
            DefaultTableModel tableModel2 = new DefaultTableModel();
            tableModel2.addColumn("SATD ID");
            tableModel2.addColumn("First File");
            tableModel2.addColumn("Second File");
            tableModel2.addColumn("Resolution");
            JTable table2 = new JTable(tableModel2);
            table2.setEnabled((false));
            JBScrollPane scrollPane2 = new JBScrollPane(table2, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            tabbedPane.addTab("SATD", scrollPane2);

            toolWindowPanel.add(tabbedPane, BorderLayout.CENTER);


            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) { // Double-click to navigate

                        int row = table.rowAtPoint(e.getPoint());
                        //int column = table.columnAtPoint(e.getPoint());
                        String path = (String) table.getValueAt(row, 2);
                        String startLineStr = (String) table.getValueAt(row, 3);
                        int line = Integer.parseInt(startLineStr);
                        navigateToCode(project, line, path);
                    }
                }
            });

            // Button action to fetch data
            button.addActionListener(e -> {
                new DatabaseLookupSwingWorker(tableModel, label, table, tableModel2, table2).execute();
            });

            // Add panel to IntelliJ's content factory
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(toolWindowPanel, "", false);
            toolWindow.getContentManager().addContent(content);
        }

    private void navigateToCode(Project project, int lineNumber, String path) {
        String homePath = project.getBasePath();
        path = homePath + "/" + path;
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file == null) {
            System.out.println("File not found: " + path);
            return;
        }
        FileEditorManager.getInstance(project).openFile(file, true);
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            System.out.println("No editor is currently open");
            return;
        }
        // Move the caret to the desired line
        CaretModel caretModel = editor.getCaretModel();
        if (lineNumber > 0 && lineNumber <= editor.getDocument().getLineCount()) {
            caretModel.moveToLogicalPosition(new LogicalPosition(lineNumber - 1, 0));
            //Scroll to the desired line to make it visible
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }

    private static class DatabaseLookupSwingWorker extends SwingWorker<Void, Void> {
        private final DefaultTableModel tableModel;
        private final DefaultTableModel tableModel2;
        private final JBLabel label;
        private final JTable table;

        public DatabaseLookupSwingWorker(DefaultTableModel tableModel, JBLabel label, JTable table,  DefaultTableModel tableModel2, JTable table2) {
            this.tableModel = tableModel;
            this.label = label;
            this.table = table;
            this.tableModel2 = tableModel2;
        }

        @Override
        protected Void doInBackground() {
            tableModel.setRowCount(0);
            tableModel2.setRowCount(0);

            String sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql";
            String databasePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/satd.db";
            String libPath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/";

            try (InputStream inputStream = new FileInputStream(sqlFilePath)) {
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();

                // Load the SQLite JDBC driver
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException e) {
                    label.setText("SQLite JDBC Driver not found. Check your classpath.");
                    return null;
                }

                String url = "jdbc:sqlite:" + databasePath;

                try (Connection conn = DriverManager.getConnection(url);
                     Statement stmt = conn.createStatement()) {
                    label.setText("Connection successful!");

                    // Execute SQL script
                    String sql = new String(inputStream.readAllBytes());
                    for (String query : sql.split(";")) {
                        if (!query.trim().isEmpty()) {
                            stmt.execute(query);
                        }
                    }

                    if (indicator != null) {
                        indicator.setText("Database initialization complete.");
                        indicator.setFraction(0.33);
                    }

                    // Run external SATD analysis
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            "java",
                            "--add-opens",
                            "java.base/java.lang=ALL-UNNAMED",
                            "-jar",
                            libPath + "target/satd-analyzer-jar-with-all-dependencies.jar",
                            "-r",
                            libPath + "test_repo.csv",
                            "-d",
                            libPath + "satd.db"
                    );
                    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                    Process process = processBuilder.start();
                    process.waitFor();

                    if (indicator != null) {
                        indicator.setText("Database up to date.");
                        indicator.setFraction(0.66);
                    }

                    // Fetch SATD data
                    ResultSet rs = stmt.executeQuery("SELECT * FROM SATDInFile");
                    int commentNumber = 1;
                    while (rs.next()) {
                        Object[] rowData = {
                                commentNumber++, rs.getString("f_comment"), rs.getString("f_path"),
                                rs.getInt("start_line"), rs.getInt("end_line"), rs.getString("containing_class"),
                                rs.getString("containing_method"), "" // Placeholder for SATD Type
                        };
                        SwingUtilities.invokeLater(() -> tableModel.addRow(rowData));
                    }

                    // Fetch SATD Type
                    rs = stmt.executeQuery("SELECT * FROM SATD");
                    int rowIndex = 0;
                    while (rs.next()) {
                        String resolution = rs.getString("resolution");
                        int finalRowIndex = rowIndex++;
                        SwingUtilities.invokeLater(() -> tableModel.setValueAt(resolution, finalRowIndex, 7));
                        tableModel2.addRow(new Object[]{
                                rs.getInt("satd_id"), rs.getInt("first_file"), rs.getInt("second_file"), resolution
                        });
                    }

                    if (indicator != null) {
                        indicator.setText("Data fetching complete.");
                        indicator.setFraction(1.0);
                    }

                    rs.close();
                } catch (Exception e) {
                    label.setText("Connection failed: " + e.getMessage());
                }
            } catch (Exception e) {
                label.setText("Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void done() {
            SwingUtilities.invokeLater(() -> {
                label.setText("Data fetching complete.");
                adjustColumnWidths(table);
            });
        }
    }

    // Custom renderer to wrap text in the "Comment" column
    static class TextAreaRenderer extends JTextArea implements TableCellRenderer {
        public TextAreaRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true); // So it paints the background correctly
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);

            // Adjust row height to fit the wrapped text
            if (table.getRowHeight(row) != getPreferredSize().height) {
                table.setRowHeight(row, getPreferredSize().height);
            }

            // Set background colors based on selection
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }
    // Method to adjust column widths dynamically
    public static void adjustColumnWidths(JTable table) {
        for (int col = 0; col < table.getColumnCount(); col++) {
            if((col != 1) && (col != 8) && (col != 9)) {
                TableColumn column = table.getColumnModel().getColumn(col);
                int minWidth = getTextWidth(table, column.getHeaderValue().toString(), table.getFont());
                int maxWidth = minWidth;

                // Iterate through rows to find the maximum width
                for (int row = 0; row < table.getRowCount(); row++) {
                    Object value = table.getValueAt(row, col);
                    if (value != null) {
                        int cellWidth = getTextWidth(table, value.toString(), table.getFont());
                        if (cellWidth > maxWidth) {
                            maxWidth = cellWidth;
                        }
                    }
                }
                // Set the column width (minimum width as the header's width, preferred as the max width)
                column.setMinWidth(minWidth + 10); // Adding some padding
                column.setPreferredWidth(maxWidth + 20); // Adding padding for readability
            }
        }
    }

    // Helper method to calculate the pixel width of a given text with a specific font
    private static int getTextWidth(JTable table, String text, Font font) {
        FontMetrics metrics = table.getFontMetrics(font);
        return metrics.stringWidth(text);
    }
}
