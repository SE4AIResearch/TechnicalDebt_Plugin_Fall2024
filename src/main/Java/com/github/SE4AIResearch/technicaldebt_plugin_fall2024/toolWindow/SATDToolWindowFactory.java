package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.toolWindow;
package technicaldebt_plugin_fall2024.toolWindow.`ToolWindow `

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import com.intellij.openapi.ui.Messages;

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

        String testRepo = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/test_repo.csv";
        writeTestRepo(testRepo, project);

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

        //Creates the directory if it doesn't exist.
        try {
            Files.createDirectories(Paths.get(PathManager.getConfigPath() + "/databases"));
        } catch (IOException e) {
            label.setText("Error: " + e.getMessage());
        }

        File db = new File(PathManager.getConfigPath() + "/databases", project.getName() + ".db");
        try {
            //If database file is created, initialize it.
            if(db.createNewFile()){
                new initialize(label, project.getName(), button).execute();
            }
            //Else load the database if it exists
            else {
                String title = "Warning";
                String message = "Loading existing SATD data for this  project. May not include most recent commits.";
                Messages.showWarningDialog(message, title);
                new loadDatabase(tableModel, label, table, tableModel2, table2, project.getName(), button).execute();
            }
        } catch (IOException e){
            label.setText("Error: " + e.getMessage());
        }

        // Set button action
        button.addActionListener(e -> {
            //TODO: Fix progress manager so that it actually waits for the task to finish
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    () -> new initializeAndConnectDatabase(tableModel, label, table, tableModel2, table2, project.getName(), button).execute(),
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
        int lastSlashIndex = path.lastIndexOf('/');
        String fileName = path.substring(lastSlashIndex + 1);
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file == null) {
            System.out.println("File " + fileName  + " not found: " + path);
            String message = "File not found at given path";
            String title = "Error";
            Messages.showErrorDialog(message, title);
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

    public static String getGitHubUrl(Project project) {
        VirtualFile gitConfigFile = LocalFileSystem.getInstance()
                .findFileByPath(project.getBasePath() + "/.git/config");

        if (gitConfigFile != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(gitConfigFile.getInputStream()))) {

                String configContent = reader.lines().collect(Collectors.joining("\n"));
                return extractRemoteUrl(configContent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;

    }

    private static String extractRemoteUrl(String configContent) {
        String[] lines = configContent.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals("[remote \"origin\"]")) {
                for (int j = i + 1; j < lines.length; j++) {
                    if (lines[j].trim().startsWith("url =")) {
                        return lines[j].trim().substring(6).trim();
                    }
                }
            }
        }
        return null;

    }

    public void writeTestRepo (String path, Project project){
        String gitUrl = getGitHubUrl(project);
        if(gitUrl.endsWith(".git")) {
            gitUrl = gitUrl.substring(0, gitUrl.length() - 4);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            // This will empty the file and write the new content
            writer.write(gitUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static class initialize extends SwingWorker<Void, Void>{
        private final JBLabel label;
        private final String projectName;
        private final JButton button;

        public initialize(JBLabel label, String projectName, JButton button){
            this.label = label;
            this.projectName = projectName;
            this.button = button;
        }

        @Override
        protected Void doInBackground() throws Exception {
            SwingUtilities.invokeLater(() ->button.setEnabled(false));
            String sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql";
            String databasePath = PathManager.getConfigPath() + "/databases/" + projectName + ".db";

            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(sqlFilePath);
            } catch (FileNotFoundException e) {
                SwingUtilities.invokeLater(() ->label.setText("Error: " + e.getMessage()));
            }

            try {
                Class.forName("org.sqlite.JDBC"); // This will throw an exception if the driver is not found
                System.out.println("Driver loaded successfully.");
            } catch (ClassNotFoundException e) {
                SwingUtilities.invokeLater(() ->label.setText("SQLite JDBC Driver not found. Check your classpath."));
            }

            String url = "jdbc:sqlite:" + databasePath;

            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {

                String sql = new String(inputStream.readAllBytes());
                String[] queries = sql.split(";");

                for (String query : queries) {
                    if (!query.trim().isEmpty()) {
                        stmt.execute(query);
                    }
                }

                stmt.close();
                conn.close();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->label.setText("Error: " + e.getMessage()));
            }

            SwingUtilities.invokeLater(() ->button.setEnabled(true));
            return null;
        }
    }

    private static class loadDatabase extends SwingWorker<Void, Void> {
        private final DefaultTableModel tableModel;
        private final DefaultTableModel tableModel2;
        private final JBLabel label;
        private final JTable table;
        private final JTable table2;
        private final String projectName;
        private final JButton button;


        public loadDatabase(DefaultTableModel tableModel, JBLabel label, JTable table, DefaultTableModel tableModel2, JTable table2, String projectName, JButton button){
            this.tableModel = tableModel;
            this.tableModel2 = tableModel2;
            this.label = label;
            this.table = table;
            this.table2 = table2;
            this.projectName = projectName;
            this.button = button;
        }

        @Override
        protected Void doInBackground() throws Exception {
            SwingUtilities.invokeLater(() ->button.setEnabled(false));
            SwingUtilities.invokeLater(() ->tableModel.setRowCount(0));
            SwingUtilities.invokeLater(() ->tableModel2.setRowCount(0));
            String databasePath = PathManager.getConfigPath() + "/databases/" + projectName + ".db";

            try {
                Class.forName("org.sqlite.JDBC"); // This will throw an exception if the driver is not found
                System.out.println("Driver loaded successfully.");
            } catch (ClassNotFoundException e) {
                SwingUtilities.invokeLater(() ->label.setText("SQLite JDBC Driver not found. Check your classpath."));
            }

            String url = "jdbc:sqlite:" + databasePath;

            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {

                String fetchQuery = "SELECT * FROM SATDInFile";
                ResultSet rs = stmt.executeQuery(fetchQuery);

                // Displaying query results
                while (rs.next()) {
                    int f_id = rs.getInt("f_id");
                    String f_comment = rs.getString("f_comment"); // Replace with actual column name
                    String f_path = rs.getString("f_path");
                    int start_line = rs.getInt("start_line");
                    int end_line = rs.getInt("end_line");
                    String containing_class = rs.getString("containing_class");
                    String containing_method = rs.getString("containing_method");
                    SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{f_id, f_comment, f_path, start_line, end_line, containing_class, containing_method}));
                }

                fetchQuery = "SELECT * FROM SATD";
                rs = stmt.executeQuery(fetchQuery);

                while (rs.next()) {
                    int satd_id = rs.getInt("satd_id");
                    int first_file = rs.getInt("first_file");
                    int second_file = rs.getInt("second_file");
                    String resolution = rs.getString("resolution");
                    SwingUtilities.invokeLater(() -> tableModel2.addRow(new Object[]{satd_id, first_file, second_file, resolution}));
                }

                SwingUtilities.invokeLater(() ->adjustColumnWidths(table));

                rs.close();
                stmt.close();
                conn.close();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->label.setText("Error: " + e.getMessage()));
            }

            SwingUtilities.invokeLater(() ->button.setEnabled(true));
            return null;
        }
    }

    private static class initializeAndConnectDatabase extends SwingWorker<Void, Void> {
        private final DefaultTableModel tableModel;
        private final DefaultTableModel tableModel2;
        private final JBLabel label;
        private final JTable table;
        private final JTable table2;
        private final String projectName;
        private final JButton button;


        initializeAndConnectDatabase(DefaultTableModel tableModel, JBLabel label, JTable table, DefaultTableModel tableModel2, JTable table2, String projectName, JButton button){
            this.tableModel = tableModel;
            this.tableModel2 = tableModel2;
            this.label = label;
            this.table = table;
            this.table2 = table2;
            this.projectName = projectName;
            this.button = button;
        }

        @Override
        protected Void doInBackground() throws Exception {
            //Gets the sql filepath from sql folder
            SwingUtilities.invokeLater(() ->button.setEnabled(false));
            SwingUtilities.invokeLater(() ->tableModel.setRowCount(0));
            SwingUtilities.invokeLater(() ->tableModel2.setRowCount(0));
            String sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql";
            String databasePath = PathManager.getConfigPath() + "/databases/" + projectName + ".db";
            String libPath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/";

            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(sqlFilePath);
            } catch (FileNotFoundException e) {
                SwingUtilities.invokeLater(() ->label.setText("Error: " + e.getMessage()));
            }

            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();

            // Load the MySQL JDBC Driver
            try {
                Class.forName("org.sqlite.JDBC"); // This will throw an exception if the driver is not found
                System.out.println("Driver loaded successfully.");
            } catch (ClassNotFoundException e) {
                SwingUtilities.invokeLater(() ->label.setText("SQLite JDBC Driver not found. Check your classpath."));
            }

            String url = "jdbc:sqlite:" + databasePath;

            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {

                SwingUtilities.invokeLater(() ->label.setText("Connection successful!"));

                // Read and execute the SQL file
                String sql = new String(inputStream.readAllBytes());
                String[] queries = sql.split(";");

                for (String query : queries) {
                    if (!query.trim().isEmpty()) {
                        stmt.execute(query);
                    }
                }

                // Update progress
                if (indicator != null) {
                    SwingUtilities.invokeLater(() ->indicator.setText("Database initialization complete."));
                    SwingUtilities.invokeLater(() ->indicator.setFraction(0.33));
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
                            (databasePath)
                    );

                    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

                    Process process = processBuilder.start();

                    int exitCode = process.waitFor();
                    System.out.println("Exit code:" + exitCode);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                // Update progress
                if (indicator != null) {
                    SwingUtilities.invokeLater(() ->indicator.setText("Database up to Date."));
                    SwingUtilities.invokeLater(() ->indicator.setFraction(0.66));
                }

                String fetchQuery = "SELECT * FROM SATDInFile";
                ResultSet rs = stmt.executeQuery(fetchQuery);

                // Displaying query results
                while (rs.next()) {
                    int f_id = rs.getInt("f_id");
                    String f_comment = rs.getString("f_comment"); // Replace with actual column name
                    String f_path = rs.getString("f_path");
                    int start_line = rs.getInt("start_line");
                    int end_line = rs.getInt("end_line");
                    String containing_class = rs.getString("containing_class");
                    String containing_method = rs.getString("containing_method");
                    SwingUtilities.invokeLater(() ->tableModel.addRow(new Object[]{f_id, f_comment, f_path, start_line, end_line, containing_class, containing_method}));
                }

                fetchQuery = "SELECT * FROM SATD";
                rs = stmt.executeQuery(fetchQuery);

                int i = 0;
                while (rs.next()) {
                    int satd_id = rs.getInt("satd_id");
                    int first_file = rs.getInt("first_file");
                    int second_file = rs.getInt("second_file");
                    String resolution = rs.getString("resolution");
                    SwingUtilities.invokeLater(() ->tableModel2.addRow(new Object[]{satd_id, first_file, second_file, resolution}));
                }

                SwingUtilities.invokeLater(() ->adjustColumnWidths(table));
                //adjustColumnWidths(table2);

                // Update progress
                if (indicator != null) {
                    SwingUtilities.invokeLater(() ->indicator.setText("Data fetching complete."));
                    SwingUtilities.invokeLater(() ->indicator.setFraction(1.0));
                }
                ;

                // Close resources
                rs.close();
                stmt.close();
                conn.close();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->label.setText("Connection failed: " + e.getMessage()));
            }

            SwingUtilities.invokeLater(() -> button.setEnabled(true));
            return null;
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
