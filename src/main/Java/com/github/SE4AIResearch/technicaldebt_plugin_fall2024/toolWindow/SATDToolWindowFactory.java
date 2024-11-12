package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.toolWindow;

import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
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
        tableModel.addColumn("Comment Type");
        tableModel.addColumn("Path");
        tableModel.addColumn("Start Line");
        tableModel.addColumn("End Line");
        tableModel.addColumn("Containing Class");
        tableModel.addColumn("Containing Method");
        tableModel.addColumn("Method Declaration");
        tableModel.addColumn("Method Body");
        tableModel.addColumn("Type");

        // Create the JTable with the model
        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Disable auto-resizing for better control

        // Set preferred widths for the columns
        table.getColumnModel().getColumn(0).setPreferredWidth(110); // Comment Number
        table.getColumnModel().getColumn(1).setPreferredWidth(500); // Comment
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Comment Type
        table.getColumnModel().getColumn(3).setPreferredWidth(100); //Path
        table.getColumnModel().getColumn(4).setPreferredWidth(100); //Start Line
        table.getColumnModel().getColumn(5).setPreferredWidth(100); //End Line
        table.getColumnModel().getColumn(6).setPreferredWidth(100); //Containing Class
        table.getColumnModel().getColumn(7).setPreferredWidth(100); //Containing Method
        table.getColumnModel().getColumn(8).setPreferredWidth(100); //Method Declaration
        table.getColumnModel().getColumn(9).setPreferredWidth(100); //Method Body
        table.getColumnModel().getColumn(10).setPreferredWidth(100); //Type

        table.setEnabled(false);

        // Set custom renderer for the "Comment" column to allow text wrapping
        table.getColumnModel().getColumn(1).setCellRenderer(new TextAreaRenderer());

        // Create the scroll pane and add the table
        JBScrollPane scrollPane = new JBScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toolWindowPanel.add(scrollPane, BorderLayout.CENTER);

        connectToDatabase(tableModel, label);

        // Adds our panel to IntelliJ's content factory
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toolWindowPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public static void connectToDatabase(DefaultTableModel tableModel, JBLabel label) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // This will throw an exception if the driver is not found
            System.out.println("Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Check your classpath.");
        }
        try {
            // Establish a connection to the MySQL database
            String url = "jdbc:mysql://localhost:3306/satd"; // Update with your database name
            String user = "root"; // Replace with your MySQL username
            String password = "1Sow74902hope"; // Replace with your MySQL password

            Connection conn = DriverManager.getConnection(url, user, password);
            label.setText("Connection successful!");

            // Create a statement and execute a simple query
            Statement stmt = conn.createStatement();
            String query = "SELECT * FROM satd.SATDInFile"; // Example query
            ResultSet rs = stmt.executeQuery(query);

            // Displaying query results
            int commentNumber = 1; // Initialize comment number
            while (rs.next()) {
                String f_comment = rs.getString("f_comment"); // Replace with actual column name
                String f_comment_type = rs.getString("f_comment_type");
                String f_path = rs.getString("f_path");
                int start_line = rs.getInt("start_line");
                int end_line = rs.getInt("end_line");
                String  containing_class = rs.getString("containing_class");
                String containing_method = rs.getString("containing_method");
                String method_declaration = rs.getString("method_declaration");
                String method_body = rs.getString("method_body");
                String type = rs.getString("type");
                tableModel.addRow(new Object[]{commentNumber++, f_comment, f_comment_type, f_path, start_line, end_line, containing_class, containing_method, method_declaration, method_body,type}); // Add new row to the table
            }

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
}
