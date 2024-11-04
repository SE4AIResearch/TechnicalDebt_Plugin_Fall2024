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
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

public class SATDToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel toolWindowPanel = new JPanel();
        toolWindowPanel.setLayout(new BoxLayout(toolWindowPanel, BoxLayout.Y_AXIS)); // Set vertical layout for new lines

        JBLabel label = new JBLabel("Connecting to SATD database...");
        toolWindowPanel.add(label, BorderLayout.CENTER);

        //JScrollPane scrollPane = new JScrollPane(toolWindowPanel);
        //scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Always show vertical scroll bar
        //scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        //TODO: Get working Scrollbar

        connectToDatabase(toolWindowPanel, label);

        //Adds our panel to Intellij's content factory
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toolWindowPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public static void connectToDatabase(JPanel toolWindowPanel, JBLabel label) {
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
            String record = "";
            while (rs.next()) {
                record += "Comment: ";
                record += rs.getString("f_comment"); // Replace with actual column name
                record += '\n';
                //JBLabel resultLabel = new JBLabel("Comment: " + record);
                //toolWindowPanel.add(resultLabel);
            } 

            label.setText(record);
            JBScrollPane scrollPane = new JBScrollPane(label, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            toolWindowPanel.add(scrollPane, BorderLayout.CENTER);

            // Close resources
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            label.setText("Connection failed: " + e.getMessage());
        }
    }
}
