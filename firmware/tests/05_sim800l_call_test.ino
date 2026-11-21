/*
 * TEST SKETCH 05: SIM800L Emergency Voice Call Test
 * 
 * Instructions:
 * Replace TARGET_PHONE_NUMBER with your emergency contact phone number.
 */

#include <HardwareSerial.h>

#define SIM_RX 27
#define SIM_TX 26

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

void makeEmergencyCall(const char* phone, int durationSeconds = 25) {
  Serial.printf("\n[DIALING] Calling emergency number: %s\n", phone);
  
  // Initiate call with semicolon at end
  String dialCmd = "ATD" + String(phone) + ";";
  sendAT(dialCmd, 3000);

  Serial.printf("Call active. Keeping line open for %d seconds...\n", durationSeconds);
  for (int i = durationSeconds; i > 0; i--) {
    Serial.printf("Calling... %d s remaining\n", i);
    delay(1000);
  }

  Serial.println("[HANGING UP] Terminating voice call...");
  sendAT("ATH", 2000);
  Serial.println("[COMPLETED] Call ended.");
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- SIM800L VOICE CALL TEST ---");

  simSerial.begin(9600, SERIAL_8N1, SIM_RX, SIM_TX);
  delay(3000);

  sendAT("AT");
  sendAT("AT+CSQ");
  sendAT("AT+CREG?");

  makeEmergencyCall(TARGET_PHONE_NUMBER, 25);
}

void loop() {
  delay(1000);
}
