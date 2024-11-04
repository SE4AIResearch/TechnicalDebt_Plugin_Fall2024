package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.toolWindow;

import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

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

        // Create the JTable with the model
        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Disable auto-resizing for better control

        // Set preferred widths for the columns
        TableColumn column1 = table.getColumnModel().getColumn(0);
        TableColumn column2 = table.getColumnModel().getColumn(1);
        column1.setPreferredWidth(100);
        column2.setPreferredWidth(500);

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
            String password = "Srasg256"; // Replace with your MySQL password

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
                tableModel.addRow(new Object[]{commentNumber++, f_comment}); // Add new row to the table
            }

            // Close resources
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            label.setText("Connection failed: " + e.getMessage());
        }
    }
}
