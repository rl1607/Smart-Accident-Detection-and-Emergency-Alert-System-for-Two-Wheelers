const fs = require('fs');
const path = require('path');

const DB_FILE = path.join(__dirname, 'data_store.json');

// Default initial state
const defaultState = {
  users: [
    {
      id: "USER_001",
      fullName: "Rahul Sharma",
      phoneNumber: "+919876543210",
      email: "rahul.sharma@example.com",
      vehicleNumber: "TS 09 EA 4589",
      vehicleModel: "Yamaha MT-15 (Two-Wheeler)",
      bloodGroup: "B+ve",
      medicalNotes: "No known chronic conditions. Wears spectacles.",
      createdAt: new Date().toISOString()
    }
  ],
  emergencyContacts: [
    {
      id: "CNT_01",
      userId: "USER_001",
      deviceId: "ESP32_VEHICLE_01",
      name: "Anjali Sharma",
      phoneNumber: "+919876543211",
      relationship: "Spouse",
      isPrimary: true,
      syncState: "Synced with ESP32"
    },
    {
      id: "CNT_02",
      userId: "USER_001",
      deviceId: "ESP32_VEHICLE_01",
      name: "Vikram Sharma",
      phoneNumber: "+919876543212",
      relationship: "Brother",
      isPrimary: false,
      syncState: "Synced with ESP32"
    }
  ],
  devices: {
    "ESP32_VEHICLE_01": {
      deviceId: "ESP32_VEHICLE_01",
      userId: "USER_001",
      isOnline: true,
      batteryPercent: 88,
      gsmSignalDbm: -68,
      gsmNetworkStatus: "Registered (4G / 2G GPRS Connected)",
      gpsFix: true,
      satellites: 9,
      mpuStatus: "Active (Calibrated: 45° Tilt, 4.0g Impact)",
      lastSeen: "Just now",
      locationSource: "GPS",
      emergencySyncStatus: "Synced",
      lastSyncTimestamp: "Today, 09:15 AM",
      currentSpeedKmh: 42.5,
      distanceTravelledKm: 18.3,
      accidentState: "SAFE"
    }
  },
  locations: [
    { id: "LOC_01", deviceId: "ESP32_VEHICLE_01", latitude: 17.4399, longitude: 78.3908, speedKmh: 0.0, altitudeM: 512.0, bearing: 45, satellites: 8, locationSource: "GPS", timestamp: Date.now() - 600000 },
    { id: "LOC_02", deviceId: "ESP32_VEHICLE_01", latitude: 17.4415, longitude: 78.3930, speedKmh: 24.0, altitudeM: 514.0, bearing: 50, satellites: 8, locationSource: "GPS", timestamp: Date.now() - 540000 },
    { id: "LOC_03", deviceId: "ESP32_VEHICLE_01", latitude: 17.4438, longitude: 78.3965, speedKmh: 38.5, altitudeM: 515.0, bearing: 55, satellites: 9, locationSource: "GPS", timestamp: Date.now() - 480000 },
    { id: "LOC_04", deviceId: "ESP32_VEHICLE_01", latitude: 17.4460, longitude: 78.4002, speedKmh: 45.0, altitudeM: 518.0, bearing: 60, satellites: 9, locationSource: "GPS", timestamp: Date.now() - 420000 },
    { id: "LOC_05", deviceId: "ESP32_VEHICLE_01", latitude: 17.4485, longitude: 78.4045, speedKmh: 48.2, altitudeM: 520.0, bearing: 65, satellites: 9, locationSource: "GPS", timestamp: Date.now() - 360000 },
    { id: "LOC_06", deviceId: "ESP32_VEHICLE_01", latitude: 17.4510, longitude: 78.4080, speedKmh: 41.0, altitudeM: 521.0, bearing: 70, satellites: 9, locationSource: "GPS", timestamp: Date.now() - 300000 },
    { id: "LOC_07", deviceId: "ESP32_VEHICLE_01", latitude: 17.4535, longitude: 78.4118, speedKmh: 35.5, altitudeM: 522.0, bearing: 72, satellites: 9, locationSource: "GPS", timestamp: Date.now() - 240000 },
    { id: "LOC_08", deviceId: "ESP32_VEHICLE_01", latitude: 17.4558, longitude: 78.4150, speedKmh: 42.0, altitudeM: 525.0, bearing: 75, satellites: 9, locationSource: "GPS", timestamp: Date.now() - 180000 },
    { id: "LOC_09", deviceId: "ESP32_VEHICLE_01", latitude: 17.4578, longitude: 78.4185, speedKmh: 36.0, altitudeM: 526.0, bearing: 78, satellites: 9, locationSource: "GPS", timestamp: Date.now() - 120000 },
    { id: "LOC_10", deviceId: "ESP32_VEHICLE_01", latitude: 17.4600, longitude: 78.4215, speedKmh: 29.5, altitudeM: 528.0, bearing: 80, satellites: 9, locationSource: "GPS", timestamp: Date.now() - 60000 },
    { id: "LOC_11", deviceId: "ESP32_VEHICLE_01", latitude: 17.4618, longitude: 78.4242, speedKmh: 34.5, altitudeM: 530.0, bearing: 85, satellites: 9, locationSource: "GPS", timestamp: Date.now() }
  ],
  accidents: [
    {
      id: "ALT_101",
      deviceId: "ESP32_VEHICLE_01",
      timestamp: Date.now() - (1000 * 60 * 60 * 24 * 2),
      latitude: 17.4375,
      longitude: 78.4482,
      locationName: "Road No. 36, Jubilee Hills, Hyderabad",
      impactG: 4.35,
      tiltAngle: 52.0,
      detectionType: "High Impact (4.35g) + Tilt (52°)",
      status: "CONFIRMED",
      smsStatus: "Sent to +919876543211",
      callStatus: "Completed (25s)",
      locationSource: "GPS"
    },
    {
      id: "ALT_102",
      deviceId: "ESP32_VEHICLE_01",
      timestamp: Date.now() - (1000 * 60 * 60 * 24 * 5),
      latitude: 17.4485,
      longitude: 78.3741,
      locationName: "Mindspace Junction, HITEC City",
      impactG: 4.10,
      tiltAngle: 48.5,
      detectionType: "Tilt Threshold Exceeded (48.5°)",
      status: "CANCELLED_BY_RIDER",
      smsStatus: "Cancelled (Within 15s)",
      callStatus: "Cancelled",
      locationSource: "GPS"
    }
  ],
  trips: [
    {
      id: "TRIP_01",
      deviceId: "ESP32_VEHICLE_01",
      date: "Today",
      startTime: "08:30 AM",
      endTime: "09:12 AM",
      distanceKm: 18.3,
      durationMinutes: 42,
      avgSpeedKmh: 26.1,
      maxSpeedKmh: 58.0,
      startLocationName: "Kondapur, Hyderabad",
      endLocationName: "Banjara Hills Road 12"
    },
    {
      id: "TRIP_02",
      deviceId: "ESP32_VEHICLE_01",
      date: "Yesterday",
      startTime: "06:15 PM",
      endTime: "07:05 PM",
      distanceKm: 21.7,
      durationMinutes: 50,
      avgSpeedKmh: 28.4,
      maxSpeedKmh: 62.5,
      startLocationName: "Gachibowli Financial District",
      endLocationName: "Madhapur Metro"
    }
  ]
};

