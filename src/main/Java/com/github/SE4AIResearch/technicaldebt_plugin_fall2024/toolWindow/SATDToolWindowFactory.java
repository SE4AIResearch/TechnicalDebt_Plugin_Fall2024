package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.toolWindow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SATDToolWindowFactory {

    public static void main(String[] args) {
        // Create and show the GUI in a standalone application
        JFrame frame = new JFrame("SATD Tool Window");
        JPanel toolWindowPanel = new JPanel();
        toolWindowPanel.setLayout(new BoxLayout(toolWindowPanel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("Connecting to SATD database...");
        toolWindowPanel.add(label);

        frame.add(toolWindowPanel);
        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Perform the database connection in a separate method
        connectToDatabase(toolWindowPanel, label);
    }

    public static void connectToDatabase(JPanel toolWindowPanel, JLabel label) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // This will throw an exception if the driver is not found
            System.out.println("Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Check your classpath.");
        }
        try {
            // Establish a connection to the MySQL database
            //TODO: make URL, user, and password not hardcoded
            String url = "jdbc:mysql://localhost:3306/satd"; // Update with your database name
            String user = "root"; // Replace with your MySQL username
            String password = "1Sow74902hope"; // Replace with your MySQL password
            
            Connection conn = DriverManager.getConnection(url, user, password);
            label.setText("Connection successful!");

            // Create a statement and execute a simple query
            Statement stmt = conn.createStatement();
            String query = "SELECT * FROM SATDInFile"; // Example query
            ResultSet rs = stmt.executeQuery(query);

            // Displaying query results
            while (rs.next()) {
                String record = rs.getString("f_comment"); // Replace with actual column name
                JLabel resultLabel = new JLabel("Comment: " + record);
                toolWindowPanel.add(resultLabel);
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
