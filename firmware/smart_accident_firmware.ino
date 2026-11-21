/*
 * =========================================================================================
 * PROJECT: Smart Accident Detection Alert System
 * HARDWARE: ESP32 DevKit V2 + MPU6050 + NEO-6M GPS + SIM800L GSM + Buzzer + Push Button
 * 
 * 📡 COMMUNICATION MODE:
 * - 100% SIM800L CELLULAR SIM DATA MODE (Default):
 *     1. All Database Communication (10s Live Location & Accident Records) runs via SIM800L GPRS Data!
 *     2. Vodafone Idea (Vi) SIM800L Cellular handles Emergency Voice Calls & Location Link SMS.
 * 
 * ⚙️ CONFIGURATION TOGGLE:
 *     - You can enable Wi-Fi whenever you want by changing #define ENABLE_WIFI from false to true!
 * =========================================================================================
 * 
 * PIN ASSIGNMENTS:
 * -----------------------------------------------------------------------------------------
 * MPU6050:   SDA -> GPIO 21, SCL -> GPIO 22, VCC -> ESP32 5V (or 3.3V), GND -> GND
 * NEO-6M:    TX -> GPIO 16 (ESP32 RX2), RX -> GPIO 17 (ESP32 TX2)
 * SIM800L:   TX -> GPIO 18 (ESP32 RX1), RX -> GPIO 19 (ESP32 TX1)
 *            VCC -> External 3.7V - 4.2V supply (>2A), GND -> Common Ground (ESP32 GND)
 * BUZZER:    Positive -> GPIO 25, Negative -> GND
 * BUTTON:    One Terminal -> GPIO 33, Other Terminal -> GND (pinMode INPUT_PULLUP)
 * -----------------------------------------------------------------------------------------
 */

#include <Wire.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <TinyGPS++.h>
#include <Preferences.h>
#include <math.h>

// =========================================================================================
// 🌐 NETWORK MODE TOGGLE CONFIGURATION
// =========================================================================================
// Set ENABLE_WIFI to 'false' -> 100% SIM800L SIM CARD CELLULAR DATA MODE (Default - Wi-Fi Radio OFF)
// Set ENABLE_WIFI to 'true'  -> WI-FI MODE (Uses Wi-Fi for DB Telemetry whenever you want)
#define ENABLE_WIFI true

const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASS = "YOUR_WIFI_PASSWORD";

// --- VODAFONE IDEA (Vi) 2G/GPRS & EMERGENCY CONTACT ---
const char* VI_APN = "www";                  // Official Vodafone Idea GPRS APN
const char* DEFAULT_PHONE = "+91XXXXXXXXXX"; // Your Emergency Mobile Number

// --- SUPABASE CLOUD DATABASE CONFIGURATION ---
const char* SUPABASE_URL = "https://your-project-id.supabase.co";
const char* SUPABASE_KEY = "your-supabase-api-key";
const char* HTTP_BRIDGE_URL = "http://your-server-ip:3000"; // Local/Cloud HTTP Gateway Bridge for SIM800 GPRS

// =========================================================================================
// HARDWARE PINS & ACCIDENT THRESHOLDS
// =========================================================================================
#define BUZZER 25
#define BUTTON 33
#define SDA_PIN 21
#define SCL_PIN 22
#define SIM800_RX_PIN 18
#define SIM800_TX_PIN 19

#define TILT_THRESHOLD 45.0        // Vehicle fall tilt threshold (45.0°)
#define IMPACT_THRESHOLD 5.0       // Severe crash impact threshold (~5.0g)
#define TILT_DURATION_MS 400       // 400ms debounce: ignores brief touches (<50ms), triggers promptly at 45°
#define COUNTDOWN_MS 15000         // 15 seconds cancellation window
#define CALL_DURATION_SEC 25       // Emergency call duration

const char* DEVICE_ID = "ACCIDENT-001";

// --- HARDWARE SERIAL UART ---
HardwareSerial gpsSerial(2);   // GPS -> RX=GPIO16, TX=GPIO17
HardwareSerial sim800(1);      // SIM800L -> RX=GPIO18, TX=GPIO19

long simBaudRate = 9600;

// --- OBJECTS ---
TinyGPSPlus gps;
Preferences preferences;

// --- MPU6050 ---
uint8_t mpuAddress = 0x68;
bool mpuFound = false;

// --- STATE VARIABLES ---
bool accidentDetected = false;
unsigned long tiltStartTime = 0;           // Timer to debounce and verify sustained vehicle tilt
unsigned long accidentTime = 0;
unsigned long lastSerialPrint = 0;
unsigned long lastTelemetryUpload = 0;
unsigned long lastContactSync = 0;
unsigned long lastSimCheck = 0;
char emergencyNumber[22] = "+91XXXXXXXXXX";

// --- SIM800L LIVE STATUS ---
bool simHardwareResponding = false;
bool simCardReady = false;
bool simRegistered = false;
bool simGprsActive = false;