class Database {
  constructor() {
    this.data = this.load();
  }

  load() {
    try {
      if (fs.existsSync(DB_FILE)) {
        const raw = fs.readFileSync(DB_FILE, 'utf-8');
        return JSON.parse(raw);
      }
    } catch (e) {
      console.error('Error loading DB file, reinitializing default:', e.message);
    }
    this.save(defaultState);
    return JSON.parse(JSON.stringify(defaultState));
  }

  save(customData = null) {
    try {
      const toWrite = customData || this.data;
      fs.writeFileSync(DB_FILE, JSON.stringify(toWrite, null, 2), 'utf-8');
    } catch (e) {
      console.error('Failed to write database file:', e.message);
    }
  }

  // Device methods
  getDevice(deviceId) {
    if (!this.data.devices[deviceId]) {
      this.data.devices[deviceId] = {
        deviceId,
        isOnline: true,
        batteryPercent: 100,
        gsmSignalDbm: -70,
        gsmNetworkStatus: "Registered",
        gpsFix: true,
        satellites: 8,
        mpuStatus: "Active",
        lastSeen: new Date().toISOString(),
        locationSource: "GPS",
        emergencySyncStatus: "Synced",
        lastSyncTimestamp: new Date().toISOString(),
        currentSpeedKmh: 0,
        distanceTravelledKm: 0,
        accidentState: "SAFE"
      };
      this.save();
    }
    return this.data.devices[deviceId];
  }

