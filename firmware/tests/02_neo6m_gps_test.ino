/*
 * TEST SKETCH 02: NEO-6M GPS Module Test
 * 
 * Pins:
 * NEO-6M TX -> ESP32 GPIO 16 (ESP32 RX)
 * NEO-6M RX -> ESP32 GPIO 17 (ESP32 TX)
 * NEO-6M VCC -> ESP32 3.3V / 5V
 * NEO-6M GND -> ESP32 GND
 * 
 * Note: NEO-6M needs a clear view of the open sky to acquire a satellite fix.
 * Indoor fix acquisition can take several minutes.
 */

#include <HardwareSerial.h>
#include <TinyGPS++.h>

#define GPS_RX 16
#define GPS_TX 17

HardwareSerial gpsSerial(2);
TinyGPSPlus gps;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- NEO-6M GPS DIAGNOSTIC TEST ---");

  gpsSerial.begin(9600, SERIAL_8N1, GPS_RX, GPS_TX);
  Serial.println("Listening for NMEA sentences on UART2 (GPIO 16/17)...");
}

void loop() {
  while (gpsSerial.available() > 0) {
    char c = gpsSerial.read();
    gps.encode(c);
  }

  static unsigned long lastPrint = 0;
  if (millis() - lastPrint >= 2000) {
    lastPrint = millis();

    if (gps.location.isValid()) {
      Serial.printf("[GPS FIX ACTIVE]\n");
      Serial.printf("  Latitude:  %.6f°\n", gps.location.lat());
      Serial.printf("  Longitude: %.6f°\n", gps.location.lng());
      Serial.printf("  Altitude:  %.1f meters\n", gps.altitude.meters());
      Serial.printf("  Speed:     %.1f km/h\n", gps.speed.kmph());
      Serial.printf("  Course:    %.1f°\n", gps.course.deg());
      Serial.printf("  Sats:      %d\n", gps.satellites.value());
      Serial.printf("  HDOP:      %.2f\n", gps.hdop.hdop());
    } else {
      Serial.printf("[GPS SEARCHING] Sats in view: %d | Total chars parsed: %lu | Wait for outdoor sky view...\n",
                    gps.satellites.value(), gps.charsProcessed());
    }
  }
}