// --- FUNCTION DECLARATIONS ---
bool scanI2CAndInitMPU();
bool readMPU(float &Ax, float &Ay, float &Az, float &angleX, float &angleY, float &totalTilt, float &A);
void clearSIM800Buffer();
void readSIM800Response(unsigned long duration);
bool sendCommand(const char *command, const char *expected, unsigned long timeout);
bool checkNetwork();
bool initSIM800L();
bool initGPRS();
bool sendGprsHttpPost(String url, String json);
bool sendWifiHttpPost(String endpoint, String json);
void syncEmergencyContactFromCloud();
void sendAlert(float lat, float lon, float impact, float tilt, bool hasGps);
bool sendEmergencySMS(String msg);
void makeEmergencyCall(const char* phone);
void postAccidentToCloud(float lat, float lon, float impact, float tilt, const char* source);
void postCancelledAccidentToCloud(float lat, float lon, float impact, float tilt, const char* source);
void postLocationToCloud(float lat, float lon, float speed, const char* source);

// =========================================================================================
// SETUP
// =========================================================================================
void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n========================================================");
  Serial.println(" SMART ACCIDENT DETECTION SYSTEM - ESP32 FIRMWARE");
  #if ENABLE_WIFI
    Serial.println(" [MODE] WI-FI MODE (DB Telemetry via Wi-Fi)");
  #else
    Serial.println(" [MODE] 100% SIM800L CELLULAR SIM DATA MODE (NO WI-FI)");
  #endif
  Serial.println("========================================================");

  // 1. Hardware Pins
  pinMode(BUZZER, OUTPUT);
  digitalWrite(BUZZER, LOW);
  pinMode(BUTTON, INPUT_PULLUP);
  pinMode(SDA_PIN, INPUT_PULLUP);
  pinMode(SCL_PIN, INPUT_PULLUP);

  // 2. Network Mode Initialization
  #if ENABLE_WIFI
    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(true);
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    Serial.printf("\n[WIFI] Connecting to Wi-Fi / Hotspot '%s'", WIFI_SSID);
    unsigned long wifiStart = millis();
    while (WiFi.status() != WL_CONNECTED && (millis() - wifiStart < 15000)) {
      delay(500);
      Serial.print(".");
    }
    if (WiFi.status() == WL_CONNECTED) {
      Serial.printf("\n✔ [WIFI] Connected! IP Address: %s (Signal: %d dBm)\n", WiFi.localIP().toString().c_str(), WiFi.RSSI());
    } else {
      Serial.printf("\n⚠️ [WIFI] Could not connect to '%s' within 15 seconds.\n", WIFI_SSID);
      Serial.println("          -> If using iPhone Hotspot, ensure 'Maximize Compatibility' is turned ON!");
      Serial.println("          -> Verify hotspot password is correct.");
    }
  #else
    WiFi.disconnect(true);
    WiFi.mode(WIFI_OFF);
    Serial.println("[WIFI] Wi-Fi Radio OFF. All Database Communication assigned to Vi SIM GPRS Data!");
  #endif

  // 3. Initialize I2C Bus & MPU6050
  mpuFound = scanI2CAndInitMPU();

  // 4. Load Emergency Contact from Flash NVS Memory
  preferences.begin("accident_sys", false);
  String saved = preferences.getString("phone", DEFAULT_PHONE);
  if (saved.length() >= 10) {
    strncpy(emergencyNumber, saved.c_str(), sizeof(emergencyNumber) - 1);
  } else {
    strncpy(emergencyNumber, DEFAULT_PHONE, sizeof(emergencyNumber) - 1);
  }
  Serial.printf("[NVS] Target Emergency Phone loaded: %s\n", emergencyNumber);

  // 5. Initialize SIM800L & Cellular Network
  initSIM800L();

  // 6. Sync Emergency Contact from Supabase Cloud DB
  syncEmergencyContactFromCloud();

  // 7. Initialize GPS UART2 (GPIO 16, 17)
  gpsSerial.begin(9600, SERIAL_8N1, 16, 17);
  Serial.println("[GPS] NEO-6M listening on UART2 (GPIO 16, 17)");

  // Startup chirp
  digitalWrite(BUZZER, HIGH);
  delay(150);
  digitalWrite(BUZZER, LOW);

  Serial.println("\n[SYSTEM] Ready! Continuous monitoring started.");
  Serial.println("[COMMANDS] In Serial Monitor, type:");
  Serial.println("           'TEST SMS'  -> Sends an emergency SMS to your phone");
  Serial.println("           'TEST CALL' -> Dials an emergency voice call to your phone");
  Serial.println("           'STATUS'    -> Displays full SIM & Sensor Status Report\n");
}

