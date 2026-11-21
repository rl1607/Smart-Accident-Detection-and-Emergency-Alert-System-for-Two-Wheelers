# Smart Accident Detection - Cloud Backend API

Node.js & Express REST API backend for the Smart Accident Detection and Alert System.

---

## 1. Features
- **Health Check Endpoint** at `GET /`
- **Device Telemetry Ingestion** (`POST /api/device/:deviceId/location`)
- **Real-Time Location & Polyline Route History** (`GET /api/device/:deviceId/locations`)
- **Accident Incident Logging** (`POST /api/device/:deviceId/accident`)
- **Latest Accident & Alert History** (`GET /api/device/:deviceId/accident/latest`, `GET /api/device/:deviceId/accidents`)
- **Offline-First Emergency Contact Sync** (`GET /api/device/:deviceId/emergency-contact` and `?format=text`)
- **Supabase PostgreSQL Integration** mapped directly to tables:
  - `users`
  - `devices`
  - `locations`
  - `emergency_contacts`
  - `accidents`
- **Built-in Fallback Store** for offline local development before Supabase keys are provided.

---

## 2. Supabase Configuration

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and fill in your Supabase credentials:
   ```env
   PORT=3000
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_SERVICE_ROLE_KEY=your-supabase-service-role-key
   ```
   > [!IMPORTANT]
   > The `SUPABASE_SERVICE_ROLE_KEY` must remain strictly on the backend server. It must never be placed inside the Android app, frontend client, or ESP32 firmware.

---

## 3. Local Setup & Testing

```bash
cd backend
npm install
npm test      # Runs the 9-point automated test suite verifying all endpoints
npm start     # Starts server on http://localhost:3000
```

---

## 4. Deploying on Render

1. Push your repository to GitHub.
2. In [Render Dashboard](https://dashboard.render.com/):
   - Click **New +** -> **Web Service**.
   - Connect your GitHub repository.
   - Set **Root Directory**: `backend`
   - Set **Build Command**: `npm install`
   - Set **Start Command**: `node server.js`
3. Under **Environment Variables**, add:
   - `SUPABASE_URL`: Your Supabase URL
   - `SUPABASE_SERVICE_ROLE_KEY`: Your Supabase service role key
4. Click **Deploy Web Service**.
5. Update your Android application base URL (in Settings screen or `RetrofitClient.kt`) with your deployed Render URL.
