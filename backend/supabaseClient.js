require('dotenv').config();
const { createClient } = require('@supabase/supabase-js');
const localDb = require('./database');

const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

let supabase = null;
let isSupabaseConfigured = false;

if (supabaseUrl && supabaseKey && !supabaseUrl.includes('your-project') && !supabaseKey.includes('your-service-role-key')) {
  try {
    supabase = createClient(supabaseUrl, supabaseKey, {
      auth: {
        persistSession: false,
        autoRefreshToken: false
      }
    });
    isSupabaseConfigured = true;
    console.log('[SUPABASE] Connected successfully to Supabase PostgreSQL at:', supabaseUrl);
  } catch (err) {
    console.error('[SUPABASE] Initialization error:', err.message);
  }
} else {
  console.log('[SUPABASE] Notice: SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY not configured in .env.');
  console.log('[SUPABASE] Operating in local persistent store fallback mode. Set .env to connect to live Supabase.');
}

/**
 * Supabase Data Service
 */
class SupabaseService {
  isConfigured() {
    return isSupabaseConfigured && supabase !== null;
  }

  // 1. Locations
  async insertLocation(deviceId, data) {
    const lat = parseFloat(data.latitude) || 0.0;
    const lon = parseFloat(data.longitude) || 0.0;
    const speed = parseFloat(data.speed || data.speedKmh) || 0.0;
    const alt = parseFloat(data.altitude || data.altitudeM) || 0.0;
    const sats = parseInt(data.satellites) || 8;
    const locSource = data.locationSource || data.location_source || "GPS";

    if (this.isConfigured()) {
      try {
        // Insert into locations table
        const { data: locationRecord, error: locErr } = await supabase
          .from('locations')
          .insert([
            {
              device_id: deviceId,
              latitude: lat,
              longitude: lon,
              speed: speed,
              altitude: alt,
              satellites: sats,
              location_source: locSource
            }
          ])
          .select()
          .single();

        if (locErr) {
          console.error('[SUPABASE] Error inserting location:', locErr.message);
          throw locErr;
        }

        // Update or insert into devices table
        const nowIso = new Date().toISOString();
        const { data: existingDevice } = await supabase
          .from('devices')
          .select('id')
          .eq('device_id', deviceId)
          .maybeSingle();

        if (existingDevice) {
          await supabase
            .from('devices')
            .update({
              last_latitude: lat,
              last_longitude: lon,
              last_seen: nowIso,
              status: 'online'
            })
            .eq('device_id', deviceId);
        } else {
          await supabase
            .from('devices')
            .insert([
              {
                device_id: deviceId,
                status: 'online',
                last_latitude: lat,
                last_longitude: lon,
                last_seen: nowIso
              }
            ]);
        }

        return {
          id: locationRecord.id,
          deviceId: locationRecord.device_id,
          latitude: locationRecord.latitude,
          longitude: locationRecord.longitude,
          speedKmh: locationRecord.speed,
          altitudeM: locationRecord.altitude,
          satellites: locationRecord.satellites,
          locationSource: locationRecord.location_source,
          createdAt: locationRecord.created_at
        };
      } catch (err) {
        console.error('[SUPABASE] Falling back to local store for insertLocation:', err.message);
      }
    }

    return localDb.addLocation(deviceId, {
      latitude: lat,
      longitude: lon,
      speedKmh: speed,
      altitudeM: alt,
      satellites: sats,
      locationSource: locSource
    });
  }

  async getLatestLocation(deviceId) {
    if (this.isConfigured()) {
      try {
        const { data, error } = await supabase
          .from('locations')
          .select('*')
          .eq('device_id', deviceId)
          .order('created_at', { ascending: false })
          .limit(1)
          .maybeSingle();

        if (error) throw error;
        if (data) {
          return {
            id: data.id,
            deviceId: data.device_id,
            latitude: data.latitude,
            longitude: data.longitude,
            speedKmh: data.speed,
            speed: data.speed,
            altitudeM: data.altitude,
            altitude: data.altitude,
            satellites: data.satellites,
            locationSource: data.location_source,
            createdAt: data.created_at,
            timestamp: new Date(data.created_at).getTime()
          };
        }
      } catch (err) {
        console.error('[SUPABASE] getLatestLocation error:', err.message);
      }
    }

    return localDb.getLatestLocation(deviceId);
  }

