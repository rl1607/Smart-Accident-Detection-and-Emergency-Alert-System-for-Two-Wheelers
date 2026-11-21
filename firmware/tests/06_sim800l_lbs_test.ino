/*
 * TEST SKETCH 06: SIM800L Cellular / LBS Fallback Location Test
 * 
 * IMPORTANT ARCHITECTURE NOTE:
 * SIM800L DOES NOT CONTAIN A TRUE GPS RECEIVER.
 * It uses GSM base station tower IDs to look up an approximate Cellular/LBS position.
 * This is used ONLY as a fallback when the primary NEO-6M GPS has no satellite fix.
 */

#include <HardwareSerial.h>

#define SIM_RX 27
#define SIM_TX 26

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
  Serial.println("\n--- SIM800L CELLULAR / LBS LOCATION TEST ---");

  simSerial.begin(9600, SERIAL_8N1, SIM_RX, SIM_TX);
  delay(3000);

  sendAT("AT");
  sendAT("AT+CSQ");
  sendAT("AT+CREG?");
  sendAT("AT+CGATT=1");

  // Open GPRS Bearer for Location Services
  sendAT("AT+SAPBR=3,1,\"Contype\",\"GPRS\"");
  sendAT("AT+SAPBR=3,1,\"APN\",\"internet\""); // Adjust APN according to your SIM carrier
  sendAT("AT+SAPBR=1,1", 5000);
  sendAT("AT+SAPBR=2,1");

  // Request LBS Location from GSM Network
  Serial.println("\nRequesting GSM Base Station LBS location...");
  String lbsResp = sendAT("AT+CIPGSMLOC=1,1", 8000);

  // Format: +CIPGSMLOC: 0,longitude,latitude,date,time
  if (lbsResp.indexOf("+CIPGSMLOC: 0,") != -1) {
    Serial.println("\n[SUCCESS] Approximate Cellular/LBS location acquired.");
    Serial.println("Source: Cellular/LBS (GSM Tower Triangulation)");
  } else {
    Serial.println("\n[NOTICE] LBS lookup failed or unsupported by current SIM carrier. NEO-6M GPS will serve as primary.");
  }
}

void loop() {
  delay(1000);
}