  updateDevice(deviceId, updates) {
    const dev = this.getDevice(deviceId);
    this.data.devices[deviceId] = { ...dev, ...updates, lastSeen: new Date().toISOString() };
    this.save();
    return this.data.devices[deviceId];
  }

  // Location methods
  addLocation(deviceId, loc) {
    const point = {
      id: "LOC_" + Date.now().toString(36).toUpperCase(),
      deviceId,
      latitude: parseFloat(loc.latitude) || 0.0,
      longitude: parseFloat(loc.longitude) || 0.0,
      speedKmh: parseFloat(loc.speedKmh || loc.speed) || 0.0,
      altitudeM: parseFloat(loc.altitudeM || loc.altitude) || 0.0,
      bearing: parseFloat(loc.bearing) || 0.0,
      satellites: parseInt(loc.satellites) || 8,
      locationSource: loc.locationSource || (loc.satellites > 0 ? "GPS" : "Cellular/LBS"),
      timestamp: loc.timestamp || Date.now()
    };

    this.data.locations.push(point);
    if (this.data.locations.length > 500) {
      this.data.locations.shift();
    }

    this.updateDevice(deviceId, {
      lastLatitude: point.latitude,
      lastLongitude: point.longitude,
      currentSpeedKmh: point.speedKmh,
      locationSource: point.locationSource,
      satellites: point.satellites,
      gpsFix: point.locationSource === "GPS"
    });

    this.save();
    return point;
  }

  getLocations(deviceId, limit = 100) {
    return this.data.locations
      .filter(l => l.deviceId === deviceId)
      .slice(-limit);
  }

  getLatestLocation(deviceId) {
    const locs = this.getLocations(deviceId, 1);
    return locs.length > 0 ? locs[0] : null;
  }

  // Accident methods
  addAccident(deviceId, acc) {
    const record = {
      id: "ALT_" + Date.now().toString(36).toUpperCase(),
      deviceId,
      timestamp: acc.timestamp || Date.now(),
      latitude: parseFloat(acc.latitude) || 0.0,
      longitude: parseFloat(acc.longitude) || 0.0,
      locationName: acc.locationName || `Coordinates: ${acc.latitude}, ${acc.longitude}`,
      impactG: parseFloat(acc.impactG) || 4.0,
      tiltAngle: parseFloat(acc.tiltAngle) || 45.0,
      detectionType: acc.detectionType || "Impact (>=4.0g) or Tilt (>=45°)",
      status: acc.status || "CONFIRMED",
      smsStatus: acc.smsStatus || "Sent",
      callStatus: acc.callStatus || "Initiated",
      locationSource: acc.locationSource || "GPS"
    };

    this.data.accidents.unshift(record);
    this.updateDevice(deviceId, { accidentState: record.status === "CONFIRMED" ? "CONFIRMED_ACCIDENT" : "CANCELLED" });
    this.save();
    return record;
  }

  getAccidents(deviceId) {
    return this.data.accidents.filter(a => a.deviceId === deviceId);
  }

  // Emergency Contacts
  getContacts(deviceId) {
    return this.data.emergencyContacts;
  }

  setContacts(deviceId, contactsList) {
    this.data.emergencyContacts = contactsList.map(c => ({
      ...c,
      deviceId,
      syncState: "Synced with ESP32"
    }));
    this.updateDevice(deviceId, {
      emergencySyncStatus: "Synced",
      lastSyncTimestamp: new Date().toISOString()
    });
    this.save();
    return this.data.emergencyContacts;
  }

  // Trips
  getTrips(deviceId) {
    return this.data.trips.filter(t => t.deviceId === deviceId);
  }

  // User
  getUser(userId) {
    return this.data.users.find(u => u.id === userId) || this.data.users[0];
  }

  updateUser(userId, updates) {
    const idx = this.data.users.findIndex(u => u.id === userId);
    if (idx !== -1) {
      this.data.users[idx] = { ...this.data.users[idx], ...updates };
    } else {
      this.data.users.push({ id: userId, ...updates, createdAt: new Date().toISOString() });
    }
    this.save();
    return this.getUser(userId);
  }
}

module.exports = new Database();
