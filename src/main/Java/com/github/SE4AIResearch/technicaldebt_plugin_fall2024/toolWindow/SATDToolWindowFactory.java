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

        JBLabel label = new JBLabel("Click the button to connect to SATD database...");
        toolWindowPanel.add(label, BorderLayout.NORTH);

        // Create a button to start the process
        JButton button = new JButton("Fetch SATD Data");
        toolWindowPanel.add(button, BorderLayout.SOUTH);

        // Create a table model with column names
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Comment Number");
        tableModel.addColumn("Comment");
        tableModel.addColumn("Path");
        tableModel.addColumn("Start Line");
        tableModel.addColumn("End Line");
        tableModel.addColumn("Containing Class");
        tableModel.addColumn("Containing Method");
        tableModel.addColumn("SATD Type");

        // Create the JTable with the model
        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Disable auto-resizing for better control

        // Set preferred widths for the columns
        table.getColumnModel().getColumn(0); // Comment Number
        table.getColumnModel().getColumn(1).setPreferredWidth(500); // Comment
        table.getColumnModel().getColumn(2); //Path
        table.getColumnModel().getColumn(3); //Start Line
        table.getColumnModel().getColumn(4); //End Line
        table.getColumnModel().getColumn(5); //Containing Class
        table.getColumnModel().getColumn(6); //Containing Method
        table.getColumnModel().getColumn(7); //SATD Type

        table.setEnabled(false);

        // Set custom renderer for the columns to allow text wrapping
        table.getColumnModel().getColumn(1).setCellRenderer(new TextAreaRenderer());

        // Create the scroll pane and add the table
        JBScrollPane scrollPane = new JBScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toolWindowPanel.add(scrollPane, BorderLayout.CENTER);

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


        // Set button action
        button.addActionListener(e -> {
            // Use ProgressManager to show progress while connecting to the database and fetching data
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    () -> initializeAndConnectDatabase(tableModel, label, table),
                    "Fetching SATD Data",
                    false,
                    project
            );
        });

        // Adds our panel to IntelliJ's content factory
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

    public void initializeAndConnectDatabase(DefaultTableModel tableModel, JBLabel label, JTable table) {
        //Gets the sql filepath from sql folder
        tableModel.setRowCount(0);
        String sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql";
        String databasePath =  PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/satd.db";
        String libPath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/";
        System.out.println(libPath);

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(sqlFilePath);
        } catch (FileNotFoundException e){
            label.setText("Error: " + e.getMessage());
        }
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        // Load the MySQL JDBC Driver
        try {
            Class.forName("org.sqlite.JDBC"); // This will throw an exception if the driver is not found
            System.out.println("Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            label.setText("SQLite JDBC Driver not found. Check your classpath.");
        }

        String url = "jdbc:sqlite:" + databasePath;

        try (Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement()) {

            label.setText("Connection successful!");

            // Read and execute the SQL file
            String sql = new String(inputStream.readAllBytes());
            String[] queries = sql.split(";");

            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query);
                }
            }

            //TODO: dynamically get p_name and p_url
            //stmt.execute("INSERT INTO Projects (p_name, p_url) VALUES ('apache/log4j', 'https://github.com/apache/log4j')");

            // Update progress
            if (indicator != null) {
                indicator.setText("Database initialization complete.");
                indicator.setFraction(0.33);
            }

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "java",
                        "--add-opens",
                        "java.base/java.lang=ALL-UNNAMED",
                        "-jar",
                        (libPath + "target/satd-analyzer-jar-with-all-dependencies.jar"),
                        "-r",
                        (libPath + "test_repo.csv"),
                        "-d",
                        (libPath + "satd.db")
                );

                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

                Process process = processBuilder.start();

                int exitCode = process.waitFor();
                System.out.println("Exit code:" + exitCode);
            } catch(IOException | InterruptedException e){
                e.printStackTrace();
            }

            // Update progress
            if (indicator != null) {
                indicator.setText("Database up to Date.");
                indicator.setFraction(0.66);
            }

            String fetchQuery = "SELECT * FROM SATDnoDups";
            ResultSet rs = stmt.executeQuery(fetchQuery);

            // Displaying query results
            int commentNumber = 1; // Initialize comment number
            while (rs.next()) {
                String f_comment = rs.getString("f_comment"); // Replace with actual column name
                String f_path = rs.getString("f_path");
                int start_line = rs.getInt("start_line");
                int end_line = rs.getInt("end_line");
                String  containing_class = rs.getString("containing_class");
                String containing_method = rs.getString("containing_method");
                tableModel.addRow(new Object[]{commentNumber++, f_comment, f_path, start_line, end_line, containing_class, containing_method});
            }

            fetchQuery = "SELECT * FROM SATD";
            rs = stmt.executeQuery(fetchQuery);

            int i = 0;
            while (rs.next()) {
                String resolution = rs.getString("resolution");
                if(i > commentNumber){
                    tableModel.addRow(new Object[]{i, null, null, null, null, null, null, resolution});
                }
                else {
                    tableModel.setValueAt(resolution, i, 7); // Add new value to "SATD Type" column
                }

                i += 1;
            }

            adjustColumnWidths(table);

            // Update progress
            if (indicator != null) {
                indicator.setText("Data fetching complete.");
                indicator.setFraction(1.0);
            };

            // Close resources
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            label.setText("Connection failed: " + e.getMessage());
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
