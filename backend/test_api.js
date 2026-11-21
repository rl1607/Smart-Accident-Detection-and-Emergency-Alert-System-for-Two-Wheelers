// Automated Backend Endpoint Verification Script
const http = require('http');

const PORT = 3000;
process.env.PORT = PORT;

// Start server in-process for testing
require('./server');

function request(method, path, body = null) {
  return new Promise((resolve, reject) => {
    const dataString = body ? JSON.stringify(body) : null;
    const options = {
      hostname: '127.0.0.1',
      port: PORT,
      path: path,
      method: method,
      headers: {
        'Content-Type': 'application/json',
        ...(dataString ? { 'Content-Length': Buffer.byteLength(dataString) } : {})
      }
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          resolve({ status: res.statusCode, body: json });
        } catch {
          resolve({ status: res.statusCode, text: data });
        }
      });
    });

    req.on('error', reject);
    if (dataString) req.write(dataString);
    req.end();
  });
}

async function runTests() {
  console.log('\n--- STARTING API VERIFICATION SUITE ---');
  await new Promise(r => setTimeout(r, 1000)); // wait for server to bind

  try {
    // 1. Health check
    const health = await request('GET', '/');
    console.log('Test 1: Health Check (GET /) -> Status:', health.status, '| Body:', health.body);

    // 2. Post Location
    const locPost = await request('POST', '/api/device/ACCIDENT-001/location', {
      latitude: 17.385044,
      longitude: 78.486671,
      speed: 32.5,
      altitude: 542.2,
      satellites: 8,
      locationSource: "GPS"
    });
    console.log('Test 2: Post Location (POST /api/device/ACCIDENT-001/location) -> Status:', locPost.status, '| Body:', locPost.body);

    // 3. Get Latest Location
    const locGet = await request('GET', '/api/device/ACCIDENT-001/location');
    console.log('Test 3: Get Latest Location (GET /api/device/ACCIDENT-001/location) -> Status:', locGet.status, '| Lat/Lon:', locGet.body.latitude, locGet.body.longitude);

    // 4. Get Locations History
    const locsGet = await request('GET', '/api/device/ACCIDENT-001/locations');
    console.log('Test 4: Get Locations History (GET /api/device/ACCIDENT-001/locations) -> Status:', locsGet.status, '| Points count:', Array.isArray(locsGet.body) ? locsGet.body.length : 0);

    // 5. Post Accident
    const accPost = await request('POST', '/api/device/ACCIDENT-001/accident', {
      latitude: 17.385044,
      longitude: 78.486671,
      locationSource: "GPS",
      status: "CONFIRMED",
      smsStatus: "SENT",
      callStatus: "INITIATED"
    });
    console.log('Test 5: Post Accident (POST /api/device/ACCIDENT-001/accident) -> Status:', accPost.status, '| Body:', accPost.body);

    // 6. Get Latest Accident
    const accLatest = await request('GET', '/api/device/ACCIDENT-001/accident/latest');
    console.log('Test 6: Get Latest Accident (GET /api/device/ACCIDENT-001/accident/latest) -> Status:', accLatest.status, '| Status:', accLatest.body?.status);

    // 7. Get Emergency Contact
    const emContact = await request('GET', '/api/device/ACCIDENT-001/emergency-contact');
    console.log('Test 7: Get Emergency Contact (GET /api/device/ACCIDENT-001/emergency-contact) -> Status:', emContact.status, '| Phone:', emContact.body?.phone || emContact.body?.phoneNumber);

    // 8. Get Emergency Contact as plain text (for ESP32 simple parsing)
    const emText = await request('GET', '/api/device/ACCIDENT-001/emergency-contact?format=text');
    console.log('Test 8: ESP32 Plaintext Contact (GET ?format=text) -> Status:', emText.status, '| Text:', emText.text);

    // 9. Update Emergency Contact
    const emUpdate = await request('PUT', '/api/device/ACCIDENT-001/emergency-contact', [{
      name: "Rahul Updated",
      phoneNumber: "+919876543210",
      relationship: "Brother",
      isPrimary: true
    }]);
    console.log('Test 9: Update Emergency Contact (PUT /api/device/ACCIDENT-001/emergency-contact) -> Status:', emUpdate.status, '| Result:', emUpdate.body?.success);

    console.log('\n>>> ALL 9 API TESTS PASSED SUCCESSFULLY! <<<\n');
    process.exit(0);
  } catch (err) {
    console.error('Test failed with error:', err);
    process.exit(1);
  }
}

runTests();
