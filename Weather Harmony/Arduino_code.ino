// Include the libraries
#include <L298N.h>
#include <DHT.h>
#include <WiFi.h>
#include <WebServer.h>
#include <HTTPClient.h>

// Pin definition for the motor
const unsigned int IN1 = 18;
const unsigned int IN2 = 19;
const unsigned int EN = 5;

// Pin definition for the DHT22 sensor
const unsigned int DHTPIN = 15; // Pin where the DHT22 is connected
#define DHTTYPE DHT22 // Define sensor type

// Create motor and DHT22 sensor instances
L298N motor(EN, IN1, IN2);
DHT dht(DHTPIN, DHTTYPE);

// Speed limits based on temperature (adjust as needed)
const int MIN_TEMP = 30; // Minimum temperature for motor to start
const int MAX_TEMP = 35; // Maximum temperature for max speed
const int MIN_SPEED = 100; // Minimum motor speed
const int MAX_SPEED = 255; // Maximum motor speed

// WiFi credentials
const char* ssid = "SK's REALME 7";
const char* password = "realme 7";

// Create a WebServer object on port 80
WebServer server(80);

// Variables to store temperature and motor speed
float temperature = 0.0;
float motor_speed = 0.0;

// Function to send data to the server
void sendDataToServer(float temperature, float motor_speed) {
  String serverName = "http://192.168.247.214/store_data.php?temperature=" + String(temperature) + "&motor_speed=" + String(motor_speed);

  if (WiFi.status() == WL_CONNECTED) { // Check WiFi connection
    HTTPClient http;
    http.begin(serverName); // Start HTTP request

    // Send GET request
    int httpResponseCode = http.GET();

    // Check response
    if (httpResponseCode > 0) {
      String response = http.getString();
      Serial.println("Data sent successfully");
      Serial.println(response);
    } else {
      Serial.print("Error sending data: ");
      Serial.println(httpResponseCode);
    }

    // End connection
    http.end();
  } else {
    Serial.println("WiFi Disconnected");
  }
}

void handleRoot() {
  String html = "<html><body>";
  html += "<h1>ESP32 Motor Controller</h1>";
  html += "<p>Temperature: " + String(temperature) + " °C</p>";
  html += "<p>Motor Speed: " + String(motor_speed) + "</p>";
  html += "</body></html>";
  server.send(200, "text/html", html);
}

void setup() {
  // Start serial communication for debugging
  Serial.begin(9600);

  // Initialize DHT sensor
  dht.begin();

  // Set initial motor speed
  motor.setSpeed(70);

  // Connect to Wi-Fi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");
  Serial.print("ESP32 IP Address: ");
  Serial.println(WiFi.localIP());

  // Define root route and bind it to the handleRoot function
  server.on("/", handleRoot);

  // Start the server
  server.begin();
}

void loop() {
  // Handle client requests
  server.handleClient();

  // Read the temperature from DHT22 sensor
  temperature = dht.readTemperature();

  // Check if the reading is valid
  if (isnan(temperature)) {
    Serial.println("Failed to read from DHT sensor!");
    return;
  }

  // Print temperature to Serial Monitor
  Serial.print("Temperature: ");
  Serial.print(temperature);
  Serial.println(" °C");

  // Adjust motor speed based on temperature
  motor_speed = map(temperature, MIN_TEMP, MAX_TEMP, MIN_SPEED, MAX_SPEED);
  motor_speed = constrain(motor_speed, MIN_SPEED, MAX_SPEED); // Constrain speed within range

  // Set motor speed
  motor.setSpeed(motor_speed);

  // Run motor forward if temperature is above minimum threshold
  if (temperature >= MIN_TEMP) {
    motor.forward();
  } else {
    motor.stop();
  }

  // Send real-time data to the server
  sendDataToServer(temperature, motor_speed);

  delay(1000); // Wait for 1 second before next reading
}
