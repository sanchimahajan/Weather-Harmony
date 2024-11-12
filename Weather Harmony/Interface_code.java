import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ESP32MotorApp {

    static JLabel temperatureLabel;
    static JLabel motorSpeedLabel;
    static Connection connection; // MySQL connection

    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("ESP32 Motor Controller");

        // Set frame size
        frame.setSize(400, 200);

        // Create labels to display temperature and motor speed
        temperatureLabel = new JLabel("Temperature: Fetching...", SwingConstants.CENTER);
        temperatureLabel.setBounds(50, 20, 300, 30);

        motorSpeedLabel = new JLabel("Motor Speed: Fetching...", SwingConstants.CENTER);
        motorSpeedLabel.setBounds(50, 60, 300, 30);

        // Create a button to manually fetch data
        JButton fetchButton = new JButton("Fetch Data");
        fetchButton.setBounds(150, 100, 100, 30);

        // Add action listener for the button
        fetchButton.addActionListener((ActionEvent e) -> {
            fetchDataFromESP32(); // Fetch data from ESP32
        });

        // Add components to the frame
        frame.setLayout(null);
        frame.add(fetchButton);
        frame.add(temperatureLabel);
        frame.add(motorSpeedLabel);

        // Set default close operation
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Make frame visible
        frame.setVisible(true);

        // Initialize database connection
        initializeDatabase();
    }

    // Initialize database connection
    public static void initializeDatabase() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Replace with your MySQL database details
            String url = "jdbc:mysql://localhost:3307/esp32_data";
            String user = "root";
            String password = "Vatsakulkarni@123";
            
            // Establish the connection
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected successfully!");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to fetch data from ESP32
    public static void fetchDataFromESP32() {
        try {
            // Replace with the IP address of your ESP32
            URI uri = new URI("http", null, "192.168.247.31", -1, "/", null, null);
            URL url = uri.toURL();
            //
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Check if connection is successful
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed: HTTP error code : " + conn.getResponseCode());
            }

            // Read the response from ESP32
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            StringBuilder response = new StringBuilder();
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            // Extract temperature and motor speed from the HTML response
            parseAndDisplayData(response.toString());

            conn.disconnect();

        } catch (IOException | RuntimeException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // Method to parse the HTML response and display data in the labels
    public static void parseAndDisplayData(String html) {
        try {
            // Extract temperature
            int tempIndex = html.indexOf("Temperature: ") + 13;
            int tempEndIndex = html.indexOf("°C", tempIndex);
            String temperature = html.substring(tempIndex, tempEndIndex);

            // Extract motor speed
            int speedIndex = html.indexOf("Motor Speed: ") + 13;
            int speedEndIndex = html.indexOf("</p>", speedIndex);
            String motorSpeed = html.substring(speedIndex, speedEndIndex);

            // Update the labels on the GUI
            SwingUtilities.invokeLater(() -> {
                temperatureLabel.setText("Temperature: " + temperature + " °C");
                motorSpeedLabel.setText("Motor Speed: " + motorSpeed);
            });

            // Store the parsed data into MySQL database
            storeDataInDatabase(Float.parseFloat(temperature), Float.parseFloat(motorSpeed));

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // Method to store data in MySQL database
    public static void storeDataInDatabase(float temperature, float motorSpeed) {
        if (connection == null) {
            System.err.println("Database connection successful");
            return;
        }

        String insertQuery = "INSERT INTO motor_data (temperature, motor_speed) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            pstmt.setFloat(1, temperature);
            pstmt.setFloat(2, motorSpeed);

            // Execute update and check if it affects any row
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Data stored successfully!");
            } else {
                System.out.println("Failed to store data!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