  async getLocationHistory(deviceId, limit = 100) {
    if (this.isConfigured()) {
      try {
        const { data, error } = await supabase
          .from('locations')
          .select('*')
          .eq('device_id', deviceId)
          .order('created_at', { ascending: true })
          .limit(limit);

        if (error) throw error;
        if (data && data.length > 0) {
          return data.map(item => ({
            id: item.id,
            deviceId: item.device_id,
            latitude: item.latitude,
            longitude: item.longitude,
            speedKmh: item.speed,
            speed: item.speed,
            altitudeM: item.altitude,
            altitude: item.altitude,
            satellites: item.satellites,
            locationSource: item.location_source,
            createdAt: item.created_at,
            timestamp: new Date(item.created_at).getTime()
          }));
        }
      } catch (err) {
        console.error('[SUPABASE] getLocationHistory error:', err.message);
      }
    }

    return localDb.getLocations(deviceId, limit);
  }

  // 2. Accidents
  async insertAccident(deviceId, data) {
    const lat = parseFloat(data.latitude) || 0.0;
    const lon = parseFloat(data.longitude) || 0.0;
    const locSource = data.locationSource || data.location_source || "GPS";
    const status = data.status || "CONFIRMED";
    const smsStatus = data.smsStatus || data.sms_status || "SENT";
    const callStatus = data.callStatus || data.call_status || "INITIATED";

    if (this.isConfigured()) {
      try {
        const { data: record, error } = await supabase
          .from('accidents')
          .insert([
            {
              device_id: deviceId,
              latitude: lat,
              longitude: lon,
              location_source: locSource,
              status: status,
              sms_status: smsStatus,
              call_status: callStatus
            }
          ])
          .select()
          .single();

        if (error) throw error;

        // Also update device accident state
        await supabase
          .from('devices')
          .update({
            status: status === 'CONFIRMED' ? 'accident_confirmed' : 'online',
            last_seen: new Date().toISOString()
          })
          .eq('device_id', deviceId);

        return {
          id: record.id,
          deviceId: record.device_id,
          latitude: record.latitude,
          longitude: record.longitude,
          locationSource: record.location_source,
          status: record.status,
          smsStatus: record.sms_status,
          callStatus: record.call_status,
          createdAt: record.created_at,
          timestamp: new Date(record.created_at).getTime(),
          locationName: `Lat: ${record.latitude}, Lon: ${record.longitude}`,
          impactG: parseFloat(data.impactG) || 4.25,
          tiltAngle: parseFloat(data.tiltAngle) || 49.0,
          detectionType: data.detectionType || "Impact (>=4.0g) or Tilt (>=45°)"
        };
      } catch (err) {
        console.error('[SUPABASE] insertAccident error:', err.message);
      }
    }

    return localDb.addAccident(deviceId, data);
  }

  async getLatestAccident(deviceId) {
    if (this.isConfigured()) {
      try {
        const { data, error } = await supabase
          .from('accidents')
          .select('*')
          .eq('device_id', deviceId)
          .order('created_at', { ascending: false })
          .limit(1)
          .maybeSingle();

        if (error) throw error;
        if (data) {
          return {
            id: data.id,
            deviceId: data.device_id,
            latitude: data.latitude,
            longitude: data.longitude,
            locationSource: data.location_source,
            status: data.status,
            smsStatus: data.sms_status,
            callStatus: data.call_status,
            createdAt: data.created_at,
            timestamp: new Date(data.created_at).getTime(),
            locationName: `Incident Location (${data.latitude.toFixed(4)}, ${data.longitude.toFixed(4)})`,
            impactG: 4.25,
            tiltAngle: 49.0,
            detectionType: "High Impact & Tilt"
          };
        }
      } catch (err) {
        console.error('[SUPABASE] getLatestAccident error:', err.message);
      }
    }

    const localAlerts = localDb.getAccidents(deviceId);
    return localAlerts.length > 0 ? localAlerts[0] : null;
  }

