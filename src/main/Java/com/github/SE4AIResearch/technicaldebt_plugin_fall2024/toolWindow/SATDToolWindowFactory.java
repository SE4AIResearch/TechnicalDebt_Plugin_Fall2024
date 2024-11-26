package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.toolWindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.PathManager;
import java.io.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
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
        JPanel toolWindowPanel = new JBPanel();
        toolWindowPanel.setLayout(new BorderLayout());

        JBLabel label = new JBLabel("Connecting to SATD database...");
        toolWindowPanel.add(label, BorderLayout.NORTH);

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


        new Task.Backgroundable(project, "Initializing and Connecting Database") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try{
                initializeAndConnectDatabase(tableModel, label, project);
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> label.setText("Unexpected error: " + e.getMessage()));
                }
            }
        }.queue();

        adjustColumnWidths(table);

        // Adds our panel to IntelliJ's content factory
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toolWindowPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public void initializeAndConnectDatabase(DefaultTableModel tableModel, JBLabel label, Project project) {
        //Gets the sql filepath from sql folder

        String sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql";
        String databasePath =  PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/Database/satd.db";

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(sqlFilePath);
        } catch (FileNotFoundException e){
            SwingUtilities.invokeLater(() -> label.setText("Connection failed: " + e.getMessage()));
        }

        // Load the MySQL JDBC Driver
        try {
            Class.forName("org.sqlite.JDBC"); // This will throw an exception if the driver is not found
            System.out.println("Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            SwingUtilities.invokeLater(() -> label.setText("SQLite JDBC Driver not found. Check your classpath."));
        }

        String url = "jdbc:sqlite:" + databasePath;

        try (Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement()) {

            SwingUtilities.invokeLater(() -> label.setText("Connection successful!"));

            // Read and execute the SQL file
            String sql = new String(inputStream.readAllBytes());
            String[] queries = sql.split(";");

            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query);
                }
            }

            String fetchQuery = "SELECT * FROM SATDInFile";
            ResultSet rs = stmt.executeQuery(fetchQuery);

            List<Object[]> tableData = new ArrayList<>();

            // Displaying query results
            int i = 1; // Initialize comment number
            while (rs.next()) {
                int commentNumber = i;
                String f_comment = rs.getString("f_comment"); // Replace with actual column name
                String f_path = rs.getString("f_path");
                int start_line = rs.getInt("start_line");
                int end_line = rs.getInt("end_line");
                String  containing_class = rs.getString("containing_class");
                String containing_method = rs.getString("containing_method");
                tableData.add(new Object[]{commentNumber, f_comment, f_path, start_line, end_line, containing_class, containing_method}); // Add new row to the table
                i += 1;
            }

            SwingUtilities.invokeLater(() -> {
                for (Object[] tableDatum : tableData) {
                    tableModel.addRow(tableDatum);
                }
            });


            fetchQuery = "SELECT * FROM SATD";
            rs = stmt.executeQuery(fetchQuery);

            List<String> resolutions = new ArrayList<>();
            //i = 0;
            while (rs.next()) {
                //int rowNum = i;
                //String resolution = rs.getString("resolution");
                resolutions.add(rs.getString("resolution"));
                //SwingUtilities.invokeLater(() -> tableModel.setValueAt(resolution, rowNum,7 )); // Add new value to "SATD Type" column
                //i += 1;
            }

            SwingUtilities.invokeLater(() ->{
                for (int j = 0; j < resolutions.size(); j++ ){
                    tableModel.setValueAt(resolutions.get(j), j, 7);
                }
            });

            // Close resources
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> label.setText("Connection failed: " + e.getMessage()));
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