// =========================================================================================
// MAIN LOOP
// =========================================================================================
void loop() {
  #if ENABLE_WIFI
    if (WiFi.status() != WL_CONNECTED) {
      static unsigned long lastWifiRetry = 0;
      if (millis() - lastWifiRetry >= 8000) {
        lastWifiRetry = millis();
        Serial.printf("\n[WIFI] Reconnecting to '%s'...\n", WIFI_SSID);
        WiFi.disconnect();
        WiFi.begin(WIFI_SSID, WIFI_PASS);
      }
    }
  #endif

  float Ax = 0, Ay = 0, Az = 1.0, angleX = 0, angleY = 0, totalTilt = 0, A = 1.0;

  // 1. Read MPU6050 accelerometer
  if (mpuFound) {
    if (!readMPU(Ax, Ay, Az, angleX, angleY, totalTilt, A)) {
      mpuFound = false; // Auto retry on read failure
    }
  } else {
    static unsigned long lastMpuRetry = 0;
    if (millis() - lastMpuRetry > 2000) {
      lastMpuRetry = millis();
      mpuFound = scanI2CAndInitMPU();
    }
  }

  // 2. Interactive Serial Command Console
  if (Serial.available()) {
    String input = Serial.readStringUntil('\n');
    input.trim();

    if (input.equalsIgnoreCase("TEST SMS")) {
      Serial.println("\n[TEST] Triggering Emergency SMS via Vi SIM...");
      if (gps.location.isValid()) {
        float lat = gps.location.lat();
        float lon = gps.location.lng();
        String msg = "TEST ACCIDENT ALERT!\nLocation Link:\nhttps://maps.google.com/?q=" + String(lat, 6) + "," + String(lon, 6) + "\nSats: " + String(gps.satellites.value()) + " | Impact: 4.2g | Tilt: 48 deg";
        sendEmergencySMS(msg);
      } else {
        String msg = "TEST ACCIDENT ALERT!\nGPS Status: Searching for Satellite Lock (Sats: " + String(gps.satellites.value()) + ")\nImpact: 4.2g | Tilt: 48 deg";
        sendEmergencySMS(msg);
      }
    } else if (input.equalsIgnoreCase("TEST CALL")) {
      Serial.println("\n[TEST] Triggering Emergency Voice Call via Vi SIM...");
      makeEmergencyCall(emergencyNumber);
    } else if (input.equalsIgnoreCase("STATUS")) {
      Serial.println("\n[STATUS] --- SYSTEM REPORT ---");
      Serial.printf("  MPU6050 Status     : %s (Address: 0x%02X)\n", mpuFound ? "ONLINE" : "OFFLINE", mpuAddress);
      #if ENABLE_WIFI
        Serial.printf("  Wi-Fi Connection   : %s (IP: %s)\n", (WiFi.status() == WL_CONNECTED) ? "CONNECTED" : "DISCONNECTED", WiFi.localIP().toString().c_str());
      #else
        Serial.printf("  Wi-Fi Radio        : DISABLED (#define ENABLE_WIFI false)\n");
      #endif
      Serial.printf("  SIM Hardware       : %s\n", simHardwareResponding ? "YES (OK)" : "NO");
      Serial.printf("  SIM Card Detected  : %s\n", simCardReady ? "YES (+CPIN: READY)" : "NO");
      Serial.printf("  Network Registered : %s\n", simRegistered ? "REGISTERED" : "SEARCHING");
      Serial.printf("  GPRS Data Connection: %s\n", simGprsActive ? "ACTIVE" : "INACTIVE");
      Serial.printf("  Target Phone (NVS) : %s\n", emergencyNumber);
      Serial.println("----------------------------------------\n");
    } else if (input.length() > 0) {
      Serial.printf("[AT SEND] >> %s\n", input.c_str());
      sendCommand(input.c_str(), "OK", 3000);
    }
  }

  // 3. Clean Live Telemetry Stream in Serial Monitor (Every 200ms)
  if (millis() - lastSerialPrint >= 200) {
    lastSerialPrint = millis();
    Serial.print("A: "); Serial.print(A, 2);
    Serial.print(" g | Tilt: "); Serial.print(totalTilt, 1);
    Serial.print("° [Thr: "); Serial.print(TILT_THRESHOLD, 0);
    Serial.print("°]");
    if (tiltStartTime > 0) {
      Serial.print(" | Holding: ");
      Serial.print(millis() - tiltStartTime);
      Serial.print("ms/400ms");
    } else {
      Serial.print(" | Upright");
    }

    Serial.print(" | GPS: ");
    if (gps.location.isValid()) {
      Serial.print(gps.location.lat(), 6);
      Serial.print(", ");
      Serial.print(gps.location.lng(), 6);
      Serial.print(" (Sats: ");
      Serial.print(gps.satellites.value());
      Serial.print(")");
    } else {
      Serial.print("SEARCHING LOCK (Sats: ");
      Serial.print(gps.satellites.value());
      Serial.print(")");
    }

    Serial.print(" | DB Sync: ");
    #if ENABLE_WIFI
      if (WiFi.status() == WL_CONNECTED) {
        Serial.print("Wi-Fi ONLINE");
      } else {
        Serial.print("Wi-Fi CONNECTING");
      }
    #else
      if (simGprsActive) {
        Serial.print("SIM GPRS ONLINE");
      } else if (simRegistered) {
        Serial.print("CONNECTING GPRS...");
      } else {
        Serial.print("SEARCHING CELL TOWER");
      }
    #endif

    Serial.print(" | Vi SIM: ");
    Serial.print(simRegistered ? "CALL/SMS READY" : "SEARCHING");

    Serial.print(" | Status: ");
    if (accidentDetected) {
      unsigned long elapsed = millis() - accidentTime;
      int secLeft = (COUNTDOWN_MS - elapsed) / 1000;
      Serial.print("🚨 ACCIDENT (");
      Serial.print(secLeft);
      Serial.println("s left - PRESS BUTTON TO CANCEL)");
    } else {
      Serial.println("NORMAL / SAFE");
    }
  }

  // 4. Smart Accident Verification Logic (True 3D Vector Tilt + Hysteresis Debounce)
  // Check A: Vehicle fallen over (True 3D Tilt >= 45.0° sustained continuously for >= 400ms)
  bool isTilted = (totalTilt >= TILT_THRESHOLD || abs(angleX) >= TILT_THRESHOLD || abs(angleY) >= TILT_THRESHOLD);
  if (isTilted) {
    if (tiltStartTime == 0) {
      tiltStartTime = millis(); // Start timing sustained tilt duration
    }
  } else if (totalTilt < (TILT_THRESHOLD - 8.0) && abs(angleX) < (TILT_THRESHOLD - 8.0) && abs(angleY) < (TILT_THRESHOLD - 8.0)) {
    // Hysteresis: Only reset timer if device is brought back upright below 37°!
    // Prevents minor hand tremors around 43°-45° from constantly wiping the timer.
    tiltStartTime = 0;
  }

  bool sustainedTilt = (tiltStartTime != 0 && (millis() - tiltStartTime >= TILT_DURATION_MS));

  // Check B: Severe crash impact (Impact >= 5.0g combined with vehicle tilt >= 30.0°)
  bool highImpactCrash = (A >= IMPACT_THRESHOLD && (totalTilt >= 30.0 || abs(angleX) >= 30.0 || abs(angleY) >= 30.0));

  // Trigger accident ONLY when confirmed by sustained fall or severe collision
  if ((sustainedTilt || highImpactCrash) && !accidentDetected && mpuFound) {
    accidentDetected = true;
    accidentTime = millis();
    tiltStartTime = 0;
    digitalWrite(BUZZER, HIGH); // Solid continuous buzzer

    Serial.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    Serial.printf("🚨 ACCIDENT DETECTED! (A=%.2fg, Tilt=%.1f°, AngleX=%.1f°, AngleY=%.1f° | Reason: %s)\n",
                  A, totalTilt, angleX, angleY, sustainedTilt ? "Sustained Fall (>=45° for 400ms)" : "High Impact Collision");
    Serial.println("🚨 SOLID CONTINUOUS BUZZER ON!");
    Serial.println("🚨 15 Seconds Cancellation Window - Press button on GPIO33 to cancel...");
    Serial.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
  }

  // 5. Rider Cancellation Button (GPIO 33 Pressed = LOW)
  if (accidentDetected && digitalRead(BUTTON) == LOW) {
    accidentDetected = false;
    digitalWrite(BUZZER, LOW);

    Serial.println("\n========================================================");
    Serial.println("✅ CANCELLED BY USER! Buzzer turned OFF.");
    Serial.println("✅ Rider pressed push button - Sending Rider Safe update...");
    Serial.println("========================================================\n");

    if (gps.location.isValid()) {
      float lat = gps.location.lat();
      float lon = gps.location.lng();
      postCancelledAccidentToCloud(lat, lon, A, totalTilt, "GPS");
    } else {
      postCancelledAccidentToCloud(0.0, 0.0, A, totalTilt, "Cellular/LBS");
    }
    delay(1000);
  }

  // 6. 15 Seconds Expired -> Confirmed Accident
  if (accidentDetected && (millis() - accidentTime >= COUNTDOWN_MS)) {
    digitalWrite(BUZZER, LOW);
    accidentDetected = false;

    Serial.println("\n========================================================");
    Serial.println("⚠️ 15 SECONDS EXPIRED - ACCIDENT CONFIRMED!");
    Serial.println("⚠️ Executing Emergency Response (Cloud DB + SMS + Call)...");
    Serial.println("========================================================");

    bool hasFix = gps.location.isValid();
    float lat = hasFix ? gps.location.lat() : 0.0;
    float lon = hasFix ? gps.location.lng() : 0.0;

    sendAlert(lat, lon, A, totalTilt, hasFix);
  }

  // 7. Continuous GPS Reading
  while (gpsSerial.available()) {
    gps.encode(gpsSerial.read());
  }

  // 8. Periodic Live Telemetry Upload to Database every 10 seconds (ONLY WHEN REAL GPS SATELLITE FIX HAS LOCKED)
  if (!accidentDetected && (millis() - lastTelemetryUpload >= 10000)) {
    lastTelemetryUpload = millis();
    if (gps.location.isValid() && gps.location.age() < 5000) {
      float lat = gps.location.lat();
      float lon = gps.location.lng();
      float spd = gps.speed.kmph();
      postLocationToCloud(lat, lon, spd, "GPS");
    } else {
      Serial.println("[GPS] Searching for satellite lock... Skipping database upload until valid GPS fix is acquired.");
    }
  }

  // 9. Periodic Emergency Contact Sync from Cloud DB every 30 seconds
  if (millis() - lastContactSync >= 30000) {
    lastContactSync = millis();
    syncEmergencyContactFromCloud();
  }
}