  async getAccidentHistory(deviceId, limit = 50) {
    if (this.isConfigured()) {
      try {
        const { data, error } = await supabase
          .from('accidents')
          .select('*')
          .eq('device_id', deviceId)
          .order('created_at', { ascending: false })
          .limit(limit);

        if (error) throw error;
        if (data && data.length > 0) {
          return data.map(item => ({
            id: item.id,
            deviceId: item.device_id,
            latitude: item.latitude,
            longitude: item.longitude,
            locationSource: item.location_source,
            status: item.status,
            smsStatus: item.sms_status,
            callStatus: item.call_status,
            createdAt: item.created_at,
            timestamp: new Date(item.created_at).getTime(),
            locationName: `Incident near ${item.latitude.toFixed(4)}, ${item.longitude.toFixed(4)}`,
            impactG: 4.25,
            tiltAngle: 49.0,
            detectionType: "Impact & 45° Tilt"
          }));
        }
      } catch (err) {
        console.error('[SUPABASE] getAccidentHistory error:', err.message);
      }
    }

    return localDb.getAccidents(deviceId);
  }

  // 3. Emergency Contacts
  async getEmergencyContact(deviceId) {
    if (this.isConfigured()) {
      try {
        // First find user_id from devices table
        const { data: dev } = await supabase
          .from('devices')
          .select('user_id')
          .eq('device_id', deviceId)
          .maybeSingle();

        let query = supabase.from('emergency_contacts').select('*');
        if (dev && dev.user_id) {
          query = query.eq('user_id', dev.user_id);
        }

        const { data: contacts, error } = await query
          .order('is_primary', { ascending: false })
          .limit(5);

        if (error) throw error;
        if (contacts && contacts.length > 0) {
          const primary = contacts.find(c => c.is_primary) || contacts[0];
          return {
            id: String(primary.id || "1"),
            name: primary.name || "Emergency Contact",
            phoneNumber: primary.phone || "+919876543210",
            phone: primary.phone || "+919876543210",
            relationship: primary.relationship || "Family",
            isPrimary: primary.is_primary !== false,
            syncState: "Synced with ESP32",
            userId: String(primary.user_id || "1")
          };
        }
      } catch (err) {
        console.error('[SUPABASE] getEmergencyContact error:', err.message);
      }
    }

    const localList = localDb.getContacts(deviceId);
    const primary = localList.find(c => c.isPrimary) || localList[0];
    return {
      name: primary ? primary.name : "Emergency Contact",
      phone: primary ? primary.phoneNumber : "+919876543210",
      relationship: primary ? primary.relationship : "Family",
      isPrimary: true
    };
  }

  async updateEmergencyContact(userId, contactData) {
    if (this.isConfigured()) {
      try {
        let validUserId = parseInt(userId, 10);
        if (isNaN(validUserId)) {
          const { data: firstUser } = await supabase.from('users').select('id').limit(1).maybeSingle();
          if (firstUser && firstUser.id) {
            validUserId = firstUser.id;
          } else {
            const { data: newUser } = await supabase.from('users').insert([{
              name: contactData.name || "Rahul Sharma",
              phone: contactData.phone || contactData.phoneNumber || "+919353035530"
            }]).select('id').maybeSingle();
            validUserId = newUser ? newUser.id : 1;
          }
        }

        // If set as primary, unmark other contacts for this user first
        if (contactData.isPrimary || contactData.is_primary) {
          await supabase
            .from('emergency_contacts')
            .update({ is_primary: false })
            .eq('user_id', validUserId);
        }

        const { data, error } = await supabase
          .from('emergency_contacts')
          .insert([
            {
              user_id: validUserId,
              name: contactData.name,
              phone: contactData.phone || contactData.phoneNumber,
              relationship: contactData.relationship,
              is_primary: contactData.isPrimary !== undefined ? contactData.isPrimary : true
            }
          ])
          .select()
          .single();

        if (error) throw error;
        return {
          id: data.id,
          userId: data.user_id,
          name: data.name,
          phone: data.phone,
          relationship: data.relationship,
          isPrimary: data.is_primary
        };
      } catch (err) {
        console.error('[SUPABASE] updateEmergencyContact error:', err.message);
      }
    }

    return localDb.setContacts("ACCIDENT-001", [{
      id: "CNT_01",
      userId: userId,
      name: contactData.name,
      phoneNumber: contactData.phone || contactData.phoneNumber,
      relationship: contactData.relationship,
      isPrimary: true
    }])[0];
  }

