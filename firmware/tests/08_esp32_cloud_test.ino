/*
 * TEST SKETCH 08: ESP32 Cloud API Telemetry Ingestion Test
 * 
 * Demonstrates sending JSON GPS location payload to backend API endpoint.
 */

#include <HardwareSerial.h>

#define SIM_RX 27
#define SIM_TX 26

const char* APN = "internet";
const char* API_POST_URL = "http://smart-accident-detection-system.onrender.com/api/device/ACCIDENT-001/location";

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

bool postLocationToCloud(float lat, float lon, float speed, const char* source) {
  String jsonBody = "{\"deviceId\":\"ACCIDENT-001\",\"latitude\":" + String(lat, 6) +
                    ",\"longitude\":" + String(lon, 6) +
                    ",\"speedKmh\":" + String(speed, 1) +
                    ",\"locationSource\":\"" + String(source) + "\"}";

  Serial.println("\nPosting Telemetry JSON to Cloud:");
  Serial.println(jsonBody);

  sendAT("AT+HTTPINIT");
  sendAT("AT+HTTPPARA=\"CID\",1");
  sendAT(String("AT+HTTPPARA=\"URL\",\"") + API_POST_URL + "\"");
  sendAT("AT+HTTPPARA=\"CONTENT\",\"application/json\"");

  // Send body size and content
  sendAT("AT+HTTPDATA=" + String(jsonBody.length()) + ",10000");
  delay(500);
  simSerial.print(jsonBody);
  delay(1000);

  // Execute POST (1 = POST)
  String actionResp = sendAT("AT+HTTPACTION=1", 10000);
  sendAT("AT+HTTPREAD", 3000);
  sendAT("AT+HTTPTERM");

  return (actionResp.indexOf("200") != -1 || actionResp.indexOf("201") != -1);
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- ESP32 CLOUD TELEMETRY TEST ---");

  simSerial.begin(9600, SERIAL_8N1, SIM_RX, SIM_TX);
  delay(3000);

  sendAT("AT");
  sendAT("AT+CSQ");
  sendAT("AT+CREG?");
  sendAT("AT+CGATT=1");

  // Bearer
  sendAT("AT+SAPBR=3,1,\"Contype\",\"GPRS\"");
  sendAT(String("AT+SAPBR=3,1,\"APN\",\"") + APN + "\"");
  sendAT("AT+SAPBR=1,1", 6000);

  // Upload test location frame
  postLocationToCloud(17.4399, 78.3908, 38.5, "GPS");
}

void loop() {
  delay(1000);
}
