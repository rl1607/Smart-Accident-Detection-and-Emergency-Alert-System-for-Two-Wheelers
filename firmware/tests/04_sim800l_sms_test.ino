/*
 * TEST SKETCH 04: SIM800L SMS Transmission Test
 * 
 * Instructions:
 * Replace TARGET_PHONE_NUMBER with your emergency mobile number (including +country code, no spaces).
 */

#include <HardwareSerial.h>

#define SIM800_RX_PIN 18
#define SIM800_TX_PIN 19

// REPLACE with your test emergency phone number
const char* TARGET_PHONE_NUMBER = "+919876543210";

HardwareSerial simSerial(1);

String sendAT(String cmd, unsigned long timeout = 3000) {
  String response = "";
  Serial.println(">> " + cmd);
  simSerial.println(cmd);
  unsigned long start = millis();
  while (millis() - start < timeout) {
    while (simSerial.available()) {
      char c = simSerial.read();
      response += c;
    }
  }
  Serial.println("<< " + response);
  return response;
}

bool sendAccidentSMS(const char* phone, float lat, float lon, const char* source) {
  Serial.printf("\nAttempting to send SMS to %s...\n", phone);

  sendAT("AT+CMGF=1", 1000); // Text mode
  delay(500);

  simSerial.printf("AT+CMGS=\"%s\"\r\n", phone);
  delay(1000);

  // Compose SMS body
  simSerial.print("ACCIDENT ALERT!\r\n\r\n");
  simSerial.print("Possible accident detected by vehicle safety system.\r\n\r\n");
  simSerial.printf("Location:\r\nhttps://maps.google.com/?q=%.6f,%.6f\r\n\r\n", lat, lon);
  simSerial.printf("Location source:\r\n%s\r\n", source);
  simSerial.print("Impact: 4.25g | Tilt: 49.0 deg\r\n");

  delay(500);
  simSerial.write(26); // Ctrl+Z to submit SMS

  // Wait for confirmation
  unsigned long start = millis();
  String resp = "";
  while (millis() - start < 15000) {
    while (simSerial.available()) {
      resp += (char)simSerial.read();
    }
    if (resp.indexOf("+CMGS:") != -1 && resp.indexOf("OK") != -1) {
      Serial.println("\n[SUCCESS] SMS successfully sent by SIM800L!");
      return true;
    }
    if (resp.indexOf("ERROR") != -1) {
      Serial.println("\n[FAILED] SIM800L returned ERROR when sending SMS.");
      return false;
    }
  }

  Serial.println("\n[TIMEOUT] SMS dispatch timed out.");
  return false;
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- SIM800L SMS TEST ---");

  simSerial.begin(9600, SERIAL_8N1, SIM800_RX_PIN, SIM800_TX_PIN);
  delay(3000);

  sendAT("AT");
  sendAT("AT+CSQ");
  sendAT("AT+CREG?");

  // Send a test accident SMS
  sendAccidentSMS(TARGET_PHONE_NUMBER, 17.4399, 78.3908, "GPS");
}

void loop() {
  delay(1000);
}
