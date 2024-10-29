import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class SATDToolWindowFactory {

    public static void main(String[] args) {
        JFrame frame = new JFrame("SATD Tool Window");
        JPanel toolWindowPanel = new JPanel();
        toolWindowPanel.setLayout(new BoxLayout(toolWindowPanel, BoxLayout.Y_AXIS)); // Set vertical layout for new lines

        JLabel label = new JLabel("Connecting to SATD database...");
        toolWindowPanel.add(label);

        JScrollPane scrollPane = new JScrollPane(toolWindowPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Always show vertical scroll bar

        frame.add(scrollPane);

        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

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