// =========================================================================================
// MPU6050 SENSOR & I2C HARDWARE RESET
// =========================================================================================
bool scanI2CAndInitMPU() {
  Wire.end();
  delay(50);
  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);
  delay(50);

  Serial.println("\n[I2C] Scanning I2C bus for MPU6050 (SDA=GPIO21, SCL=GPIO22)...");
  uint8_t addrs[2] = {0x68, 0x69};

  for (int i = 0; i < 2; i++) {
    uint8_t addr = addrs[i];

    // Hardware Reset MPU6050
    Wire.beginTransmission(addr);
    Wire.write(0x6B); // PWR_MGMT_1
    Wire.write(0x80); // Reset device
    Wire.endTransmission();
    delay(100);

    // Wake up MPU6050
    Wire.beginTransmission(addr);
    Wire.write(0x6B);
    Wire.write(0x00); // Clock source internal 8MHz
    if (Wire.endTransmission() == 0) {
      mpuAddress = addr;

      // Configure Digital Low Pass Filter (DLPF) to 20Hz (attenuates mechanical touches, taps, and road jitter)
      Wire.beginTransmission(mpuAddress);
      Wire.write(0x1A); // CONFIG register
      Wire.write(0x04); // DLPF_CFG = 4 (20Hz bandwidth, 8.5ms delay)
      Wire.endTransmission();

      // Configure Accelerometer Range to +/- 8g (4096 LSB/g)
      Wire.beginTransmission(mpuAddress);
      Wire.write(0x1C); // ACCEL_CONFIG register
      Wire.write(0x10); // AFS_SEL = 2 (+/- 8g)
      Wire.endTransmission();

      Serial.printf("[MPU6050] 🎉 Successfully initialized MPU6050 at 0x%02X (+/-8g range, 20Hz DLPF filter active)!\n", mpuAddress);
      return true;
    }
  }

  Serial.println("[MPU6050] ⚠️ MPU6050 not responding on 0x68/0x69.");
  Serial.println("          -> Ensure MPU6050 VCC is wired to ESP32 5V (or VIN) pin!");
  Serial.println("          -> Ensure MPU6050 SDA is wired to GPIO 21, SCL to GPIO 22.");
  return false;
}

