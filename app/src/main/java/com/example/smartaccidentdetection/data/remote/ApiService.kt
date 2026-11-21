package com.example.smartaccidentdetection.data.remote

import com.example.smartaccidentdetection.data.model.AccidentAlert
import com.example.smartaccidentdetection.data.model.ApiResponse
import com.example.smartaccidentdetection.data.model.DeviceStatus
import com.example.smartaccidentdetection.data.model.EmergencyContact
import com.example.smartaccidentdetection.data.model.LocationPoint
import com.example.smartaccidentdetection.data.model.Trip
import com.example.smartaccidentdetection.data.model.UserProfile
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * REST API Endpoints defined in Architecture Specification
 */
interface ApiService {

    @GET("/")
    suspend fun getHealth(): Response<Map<String, String>>

    @GET("/api/device/{deviceId}")
    suspend fun getDevice(@Path("deviceId") deviceId: String): Response<DeviceStatus>

    @GET("/api/device/{deviceId}/status")
    suspend fun getDeviceStatus(@Path("deviceId") deviceId: String): Response<DeviceStatus>

    @GET("/api/device/{deviceId}/location")
    suspend fun getCurrentLocation(@Path("deviceId") deviceId: String): Response<LocationPoint>

    @GET("/api/device/{deviceId}/locations")
    suspend fun getRouteLocations(@Path("deviceId") deviceId: String): Response<List<LocationPoint>>

    @POST("/api/device/{deviceId}/location")
    suspend fun postLocation(
        @Path("deviceId") deviceId: String,
        @Body location: LocationPoint
    ): Response<ApiResponse<LocationPoint>>

    @POST("/api/device/{deviceId}/accident")
    suspend fun postAccident(
        @Path("deviceId") deviceId: String,
        @Body accident: AccidentAlert
    ): Response<ApiResponse<AccidentAlert>>

    @GET("/api/device/{deviceId}/alerts")
    suspend fun getAlerts(@Path("deviceId") deviceId: String): Response<List<AccidentAlert>>

    @GET("/api/device/{deviceId}/accidents")
    suspend fun getAccidents(@Path("deviceId") deviceId: String): Response<List<AccidentAlert>>

    @GET("/api/device/{deviceId}/accident/latest")
    suspend fun getLatestAccident(@Path("deviceId") deviceId: String): Response<AccidentAlert>

    @GET("/api/device/{deviceId}/emergency-contact")
    suspend fun getEmergencyContact(@Path("deviceId") deviceId: String): Response<EmergencyContact>

    @PUT("/api/device/{deviceId}/emergency-contact")
    suspend fun updateEmergencyContacts(
        @Path("deviceId") deviceId: String,
        @Body contacts: List<EmergencyContact>
    ): Response<ApiResponse<List<EmergencyContact>>>

    @POST("/api/users/{userId}/emergency-contact")
    suspend fun postUserEmergencyContact(
        @Path("userId") userId: String,
        @Body contact: EmergencyContact
    ): Response<ApiResponse<EmergencyContact>>

    @PUT("/api/users/{userId}/emergency-contact")
    suspend fun updateUserEmergencyContact(
        @Path("userId") userId: String,
        @Body contact: EmergencyContact
    ): Response<ApiResponse<EmergencyContact>>

    @GET("/api/device/{deviceId}/trips")
    suspend fun getTrips(@Path("deviceId") deviceId: String): Response<List<Trip>>

    @GET("/api/users/{id}")
    suspend fun getUser(@Path("id") userId: String): Response<UserProfile>

    @PUT("/api/users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body user: UserProfile
    ): Response<ApiResponse<UserProfile>>
}

/**
 * Retrofit Client Singleton with configurable Base URL
 */
object RetrofitClient {
    // Default endpoint: Local PC running backend server on same Wi-Fi
    var baseUrl: String = "http://192.168.31.86:3000/"
        set(value) {
            field = if (!value.endsWith("/")) "$value/" else value
            apiServiceInstance = createService(field)
        }

    private fun createService(url: String): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private var apiServiceInstance: ApiService = createService(baseUrl)

    val apiService: ApiService
        get() = apiServiceInstance
}
