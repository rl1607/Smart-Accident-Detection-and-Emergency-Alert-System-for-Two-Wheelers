/*
 * TEST SKETCH 01: MPU6050 Accelerometer & Gyroscope Test
 * 
 * Pins:
 * MPU6050 SDA -> ESP32 GPIO 21
 * MPU6050 SCL -> ESP32 GPIO 22
 * MPU6050 VCC -> ESP32 3.3V
 * MPU6050 GND -> ESP32 GND
 * 
 * Thresholds:
 * Impact: ~4.0g
 * Tilt: >= 45.0 degrees
 */

#include <Wire.h>

#define MPU_SDA 21
#define MPU_SCL 22
#define MPU_ADDR 0x68

#define IMPACT_THRESHOLD 4.0
#define TILT_THRESHOLD 45.0

int16_t ax, ay, az;
int16_t gx, gy, gz;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- MPU6050 DIAGNOSTIC TEST ---");

  Wire.begin(MPU_SDA, MPU_SCL);

  // Wake up MPU6050
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x6B); // PWR_MGMT_1
  Wire.write(0x00); // Wake up
  byte error = Wire.endTransmission();

  if (error == 0) {
    Serial.println("MPU6050 initialized successfully at address 0x68");
  } else {
    Serial.printf("Failed to connect to MPU6050. Error code: %d. Check SDA(21)/SCL(22) wiring.\n", error);
    while (1) delay(1000);
  }
}

void loop() {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x3B); // Register 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(false);
  Wire.requestFrom((uint8_t)MPU_ADDR, (size_t)14, true);

  if (Wire.available() >= 14) {
    ax = Wire.read() << 8 | Wire.read();
    ay = Wire.read() << 8 | Wire.read();
    az = Wire.read() << 8 | Wire.read();
    Wire.read(); Wire.read(); // Skip temp
    gx = Wire.read() << 8 | Wire.read();
    gy = Wire.read() << 8 | Wire.read();
    gz = Wire.read() << 8 | Wire.read();

    // Convert raw to Gs (+/- 2g default sensitivity: 16384 LSB/g)
    float gX = ax / 16384.0;
    float gY = ay / 16384.0;
    float gZ = az / 16384.0;

    // Total Acceleration Vector
    float totalAcc = sqrt(gX * gX + gY * gY + gZ * gZ);

    // Tilt Angles in degrees (relative to vertical)
    float tiltAngleX = abs(atan2(gY, sqrt(gX * gX + gZ * gZ)) * 180.0 / PI);
    float tiltAngleY = abs(atan2(gX, sqrt(gY * gY + gZ * gZ)) * 180.0 / PI);

    Serial.printf("Acc: [%.2f, %.2f, %.2f] G | Mag: %.2f G | Tilt: X=%.1f°, Y=%.1f°\n",
                  gX, gY, gZ, totalAcc, tiltAngleX, tiltAngleY);

    if (totalAcc >= IMPACT_THRESHOLD) {
      Serial.printf(">>> [ALERT] IMPACT THRESHOLD EXCEEDED! (%.2f g >= 4.0 g)\n", totalAcc);
    }

    if (tiltAngleX >= TILT_THRESHOLD || tiltAngleY >= TILT_THRESHOLD) {
      Serial.printf(">>> [ALERT] VEHICLE TILT THRESHOLD EXCEEDED! (>= 45.0°)\n");
    }
  }

  delay(200);
}