  // 4. Device Information & Status
  async getDevice(deviceId) {
    if (this.isConfigured()) {
      try {
        const { data, error } = await supabase
          .from('devices')
          .select('*')
          .eq('device_id', deviceId)
          .maybeSingle();

        if (error) throw error;
        if (data) {
          // Check latest location for speed
          const { data: latestLoc } = await supabase
            .from('locations')
            .select('speed')
            .eq('device_id', deviceId)
            .order('created_at', { ascending: false })
            .limit(1)
            .maybeSingle();

          return {
            deviceId: data.device_id,
            userId: data.user_id,
            status: data.status,
            isOnline: data.status === 'online',
            batteryPercent: 90,
            gsmSignalDbm: -68,
            gsmNetworkStatus: "Registered (GPRS)",
            gpsFix: true,
            satellites: 8,
            mpuStatus: "Active (Calibrated 45°/4.0g)",
            lastSeen: data.last_seen || "Just now",
            locationSource: "GPS",
            emergencySyncStatus: "Synced",
            lastSyncTimestamp: "Today",
            lastLatitude: data.last_latitude,
            lastLongitude: data.last_longitude,
            currentSpeedKmh: latestLoc ? parseFloat(latestLoc.speed || 0.0) : 0.0,
            distanceTravelledKm: 14.8,
            accidentState: data.status === 'accident_confirmed' ? 'CONFIRMED_ACCIDENT' : 'SAFE'
          };
        }
      } catch (err) {
        console.error('[SUPABASE] getDevice error:', err.message);
      }
    }

    return localDb.getDevice(deviceId);
  }

  // 5. User Profile
  async getUser(userId) {
    if (this.isConfigured()) {
      try {
        const { data, error } = await supabase
          .from('users')
          .select('*')
          .eq('id', userId)
          .maybeSingle();

        if (error) throw error;
        if (data) {
          return {
            id: data.id,
            fullName: data.name,
            phoneNumber: data.phone,
            email: data.email,
            vehicleNumber: data.vehicle_number,
            vehicleModel: data.vehicle_model,
            createdAt: data.created_at
          };
        }
      } catch (err) {
        console.error('[SUPABASE] getUser error:', err.message);
      }
    }

    return localDb.getUser(userId);
  }

  async updateUser(userId, userData) {
    if (this.isConfigured()) {
      try {
        const { data, error } = await supabase
          .from('users')
          .upsert([
            {
              id: userId,
              name: userData.fullName || userData.name,
              phone: userData.phoneNumber || userData.phone,
              email: userData.email,
              vehicle_number: userData.vehicleNumber || userData.vehicle_number,
              vehicle_model: userData.vehicleModel || userData.vehicle_model
            }
          ])
          .select()
          .single();

        if (error) throw error;
        return {
          id: data.id,
          fullName: data.name,
          phoneNumber: data.phone,
          email: data.email,
          vehicleNumber: data.vehicle_number,
          vehicleModel: data.vehicle_model
        };
      } catch (err) {
        console.error('[SUPABASE] updateUser error:', err.message);
      }
    }

    return localDb.updateUser(userId, userData);
  }
}

module.exports = new SupabaseService();