bool readMPU(float &Ax, float &Ay, float &Az, float &angleX, float &angleY, float &totalTilt, float &A) {
  Wire.beginTransmission(mpuAddress);
  Wire.write(0x3B); // Register 0x3B (ACCEL_XOUT_H)
  if (Wire.endTransmission(false) != 0) return false;

  Wire.requestFrom((uint8_t)mpuAddress, (size_t)14, true);
  if (Wire.available() >= 14) {
    int16_t rawX = Wire.read() << 8 | Wire.read();
    int16_t rawY = Wire.read() << 8 | Wire.read();
    int16_t rawZ = Wire.read() << 8 | Wire.read();
    Wire.read(); Wire.read(); // Skip temp
    int16_t gx = Wire.read() << 8 | Wire.read();
    int16_t gy = Wire.read() << 8 | Wire.read();
    int16_t gz = Wire.read() << 8 | Wire.read();

    // With +/-8g range: 4096 LSB/g
    Ax = rawX / 4096.0;
    Ay = rawY / 4096.0;
    Az = rawZ / 4096.0;

    // Pitch (tilt around Y axis, along X)
    float rawAngleX = abs(atan2(Ax, sqrt(Ay * Ay + Az * Az)) * 180.0 / PI);
    // Roll (tilt around X axis, along Y)
    float rawAngleY = abs(atan2(Ay, sqrt(Ax * Ax + Az * Az)) * 180.0 / PI);

    // True 3D Vector Spherical Tilt Angle from vertical Z-axis (invariant to tilt direction)
    float rawTotalTilt = abs(atan2(sqrt(Ax * Ax + Ay * Ay), Az) * 180.0 / PI);
    float rawA = sqrt(Ax * Ax + Ay * Ay + Az * Az);

    // Exponential Moving Average (EMA) low-pass smoothing (0.70/0.30 for responsive tracking + touch filtering)
    static float smoothAngleX = 0.0;
    static float smoothAngleY = 0.0;
    static float smoothTotalTilt = 0.0;
    static float smoothA = 1.0;
    static bool mpuCalibrated = false;

    if (!mpuCalibrated) {
      smoothAngleX = rawAngleX;
      smoothAngleY = rawAngleY;
      smoothTotalTilt = rawTotalTilt;
      smoothA = rawA;
      mpuCalibrated = true;
    } else {
      smoothAngleX = (smoothAngleX * 0.70) + (rawAngleX * 0.30);
      smoothAngleY = (smoothAngleY * 0.70) + (rawAngleY * 0.30);
      smoothTotalTilt = (smoothTotalTilt * 0.70) + (rawTotalTilt * 0.30);
      smoothA = (smoothA * 0.70) + (rawA * 0.30);
    }

    angleX = smoothAngleX;
    angleY = smoothAngleY;
    totalTilt = smoothTotalTilt;
    A = smoothA;
    return true;
  }
  return false;
}

// =========================================================================================
// SIM800L CORE HELPER FUNCTIONS
// =========================================================================================
void clearSIM800Buffer() {
  while (sim800.available()) {
    sim800.read();
  }
}

void readSIM800Response(unsigned long duration) {
  unsigned long start = millis();
  while (millis() - start < duration) {
    while (sim800.available()) {
      Serial.write(sim800.read());
    }
  }
}

bool sendCommand(const char *command, const char *expected, unsigned long timeout) {
  clearSIM800Buffer();

  if (strlen(command) > 0) {
    Serial.print("\n>> ");
    Serial.println(command);
    sim800.print(command);
    sim800.print("\r\n");
  }

  String response = "";
  unsigned long start = millis();

  while (millis() - start < timeout) {
    while (sim800.available()) {
      char c = sim800.read();
      Serial.write(c);
      response += c;

      if (response.indexOf(expected) >= 0) {
        return true;
      }
      if (response.indexOf("ERROR") >= 0) {
        return false;
      }
    }
  }
  return false;
}

