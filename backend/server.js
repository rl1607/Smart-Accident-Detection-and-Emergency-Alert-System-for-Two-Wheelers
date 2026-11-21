require('dotenv').config();
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const supabase = require('./supabaseClient');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(morgan('dev'));

// 1. Health check endpoint
app.get('/', (req, res) => {
  res.json({
    status: "online",
    service: "Smart Accident Detection API",
    database: supabase.isConfigured() ? "Supabase PostgreSQL" : "Local Persistent Store",
    timestamp: new Date().toISOString()
  });
});

// 2. Device Location Upload (from ESP32 / SIM800L GPRS or App)
app.post('/api/device/:deviceId/location', async (req, res) => {
  const { deviceId } = req.params;
  const payload = { ...req.query, ...req.body };

  if (payload.latitude === undefined || payload.longitude === undefined) {
    return res.status(400).json({ error: "Missing latitude or longitude in request" });
  }

  try {
    const point = await supabase.insertLocation(deviceId, payload);
    console.log(`[LOCATION] Device ${deviceId}: Lat ${point.latitude}, Lon ${point.longitude}, Speed ${point.speedKmh || point.speed} km/h`);
    res.status(201).json({
      success: true,
      message: "Location ingested successfully",
      data: point
    });
  } catch (err) {
    console.error('[ERROR] Failed to ingest location:', err.message);
    res.status(500).json({ error: "Failed to store location", details: err.message });
  }
});

// 3. Current Latest Location
app.get('/api/device/:deviceId/location', async (req, res) => {
  const { deviceId } = req.params;
  try {
    const latest = await supabase.getLatestLocation(deviceId);
    if (!latest) {
      return res.status(404).json({ error: `No location recorded for device ${deviceId}` });
    }
    res.json(latest);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. Location History (for Drawing Route Polyline on Android Google Maps)
app.get('/api/device/:deviceId/locations', async (req, res) => {
  const { deviceId } = req.params;
  const limit = parseInt(req.query.limit) || 100;
  try {
    const history = await supabase.getLocationHistory(deviceId, limit);
    res.json(history);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. Accident Creation (from ESP32 confirmed accident or test trigger)
app.post('/api/device/:deviceId/accident', async (req, res) => {
  const { deviceId } = req.params;
  const payload = { ...req.query, ...req.body };

  try {
    const accident = await supabase.insertAccident(deviceId, payload);
    console.log(`[ALERT] Accident logged for device ${deviceId}:`, accident);
    res.status(201).json({
      success: true,
      message: "Accident recorded",
      data: accident
    });
  } catch (err) {
    console.error('[ERROR] Failed to record accident:', err.message);
    res.status(500).json({ error: "Failed to record accident", details: err.message });
  }
});

// 6. Latest Accident for Device
app.get('/api/device/:deviceId/accident/latest', async (req, res) => {
  const { deviceId } = req.params;
  try {
    const latest = await supabase.getLatestAccident(deviceId);
    if (!latest) {
      return res.status(404).json({ error: `No accident recorded for device ${deviceId}` });
    }
    res.json(latest);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 7. Accident History
app.get('/api/device/:deviceId/accidents', async (req, res) => {
  const { deviceId } = req.params;
  const limit = parseInt(req.query.limit) || 50;
  try {
    const list = await supabase.getAccidentHistory(deviceId, limit);
    res.json(list);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Alias for Android Alerts Screen
app.get('/api/device/:deviceId/alerts', async (req, res) => {
  const { deviceId } = req.params;
  try {
    const list = await supabase.getAccidentHistory(deviceId);
    res.json(list);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 8. Emergency Contact Retrieval (Used by ESP32 via GPRS and Android App)
app.get('/api/device/:deviceId/emergency-contact', async (req, res) => {
  const { deviceId } = req.params;
  try {
    const contact = await supabase.getEmergencyContact(deviceId);

    // Format query for ESP32 text parsing
    if (req.query.format === 'text') {
      return res.send(contact.phone || "+919876543210");
    }

    res.json(contact);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 9. Update Emergency Contact by User ID
app.post('/api/users/:userId/emergency-contact', async (req, res) => {
  const { userId } = req.params;
  try {
    const updated = await supabase.updateEmergencyContact(userId, req.body);
    res.json({
      success: true,
      message: "Emergency contact updated successfully",
      data: updated
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/users/:userId/emergency-contact', async (req, res) => {
  const { userId } = req.params;
  try {
    const updated = await supabase.updateEmergencyContact(userId, req.body);
    res.json({
      success: true,
      message: "Emergency contact updated successfully",
      data: updated
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Alias for updating emergency contact directly by device ID
app.put('/api/device/:deviceId/emergency-contact', async (req, res) => {
  const { deviceId } = req.params;
  try {
    const contact = Array.isArray(req.body) ? req.body[0] : req.body;
    const updated = await supabase.updateEmergencyContact(contact.userId || "USER_001", contact);
    res.json({
      success: true,
      message: "Emergency contact updated for device",
      data: [updated]
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 10. Device Status & Information
app.get('/api/device/:deviceId', async (req, res) => {
  const { deviceId } = req.params;
  try {
    const device = await supabase.getDevice(deviceId);
    res.json(device);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/device/:deviceId/status', async (req, res) => {
  const { deviceId } = req.params;
  try {
    const device = await supabase.getDevice(deviceId);
    res.json(device);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/device/:deviceId/trips', (req, res) => {
  const localDb = require('./database');
  res.json(localDb.getTrips(req.params.deviceId));
});

// 11. User Profile Management
app.get('/api/users/:id', async (req, res) => {
  try {
    const user = await supabase.getUser(req.params.id);
    res.json(user);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/users/:id', async (req, res) => {
  try {
    const updated = await supabase.updateUser(req.params.id, req.body);
    res.json({
      success: true,
      message: "User profile updated",
      data: updated
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 12. Backward Compatibility Routes for testing
app.get('/data', async (req, res) => {
  const dev = await supabase.getDevice("ACCIDENT-001");
  res.json(dev);
});

app.post('/update', async (req, res) => {
  const payload = { ...req.query, ...req.body };
  const point = await supabase.insertLocation("ACCIDENT-001", payload);
  res.json({ success: true, point });
});

// Start Server listening on 0.0.0.0 for Render & local
app.listen(PORT, '0.0.0.0', () => {
  console.log(`====================================================`);
  console.log(` Smart Accident Detection REST API Server`);
  console.log(` Listening on: http://0.0.0.0:${PORT}`);
  console.log(` Health Check: http://localhost:${PORT}/`);
  console.log(` Database:    ${supabase.isConfigured() ? "Supabase PostgreSQL" : "Local Store Fallback"}`);
  console.log(`====================================================`);
});
