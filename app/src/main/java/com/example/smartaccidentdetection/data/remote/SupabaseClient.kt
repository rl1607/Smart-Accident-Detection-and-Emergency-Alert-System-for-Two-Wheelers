package com.example.smartaccidentdetection.data.remote

import com.example.smartaccidentdetection.data.model.AccidentAlert
import com.example.smartaccidentdetection.data.model.DeviceStatus
import com.example.smartaccidentdetection.data.model.EmergencyContact
import com.example.smartaccidentdetection.data.model.LocationPoint
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Direct Cloud REST API connection to Supabase PostgreSQL.
 * Allows the Android app to operate 100% independently anywhere in the world
 * without requiring a running local PC or Android Studio.
 */
interface SupabaseApiService {

    @GET("locations")
    suspend fun getLatestLocation(
        @Query("device_id") deviceIdFilter: String,
        @Query("order") order: String = "id.desc",
        @Query("limit") limit: Int = 1
    ): Response<List<LocationPoint>>

    @GET("locations")
    suspend fun getRoutePoints(
        @Query("device_id") deviceIdFilter: String,
        @Query("order") order: String = "id.desc",
        @Query("limit") limit: Int = 100
    ): Response<List<LocationPoint>>

    @GET("devices")
    suspend fun getDeviceStatus(
        @Query("device_id") deviceIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<DeviceStatus>>

    @GET("accidents")
    suspend fun getAccidents(
        @Query("device_id") deviceIdFilter: String,
        @Query("order") order: String = "id.desc",
        @Query("limit") limit: Int = 50
    ): Response<List<AccidentAlert>>

    @POST("accidents")
    suspend fun postAccident(
        @Body accident: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<AccidentAlert>>

    @GET("emergency_contacts")
    suspend fun getEmergencyContacts(
        @Query("select") select: String = "*"
    ): Response<List<EmergencyContact>>

    @PATCH("emergency_contacts")
    suspend fun updateEmergencyContact(
        @Query("id") idFilter: String,
        @Body contact: Map<String, String>
    ): Response<Void>

    @GET("users")
    suspend fun getUsers(
        @Query("select") select: String = "*"
    ): Response<List<com.example.smartaccidentdetection.data.model.UserProfile>>

    @PATCH("users")
    suspend fun updateUser(
        @Query("id") idFilter: String,
        @Body user: Map<String, String>
    ): Response<Void>
}

object SupabaseClient {
    private const val SUPABASE_BASE_URL = "https://your-project-id.supabase.co/rest/v1/"
    private const val SUPABASE_API_KEY = "your-supabase-api-key"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_API_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: SupabaseApiService = Retrofit.Builder()
        .baseUrl(SUPABASE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SupabaseApiService::class.java)
}