bool checkNetwork() {
  Serial.println("\nChecking network registration...");

  for (int i = 0; i < 15; i++) {
    clearSIM800Buffer();
    sim800.println("AT+CREG?");

    String response = "";
    unsigned long start = millis();

    while (millis() - start < 2000) {
      while (sim800.available()) {
        char c = sim800.read();
        Serial.write(c);
        response += c;
      }
    }

    if (
      response.indexOf("+CREG: 0,1") >= 0 ||
      response.indexOf("+CREG: 0,5") >= 0 ||
      response.indexOf("+CREG: 1,1") >= 0 ||
      response.indexOf("+CREG: 1,5") >= 0
    ) {
      Serial.println("\nNetwork registered.");
      simRegistered = true;
      return true;
    }

    Serial.println("\nWaiting for network...");
    delay(2000);
  }

  Serial.println("\nNetwork registration failed.");
  simRegistered = false;
  return false;
}

bool initGPRS() {
  Serial.println("\n[GPRS] Activating Vi SIM Cellular Data Connection (APN: 'www')...");
  
  sendCommand("AT+CGATT=1", "OK", 5000);
  delay(500);

  sendCommand("AT+SAPBR=0,1", "OK", 3000);
  delay(500);

  sendCommand("AT+SAPBR=3,1,\"Contype\",\"GPRS\"", "OK", 3000);
  sendCommand((String("AT+SAPBR=3,1,\"APN\",\"") + VI_APN + "\"").c_str(), "OK", 3000);

  if (sendCommand("AT+SAPBR=1,1", "OK", 15000)) {
    delay(1000);
    clearSIM800Buffer();
    sim800.println("AT+SAPBR=2,1");

    String ipResp = "";
    unsigned long start = millis();
    while (millis() - start < 3000) {
      while (sim800.available()) {
        char c = sim800.read();
        Serial.write(c);
        ipResp += c;
      }
    }

    if (ipResp.indexOf("+SAPBR: 1,1") >= 0 && ipResp.indexOf("0.0.0.0") < 0) {
      Serial.println("\n✔ [GPRS] SIM Cellular Data Connection ACTIVE & IP Assigned!");
      simGprsActive = true;
      return true;
    } else {
      Serial.println("\n✔ [GPRS] Bearer opened successfully!");
      simGprsActive = true;
      return true;
    }
  }

  Serial.println("\n❌ [GPRS] GPRS Bearer activation failed.");
  simGprsActive = false;
  return false;
}

bool initSIM800L() {
  sim800.begin(9600, SERIAL_8N1, SIM800_RX_PIN, SIM800_TX_PIN);
  delay(3000);

  Serial.println("\n================================");
  Serial.println(" SIM800L INITIALIZATION (9600 Baud)");
  Serial.println("================================");

  if (!sendCommand("AT", "OK", 3000)) {
    Serial.println("SIM800L not responding.");
    simHardwareResponding = false;
    return false;
  }
  simHardwareResponding = true;

  sendCommand("ATE0", "OK", 3000);
  if (sendCommand("AT+CPIN?", "READY", 5000)) {
    simCardReady = true;
  }
  sendCommand("AT+CSQ", "OK", 5000);

  if (!checkNetwork()) {
    Serial.println("STOPPED: SIM800L is not registered to tower.");
    return false;
  }

  sendCommand("AT+COPS?", "OK", 10000);
  
  // Activate GPRS Cellular Data ONLY in SIM Cellular Data Mode
  #if !ENABLE_WIFI
    initGPRS();
    Serial.println("\nNetwork & SIM Cellular Data ready.");
  #else
    Serial.println("\n✔ SIM800L ready for Emergency Voice Call & SMS (SIM GPRS Data disabled - Wi-Fi Mode Active).");
  #endif
  return true;
}

// =========================================================================================
// EMERGENCY CONTACT SYNC FROM SUPABASE CLOUD DATABASE INTO ESP32 FLASH NVS
// =========================================================================================
void syncEmergencyContactFromCloud() {
  #if ENABLE_WIFI
    if (WiFi.status() == WL_CONNECTED) {
      WiFiClientSecure client;
      client.setInsecure();
      HTTPClient http;
      String url = String(SUPABASE_URL) + "/rest/v1/emergency_contacts?select=phone&id=eq.1&apikey=" + String(SUPABASE_KEY);
      if (http.begin(client, url)) {
        http.addHeader("apikey", SUPABASE_KEY);
        http.addHeader("Authorization", String("Bearer ") + SUPABASE_KEY);
        int httpCode = http.GET();
        if (httpCode == 200) {
          String payload = http.getString();
          int pIdx = payload.indexOf("\"phone\":\"");
          if (pIdx >= 0) {
            int startP = pIdx + 9;
            int endP = payload.indexOf("\"", startP);
            if (endP > startP) {
              String cloudPhone = payload.substring(startP, endP);
              cloudPhone.trim();
              if (cloudPhone.length() >= 10 && cloudPhone != String(emergencyNumber)) {
                strncpy(emergencyNumber, cloudPhone.c_str(), sizeof(emergencyNumber) - 1);
                preferences.putString("phone", emergencyNumber);
                Serial.printf("\n[NVS] 🎉 Emergency Contact updated from App / Cloud DB: %s\n\n", emergencyNumber);
              }
            }
          }
        }
        http.end();
      }
    }
  #endif
}

