# 🚨 Smart Accident Detection & Emergency Alert System

[![Platform: ESP32](https://img.shields.io/badge/Platform-ESP32-blue.svg)](https://www.espressif.com/)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-green.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Supabase%20PostgreSQL-3ECF8E.svg)](https://supabase.com/)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg)](https://kotlinlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An intelligent, IoT-enabled motorcycle and vehicle safety system designed to automatically detect collisions and falls, prevent false alarms through an interactive countdown, and immediately alert emergency contacts with real-time GPS coordinates via SMS, automated voice calls, and a companion Android application connected to a live cloud database.

---

## 📌 Table of Contents
- [System Architecture](#-system-architecture)
- [Key Features](#-key-features)
- [Hardware Components & Pinout](#-hardware-components--pinout)
- [Repository Structure](#-repository-structure)
- [Prerequisites & Libraries](#-prerequisites--libraries)
- [Setup & Installation Guide](#-setup--installation-guide)
  - [1. ESP32 Firmware Setup](#1-esp32-firmware-setup)
  - [2. Cloud Database (Supabase) Setup](#2-cloud-database-supabase-setup)
  - [3. Android Mobile Application Setup](#3-android-mobile-application-setup)
  - [4. Optional Node.js Gateway](#4-optional-nodejs-gateway)
- [Security & Environment Variables](#-security--environment-variables)
- [How to Upload / Push to GitHub](#-how-to-upload--push-to-github)
- [License](#-license)

---

## 🏗 System Architecture

```
                    +------------------------+
                    |  Vehicle Sensors / IMU |
                    |  MPU-6050 & NEO-6M GPS |
                    +-----------+------------+
                                |
                                v
                    +------------------------+
                    |    ESP32 Controller    |
                    | (Detection & Decision) |
                    +----+--------------+----+
                         |              |
             [45° Tilt / Impact]   [15s Countdown]
                         |              | (Cancel via Button)
                         v              v
        +----------------+--+      +----+---------------+
        | SIM800L GSM/GPRS  |      |   Wi-Fi / GPRS     |
        +---+------------+--+      +---------+----------+
            |            |                   |
            v            v                   v
      [Emergency   [Emergency        [Supabase Cloud]
         SMS]         Call]                  |
                                             v
                                  +---------------------+
                                  | Android Mobile App  |
                                  | (Live Map & Alerts) |
                                  +---------------------+
```

---

## ✨ Key Features

1. **Precision Fall & Impact Detection**:
   - **Vector Spherical Tilt Detection**: Continuously computes roll and pitch angles; triggers when the vehicle sustains a tilt greater than **45°** for more than 400ms.
   - **G-Force Impact Detection**: Detects severe kinetic deceleration/impacts exceeding **5.0g**.
2. **False Alarm Cancellation Mechanism**:
   - High-pitch buzzer alert with a **15-second countdown** upon detection.
   - Push button on **GPIO 33** allows the rider to cancel the alert if uninjured, dispatching a "Rider Safe / False Alarm" status to the cloud.
3. **Emergency Cellular Alerts (SIM800L)**:
   - **Automated SMS**: Dispatches an emergency SMS containing exact Google Maps coordinates (`https://maps.google.com/?q=lat,lon`), impact force, and tilt angle.
   - **Automated Voice Call**: Initiates a phone call to the primary emergency contact.
4. **Dual-Path Cloud Telemetry**:
   - Supports 2G/GPRS cellular data and Wi-Fi to synchronize 10-second live telemetry (`locations`) and collision reports (`accidents`) to Supabase.
5. **Modern Android Companion App**:
   - Built with **Jetpack Compose** and **Material Design 3**.
   - Live Google Maps real-time tracking and route polyline display.
   - Incident history logs, device status monitoring, and cloud-synced emergency contacts.

---

## 🔌 Hardware Components & Pinout

### Required Components:
- **ESP32 Dev Module** (Dual-Core 240MHz, 4MB Flash)
- **MPU-6050** 6-Axis Accelerometer & Gyroscope
- **NEO-6M GPS Module** with Ceramic Antenna
- **SIM800L 2G GSM/GPRS Module** (with external 3.7V–4.2V Li-ion battery or 2A buck converter)
- **Active Buzzer (5V / 3.3V)**
- **Momentary Push Button** (Normally Open)
- **Common Ground** shared across all modules

### Pin Connection Table:

| Component | Component Pin | ESP32 GPIO | Description / Notes |
|---|---|---|---|
| **MPU-6050** | VCC | 3.3V / 5V | Power supply |
| | GND | GND | Common Ground |
| | SDA | **GPIO 21** | I2C Data line |
| | SCL | **GPIO 22** | I2C Clock line |
| **NEO-6M GPS** | VCC | 3.3V / 5V | Power supply |
| | GND | GND | Common Ground |
| | TX | **GPIO 16** (RX2) | Hardware UART2 Receive |
| | RX | **GPIO 17** (TX2) | Hardware UART2 Transmit |
| **SIM800L GSM** | VCC | **3.7V - 4.2V** | **External Power (Peak 2A required)** |
| | GND | GND | **Common Ground with ESP32** |
| | TX | **GPIO 18** (RX1) | Hardware UART1 Receive |
| | RX | **GPIO 19** (TX1) | Hardware UART1 Transmit |
| **Active Buzzer** | Positive (+) | **GPIO 25** | Trigger pin |
| | Negative (-) | GND | Ground |
| **Push Button** | Terminal 1 | **GPIO 33** | Internal Pull-Up (`INPUT_PULLUP`) |
| | Terminal 2 | GND | Connects to GND on press |

---

## 📁 Repository Structure

```
SmartAccidentDetection/
├── app/                                 # Android Mobile App
│   ├── src/main/java/com/example/...    # Kotlin source code (Compose, Retrofit, Repositories)
│   ├── src/main/res/                    # Android layouts, drawables, strings
│   ├── AndroidManifest.xml              # Permissions and Map API configurations
│   └── build.gradle.kts                 # App-level dependencies & build rules
├── backend/                             # Node.js Cloud Gateway (Optional)
│   ├── server.js                        # Express HTTP server
│   ├── supabaseClient.js                # Supabase REST client
│   └── package.json                     # Node dependencies
├── firmware/                            # Embedded C++ Firmware
│   ├── smart_accident_firmware/
│   │   └── smart_accident_firmware.ino  # Main ESP32 production firmware
│   └── tests/                           # Unit tests for MPU6050, GPS, SIM800L, Buzzer
├── gradle/                              # Gradle build wrapper
├── .gitignore                           # Git ignore rules for build artifacts & secrets
├── build.gradle.kts                     # Project-level Gradle build configuration
├── local.properties.example             # Template for Android SDK and Maps API Key
└── README.md                            # Comprehensive project documentation
```

---

## 📦 Prerequisites & Libraries

### For Arduino IDE (Firmware):
- **ESP32 Board Support Package** (by Espressif Systems v2.0.x or v3.0.x)
- **TinyGPSPlus** (by Mikal Hart)
- Built-in libraries: `Wire.h`, `WiFi.h`, `HTTPClient.h`, `WiFiClientSecure.h`, `Preferences.h`

### For Android Studio:
- **Android Studio Ladybug (2024.2+)** or Hedgehog+
- **JDK 11** or **JDK 17**
- **Android SDK API 34** (minimum API 24)
- Google Maps API Key from [Google Cloud Console](https://console.cloud.google.com/)

---

## 🚀 Setup & Installation Guide

### 1. ESP32 Firmware Setup
1. Open Arduino IDE and install the **ESP32 by Espressif Systems** board package.
2. Install **TinyGPSPlus** via Library Manager (`Sketch` -> `Include Library` -> `Manage Libraries...`).
3. Open `firmware/smart_accident_firmware/smart_accident_firmware.ino`.
4. Configure your credentials at the top of the file:
   ```cpp
   #define ENABLE_WIFI true              // Set to true for Wi-Fi or false for 100% SIM800 GPRS
   const char* WIFI_SSID = "YOUR_WIFI_SSID";
   const char* WIFI_PASS = "YOUR_WIFI_PASSWORD";
   const char* DEFAULT_PHONE = "+91XXXXXXXXXX"; // Your emergency mobile number
   const char* SUPABASE_URL = "https://your-project.supabase.co";
   const char* SUPABASE_KEY = "your-supabase-api-key";
   ```
5. Select **ESP32 Dev Module** under `Tools` -> `Board`.
6. Select your USB COM Port and click **Upload**.

---

### 2. Cloud Database (Supabase) Setup
Create the following PostgreSQL tables in your Supabase project:

```sql
-- 1. Locations Table (Live GPS Tracking)
CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed_kmh REAL DEFAULT 0.0,
    source TEXT DEFAULT 'GPS',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Accidents Table (Incident Alerts)
CREATE TABLE accidents (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    impact_g REAL DEFAULT 0.0,
    tilt_angle REAL DEFAULT 0.0,
    status TEXT DEFAULT 'CONFIRMED',
    source TEXT DEFAULT 'GPS',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Emergency Contacts Table
CREATE TABLE emergency_contacts (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    relationship TEXT,
    is_primary BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

### 3. Android Mobile Application Setup
1. Open the project folder in **Android Studio**.
2. Copy `local.properties.example` to `local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=AIzaSyYourActualGoogleMapsApiKeyHere
   ```
3. Update `SupabaseClient.kt` in `app/src/main/java/com/example/smartaccidentdetection/data/remote/`:
   ```kotlin
   private const val SUPABASE_BASE_URL = "https://your-project.supabase.co/rest/v1/"
   private const val SUPABASE_API_KEY = "your-supabase-api-key"
   ```
4. Click **Sync Project with Gradle Files**.
5. Connect your Android device or start an emulator and click **Run 'app'**.

---

## 🔒 Security & Environment Variables

> [!WARNING]
> **Before pushing to a public GitHub repository**, make sure you do NOT publish private credentials:
> 1. **Do not commit personal phone numbers or Wi-Fi passwords** in `smart_accident_firmware.ino`. Replace them with placeholders (e.g. `"YOUR_WIFI_PASSWORD"`).
> 2. **Never commit `local.properties`** or `backend/.env` (both are already protected in `.gitignore`).
> 3. Use restricted Supabase Anon keys rather than full admin service role keys in mobile and embedded clients.

---

## 📤 How to Upload / Push to GitHub

Follow these steps from your terminal inside the project folder:

```bash
# 1. Navigate to the project root directory
cd c:\Users\rloka\AndroidStudioProjects\SmartAccidentDetection

# 2. Initialize Git inside this project folder
git init

# 3. Verify .gitignore is active and check files to be committed
git status

# 4. Stage all project files
git add .

# 5. Commit the files
git commit -m "Initial commit: Smart Accident Detection System (Firmware + Android App + Backend)"

# 6. Set default branch to main
git branch -M main

# 7. Add your GitHub repository remote
git remote add origin https://github.com/<your-username>/<your-repository-name>.git

# 8. Push to GitHub
git push -u origin main
```

---

## 📄 License
This project is open-source and licensed under the [MIT License](LICENSE).
