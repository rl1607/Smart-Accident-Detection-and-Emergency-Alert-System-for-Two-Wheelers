/*
 * TEST SKETCH 07: SIM800L GPRS HTTP Communication Test
 * 
 * Verifies AT+HTTPINIT, AT+HTTPPARA, AT+HTTPACTION, and AT+HTTPREAD
 */

#include <HardwareSerial.h>

#define SIM_RX 27
#define SIM_TX 26

// Set to your carrier's APN (e.g., "airtelgprs.com", "jionet", "www", "internet")
const char* APN = "internet";
const char* HEALTH_URL = "http://smart-accident-detection-system.onrender.com/";

HardwareSerial simSerial(1);

String sendAT(String cmd, unsigned long timeout = 4000) {
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

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- SIM800L GPRS HTTP TEST ---");

  simSerial.begin(9600, SERIAL_8N1, SIM_RX, SIM_TX);
  delay(3000);

  sendAT("AT");
  sendAT("AT+CSQ");
  sendAT("AT+CREG?");
  sendAT("AT+CGATT=1");

  // Setup GPRS Bearer
  sendAT("AT+SAPBR=3,1,\"Contype\",\"GPRS\"");
  sendAT(String("AT+SAPBR=3,1,\"APN\",\"") + APN + "\"");
  sendAT("AT+SAPBR=1,1", 6000);
  sendAT("AT+SAPBR=2,1");

  // HTTP GET Health Check
  sendAT("AT+HTTPTERM"); // Reset previous session if open
  sendAT("AT+HTTPINIT");
  sendAT("AT+HTTPPARA=\"CID\",1");
  sendAT(String("AT+HTTPPARA=\"URL\",\"") + HEALTH_URL + "\"");
  sendAT("AT+HTTPACTION=0", 8000); // 0 = GET
  sendAT("AT+HTTPREAD", 3000);
  sendAT("AT+HTTPTERM");

  Serial.println("\n[COMPLETED] GPRS HTTP Test finished.");
}

void loop() {
  delay(1000);
}