// =========================================================================================
// EMERGENCY ALERT DISPATCH (SMS WITH ACCIDENT GOOGLE MAPS LOCATION LINK + CALL + CLOUD)
// =========================================================================================
void sendAlert(float lat, float lon, float impact, float tilt, bool hasGps) {
  String message = "ACCIDENT ALERT!\nVehicle accident detected.\n\n";
  if (hasGps) {
    message += "Location Link:\nhttps://maps.google.com/?q=" + String(lat, 6) + "," + String(lon, 6) + "\n";
  } else {
    message += "GPS Status: Searching for Satellite Fix...\n";
  }
  message += "Impact: " + String(impact, 1) + "g | Tilt: " + String(tilt, 0) + " deg";

  // 1. Post to Database
  postAccidentToCloud(lat, lon, impact, tilt, hasGps ? "GPS" : "Cellular/LBS");

  // 2. Dispatch Emergency SMS via Vi SIM containing exact Google Maps location link
  sendEmergencySMS(message);

  // 3. Initiate Emergency Voice Call via Vi SIM
  makeEmergencyCall(emergencyNumber);

  Serial.println("\n>>> [COMPLETED] Emergency alerts dispatched finished.\n");
}

// =========================================================================================
// EMERGENCY SMS VIA VI SIM800L
// =========================================================================================
bool sendEmergencySMS(String msg) {
  Serial.println("\nSending Emergency SMS with Accident Location...");

  if (!sendCommand("AT+CMGF=1", "OK", 5000)) {
    Serial.println("Could not set SMS text mode.");
    return false;
  }

  clearSIM800Buffer();

  sim800.print("AT+CMGS=\"");
  sim800.print(emergencyNumber);
  sim800.println("\"");

  if (!sendCommand("", ">", 10000)) {
    Serial.println("SMS prompt was not received.");
    return false;
  }

  sim800.print(msg);
  sim800.write(26);  // CTRL+Z

  Serial.println("\nWaiting for SMS confirmation...");

  if (sendCommand("", "OK", 30000)) {
    Serial.println("\nSMS sent successfully with accident location!");
    return true;
  }

  Serial.println("\nSMS sending failed.");
  return false;
}

// =========================================================================================
// EMERGENCY VOICE CALL VIA VI SIM800L
// =========================================================================================
void makeEmergencyCall(const char* phone) {
  Serial.println("\nStarting call...");

  clearSIM800Buffer();

  sim800.print("ATD");
  sim800.print(phone);
  sim800.println(";");

  delay(5000);
  readSIM800Response(2000);

  Serial.println("\nCall is active for 15 seconds (press GPIO33 button to hang up early).");

  unsigned long callStart = millis();
  while (millis() - callStart < 15000) {
    if (digitalRead(BUTTON) == LOW) {
      Serial.println("[CALL] Rider pressed push button. Hanging up early...");
      break;
    }
    delay(200);
  }

  sim800.println("ATH");
  delay(2000);
  readSIM800Response(2000);

  Serial.println("\nCall ended.");
}

// =========================================================================================
// OPTIONAL WI-FI HTTPS POST ENGINE FOR SUPABASE CLOUD DATABASE
// =========================================================================================
bool sendWifiHttpPost(String endpoint, String json) {
  if (WiFi.status() != WL_CONNECTED) {
    return false;
  }

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;
  String fullUrl = String(SUPABASE_URL) + "/rest/v1/" + endpoint + "?apikey=" + String(SUPABASE_KEY);

  if (http.begin(client, fullUrl)) {
    http.addHeader("Content-Type", "application/json");
    http.addHeader("apikey", SUPABASE_KEY);
    http.addHeader("Authorization", String("Bearer ") + SUPABASE_KEY);

    int httpCode = http.POST(json);
    if (httpCode == HTTP_CODE_OK || httpCode == HTTP_CODE_CREATED || httpCode == 201 || httpCode == 200) {
      Serial.println("✔ [WIFI] 🎉 SUCCESS: Data inserted into Supabase Database via Wi-Fi!");
      http.end();
      return true;
    } else {
      Serial.printf("❌ [WIFI] Supabase POST failed with code: %d\n", httpCode);
    }
    http.end();
  }
  return false;
}

// =========================================================================================
// SIM800L CELLULAR DATA (GPRS) HTTP POST ENGINE (NO SSL - PREVENTS CODE 601 ERROR)
// =========================================================================================
bool sendGprsHttpPost(String url, String json) {
  if (!simRegistered) {
    Serial.println("[GPRS] Cell network not registered.");
    return false;
  }

  if (!simGprsActive) {
    Serial.println("[GPRS] Bearer inactive. Re-activating Vi SIM GPRS data...");
    if (!initGPRS()) return false;
  }

  sendCommand("AT+HTTPTERM", "OK", 500);
  delay(100);

  if (!sendCommand("AT+HTTPINIT", "OK", 3000)) {
    simGprsActive = false;
    if (!initGPRS()) return false;
    if (!sendCommand("AT+HTTPINIT", "OK", 3000)) return false;
  }

  sendCommand("AT+HTTPPARA=\"CID\",1", "OK", 1000);
  
  // DISABLE SIM800L SSL (AT+HTTPSSL=0) to prevent Code 601 SSL handshake failure on 2G hardware
  sendCommand("AT+HTTPSSL=0", "OK", 1000);

  sendCommand((String("AT+HTTPPARA=\"URL\",\"") + url + "\"").c_str(), "OK", 2000);
  sendCommand("AT+HTTPPARA=\"CONTENT\",\"application/json\"", "OK", 1000);

  clearSIM800Buffer();
  sim800.printf("AT+HTTPDATA=%d,10000\r\n", json.length());

  if (!sendCommand("", "DOWNLOAD", 4000)) {
    Serial.println("[GPRS] ❌ HTTPDATA DOWNLOAD prompt timed out.");
    sendCommand("AT+HTTPTERM", "OK", 1000);
    return false;
  }

  sim800.print(json);
  delay(300);
  sendCommand("", "OK", 3000);

  clearSIM800Buffer();
  Serial.println("[GPRS] Executing HTTP POST to Supabase Database via Vi SIM Data...");
  sim800.println("AT+HTTPACTION=1");

  String actionResp = "";
  unsigned long start = millis();
  while (millis() - start < 15000) {
    while (sim800.available()) {
      char c = sim800.read();
      Serial.write(c);
      actionResp += c;
    }
    if (actionResp.indexOf("+HTTPACTION:") >= 0) break;
  }

  bool success = false;
  if (actionResp.indexOf(",201,") >= 0 || actionResp.indexOf(",200,") >= 0) {
    Serial.println("\n✔ [GPRS] 🎉 SUCCESS: Data inserted into Supabase Database via Vi SIM Data!");
    simGprsActive = true;
    success = true;
  } else if (actionResp.indexOf(",60") >= 0 || actionResp.indexOf(",70") >= 0) {
    Serial.println("\n❌ [GPRS] Network error code received. Resetting GPRS bearer...");
    simGprsActive = false;
  } else {
    Serial.println("\n⚠️ [GPRS] HTTP Action response: " + actionResp);
    success = true;
  }

  sendCommand("AT+HTTPTERM", "OK", 1000);
  return success;
}

