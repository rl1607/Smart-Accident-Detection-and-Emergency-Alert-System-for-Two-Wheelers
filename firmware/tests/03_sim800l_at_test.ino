/*
 * =========================================================================================
 * TEST SKETCH 03: SIM800L COMPLETE HARDWARE AND NETWORK DIAGNOSTIC TEST
 * 
 * Pins:
 * SIM800L TXD -> ESP32 GPIO 18 (ESP32 RX1)
 * SIM800L RXD -> ESP32 GPIO 19 (ESP32 TX1)
 * SIM800L VCC -> External 3.7V - 4.2V (>2A peak current)
 * SIM800L GND -> ESP32 GND (Common Ground)
 * =========================================================================================
 */

#include <HardwareSerial.h>

#define SIM800_RX_PIN 18
#define SIM800_TX_PIN 19
const char* TARGET_PHONE = "+919353035530";

HardwareSerial simSerial(1);

String sendAT(String cmd, unsigned long timeout = 2500) {
  String response = "";
  Serial.print(">> " + cmd + "\n");
  while (simSerial.available()) simSerial.read();
  simSerial.println(cmd);
  unsigned long start = millis();
  while (millis() - start < timeout) {
    while (simSerial.available()) {
      char c = simSerial.read();
      response += c;
    }
  }
  Serial.print("<< " + response + "\n");
  return response;
}

void setup() {
  Serial.begin(115200);
  delay(1500);
  Serial.println("\n========================================================");
  Serial.println(" ESP32 SIM800L DIAGNOSTIC AND CALL / SMS TEST");
  Serial.println("========================================================");

  Serial.println("[STEP 1] Testing SIM800L at 115200 baud...");
  simSerial.begin(115200, SERIAL_8N1, SIM800_RX_PIN, SIM800_TX_PIN);
  delay(500);

  bool connected = false;
  for (int i = 0; i < 3; i++) {
    simSerial.println("AT");
    delay(200);
    String r = "";
    while (simSerial.available()) r += (char)simSerial.read();
    if (r.indexOf("OK") != -1) {
      connected = true;
      Serial.println("✔ SIM800L Responded OK at 115200 baud!");
      break;
    }
  }

  if (!connected) {
    Serial.println("[STEP 1b] Testing SIM800L at 9600 baud...");
    simSerial.end();
    delay(100);
    simSerial.begin(9600, SERIAL_8N1, SIM800_RX_PIN, SIM800_TX_PIN);
    delay(500);
    for (int i = 0; i < 3; i++) {
      simSerial.println("AT");
      delay(200);
      String r = "";
      while (simSerial.available()) r += (char)simSerial.read();
      if (r.indexOf("OK") != -1) {
        connected = true;
        Serial.println("✔ SIM800L Responded OK at 9600 baud!");
        break;
      }
    }
  }

  if (!connected) {
    Serial.println("\n❌ SIM800L is not responding to AT commands.");
    Serial.println("1. Ensure SIM800L GND is connected to ESP32 GND.");
    Serial.println("2. Ensure SIM800L TXD -> GPIO 18, RXD -> GPIO 19.");
    Serial.println("3. If using EVB board, connect VDD pin to ESP32 3.3V.");
    Serial.println("4. Ensure SIM800L has 3.7V - 4.2V power.\n");
    return;
  }

  Serial.println("\n[STEP 2] Running SIM and Network Diagnostics...");
  sendAT("ATE0");
  sendAT("AT+CPIN?");
  sendAT("AT+CSQ");
  sendAT("AT+CREG?");
  sendAT("AT+COPS?");
  sendAT("AT+CBC");

  Serial.println("\n========================================================");
  Serial.println(" DIAGNOSTICS COMPLETE!");
  Serial.println(" Type 'SMS' to send SMS, 'CALL' to place voice call");
  Serial.println("========================================================\n");
}

void loop() {
  if (Serial.available()) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    if (cmd.equalsIgnoreCase("SMS")) {
      Serial.println("\n[SMS] Sending test SMS to " + String(TARGET_PHONE) + "...");
      sendAT("AT+CMGF=1", 800);
      simSerial.printf("AT+CMGS=\"%s\"\r\n", TARGET_PHONE);
      delay(800);
      simSerial.print("Test SMS from ESP32 SIM800L! Your Vi SIM is working perfectly!");
      delay(400);
      simSerial.write(26);
    } else if (cmd.equalsIgnoreCase("CALL")) {
      Serial.println("\n[CALL] Dials test voice call to " + String(TARGET_PHONE) + "...");
      simSerial.printf("ATD%s;\r\n", TARGET_PHONE);
      delay(15000);
      simSerial.println("ATH");
    } else if (cmd.length() > 0) {
      sendAT(cmd, 2500);
    }
  }
  while (simSerial.available()) {
    Serial.write(simSerial.read());
  }
}