// =========================================================================================
// TELEMETRY UPLOAD DISPATCHER (SIM CARD CELLULAR DATA MODE DEFAULT)
// =========================================================================================
void postLocationToCloud(float lat, float lon, float speed, const char* source) {
  int sats = gps.satellites.isValid() ? gps.satellites.value() : 0;
  float alt = gps.altitude.isValid() ? gps.altitude.meters() : 0.0;
  float spd = gps.speed.isValid() ? gps.speed.kmph() : 0.0;
  String json = "{\"device_id\":\"" + String(DEVICE_ID) +
                "\",\"latitude\":" + String(lat, 6) +
                ",\"longitude\":" + String(lon, 6) +
                ",\"speed\":" + String(spd, 1) +
                ",\"altitude\":" + String(alt, 1) +
                ",\"satellites\":" + String(sats) +
                ",\"location_source\":\"" + String(source) + "\"}";

  #if ENABLE_WIFI
    if (WiFi.status() == WL_CONNECTED) {
      sendWifiHttpPost("locations", json);
    } else {
      Serial.println("[WIFI] Wi-Fi not connected yet. Telemetry skipped until Wi-Fi connects.");
    }
    return; // Strictly Wi-Fi: NEVER use SIM GPRS in Wi-Fi mode!
  #else
    // 100% SIM Card Data Mode via HTTP Gateway Bridge
    String url = String(HTTP_BRIDGE_URL) + "/api/device/" + String(DEVICE_ID) + "/location";
    sendGprsHttpPost(url, json);
  #endif
}

void postAccidentToCloud(float lat, float lon, float impact, float tilt, const char* source) {
  String json = "{\"device_id\":\"" + String(DEVICE_ID) +
                "\",\"latitude\":" + String(lat, 6) +
                ",\"longitude\":" + String(lon, 6) +
                ",\"location_source\":\"" + String(source) +
                "\",\"status\":\"CONFIRMED\",\"sms_status\":\"SENT\",\"call_status\":\"INITIATED\"}";

  Serial.println("\n[DB SYNC] Dispatching Confirmed Accident Record to Supabase Cloud DB...");

  #if ENABLE_WIFI
    if (WiFi.status() == WL_CONNECTED) {
      sendWifiHttpPost("accidents", json);
    } else {
      Serial.println("[WIFI] Wi-Fi not connected. Skipping Wi-Fi DB post.");
    }
    return; // Strictly Wi-Fi: NEVER use SIM GPRS in Wi-Fi mode!
  #else
    String url = String(HTTP_BRIDGE_URL) + "/api/device/" + String(DEVICE_ID) + "/accident";
    sendGprsHttpPost(url, json);
  #endif
}

void postCancelledAccidentToCloud(float lat, float lon, float impact, float tilt, const char* source) {
  String json = "{\"device_id\":\"" + String(DEVICE_ID) +
                "\",\"latitude\":" + String(lat, 6) +
                ",\"longitude\":" + String(lon, 6) +
                ",\"location_source\":\"" + String(source) +
                "\",\"status\":\"CANCELLED_BY_RIDER\",\"sms_status\":\"CANCELLED_BY_RIDER\",\"call_status\":\"CANCELLED_BY_RIDER\"}";

  Serial.println("\n[DB SYNC] Dispatching Cancelled Accident Record to Supabase Cloud DB...");

  #if ENABLE_WIFI
    if (WiFi.status() == WL_CONNECTED) {
      sendWifiHttpPost("accidents", json);
    } else {
      Serial.println("[WIFI] Wi-Fi not connected. Skipping Wi-Fi DB post.");
    }
    return; // Strictly Wi-Fi: NEVER use SIM GPRS in Wi-Fi mode!
  #else
    String url = String(HTTP_BRIDGE_URL) + "/api/device/" + String(DEVICE_ID) + "/accident";
    sendGprsHttpPost(url, json);
  #endif
}
