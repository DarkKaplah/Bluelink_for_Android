package com.example.iosportattempt1.repository

import android.util.Log
import com.example.iosportattempt1.models.*
import com.example.iosportattempt1.network.*
import com.example.iosportattempt1.storage.DataStoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class BluelinkRepository(
    private val dataStoreManager: DataStoreManager
) {
    private val TAG = "BluelinkRepo"
    private val api: BluelinkApi
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        isLenient = true
    }

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.owners.kia.com/apigw/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        api = retrofit.create(BluelinkApi::class.java)
    }

    private fun genRanHex(size: Int): String {
        val chars = "0123456789abcdef"
        return (1..size).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun generateDeviceId(): String {
        return "${genRanHex(22)}:${genRanHex(9)}_${genRanHex(10)}-${genRanHex(5)}_${genRanHex(22)}_${genRanHex(8)}-${genRanHex(18)}-_${genRanHex(22)}_${genRanHex(17)}"
    }

    private fun getDateString(): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(Date())
    }

    private fun getOffset(): String {
        val tz = TimeZone.getDefault()
        val offsetMillis = tz.getOffset(System.currentTimeMillis())
        val hours = offsetMillis / (1000 * 60 * 60)
        return hours.toString()
    }

    private suspend fun getBaseHeaders(includeAuth: Boolean = true): MutableMap<String, String> {
        val tokens = dataStoreManager.tokensFlow.first()
        var deviceId = tokens["device_id"]
        var clientUuid = tokens["client_uuid"]
        val accessToken = tokens["access_token"]
        val authId = tokens["auth_id"]

        if (deviceId.isNullOrEmpty()) {
            deviceId = generateDeviceId()
            clientUuid = UUID.randomUUID().toString().lowercase()
            dataStoreManager.saveTokens(deviceId = deviceId, clientUuid = clientUuid)
        }

        val headers = mutableMapOf(
            "Accept" to "application/json, text/plain, */*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Content-Type" to "application/json",
            "from" to "SPA",
            "language" to "0",
            "offset" to getOffset(), 
            "appType" to "L",
            "appVersion" to "7.22.0",
            "clientuuid" to clientUuid!!,
            "clientId" to "SPACL716-APL",
            "phonebrand" to "iPhone",
            "osType" to "iOS",
            "osVersion" to "15.8.5",
            "secretKey" to "sydnat-9kykci-Kuhtep-h5nK",
            "to" to "APIGW",
            "tokentype" to "A",
            "User-Agent" to "KIAPrimo_iOS/37 CFNetwork/1335.0.3.4 Darwin/21.6.0",
            "deviceId" to deviceId!!,
            "Host" to "api.owners.kia.com",
            "date" to getDateString()
        )
        
        if (includeAuth) {
            if (!accessToken.isNullOrEmpty()) {
                headers["sid"] = accessToken
            }

            if (!authId.isNullOrEmpty()) {
                headers["vinKey"] = authId
            }
        }
        
        return headers
    }

    suspend fun login(): String? {
        val appConfig = dataStoreManager.appConfigFlow.first()
        val auth = appConfig.auth
        val headers = getBaseHeaders(includeAuth = false)
        val tokens = dataStoreManager.tokensFlow.first()
        val rmToken = tokens["rm_token"]

        if (!rmToken.isNullOrEmpty()) {
            headers["rmtoken"] = rmToken
        }

        val body = buildJsonObject {
            put("deviceKey", headers["deviceId"] ?: "")
            put("deviceType", 2)
            put("tncFlag", 1)
            putJsonObject("userCredential") {
                put("userId", auth.username)
                put("password", auth.password)
            }
        }

        try {
            val response = api.login(headers, body)
            if (response.isSuccessful) {
                val sid = response.headers()["sid"]
                val xid = response.headers()["xid"]
                val rmtoken = response.headers()["rmtoken"]

                if (!sid.isNullOrEmpty()) {
                    dataStoreManager.saveTokens(accessToken = sid, rmToken = rmtoken)
                    // After successful login, get the correct vehicleKey using the new sid
                    getVehicles(sid)
                    return "SUCCESS"
                } else if (!xid.isNullOrEmpty()) {
                    val loginResponse = response.body()
                    val otpKey = loginResponse?.payload?.otpKey
                    
                    if (otpKey != null) {
                        val phoneVerifyStatus = loginResponse.payload.phoneVerifyStatus ?: false
                        val notifyType = if (phoneVerifyStatus && appConfig.mfaPreference.lowercase() == "sms") "SMS" else "EMAIL"
                        val mfaHeaders = getBaseHeaders(includeAuth = false)
                        mfaHeaders["otpkey"] = otpKey
                        mfaHeaders["notifytype"] = notifyType
                        mfaHeaders["xid"] = xid
                        
                        val sendOtpResponse = api.sendOTP(mfaHeaders, buildJsonObject { })
                        if (sendOtpResponse.isSuccessful) {
                            return "MFA_REQUIRED:$xid:$otpKey"
                        } else {
                            Log.e(TAG, "sendOTP failed: ${sendOtpResponse.code()}")
                        }
                    } else {
                        Log.e(TAG, "otpKey is null in login response")
                    }
                }
            } else {
                Log.e(TAG, "Login API failed with code: ${response.code()} error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login exception", e)
            return "ERROR: ${e.message}"
        }
        return "FAILED"
    }

    suspend fun verifyMfa(code: String, xid: String, otpKey: String): Boolean {
        val headers = getBaseHeaders(includeAuth = false)
        headers["xid"] = xid
        headers["otpkey"] = otpKey
        val body = buildJsonObject {
            put("otp", code)
        }
        try {
            val response = api.verifyOTP(headers, body)
            if (response.isSuccessful) {
                val sid = response.headers()["sid"]
                val rmtoken = response.headers()["rmtoken"]
                if (!sid.isNullOrEmpty()) {
                    dataStoreManager.saveTokens(accessToken = sid, rmToken = rmtoken)
                    // After successful MFA, get the correct vehicleKey using the new sid
                    getVehicles(sid)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyMfa exception", e)
        }
        return false
    }

    suspend fun getVehicles(overrideSid: String? = null): String? {
        val headers = getBaseHeaders(includeAuth = true)
        if (!overrideSid.isNullOrEmpty()) {
            headers["sid"] = overrideSid
        }
        // Ensure vinKey is NOT sent for ownr/gvl as it's not valid yet
        headers.remove("vinKey")
        try {
            Log.d(TAG, "Fetching vehicles...")
            val response = api.getVehicles(headers)
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "getVehicles response: $body")
                val vehicles = body?.payload?.vehicleSummary
                if (!vehicles.isNullOrEmpty()) {
                    val vehicleKey = vehicles[0].vehicleKey
                    if (vehicleKey != null) {
                        Log.d(TAG, "Found vehicleKey: $vehicleKey")
                        dataStoreManager.saveTokens(authId = vehicleKey)
                        return vehicleKey
                    }
                } else {
                    Log.w(TAG, "No vehicles found in summary")
                }
            } else {
                Log.e(TAG, "getVehicles failed: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getVehicles exception", e)
        }
        return null
    }

    suspend fun lock(): Boolean {
        return executeCommand { api.lock(it) }
    }

    suspend fun unlock(): Boolean {
        return executeCommand { api.unlock(it) }
    }

    suspend fun startCharge(): Boolean {
        return executeCommand { api.startCharge(it) }
    }

    suspend fun stopCharge(): Boolean {
        return executeCommand { api.stopCharge(it) }
    }

    suspend fun climateOn(temp: Double): Boolean {
        val appConfig = dataStoreManager.appConfigFlow.first()
        val tempUnit = if (appConfig.tempType.equals("F", ignoreCase = true)) 1 else 0
        val tempValueStr = if (temp % 1.0 == 0.0) temp.toInt().toString() else temp.toString()

        val body = buildJsonObject {
            putJsonObject("remoteClimate") {
                put("airCtrl", true)
                put("defrost", false)
                putJsonObject("airTemp") {
                    put("value", tempValueStr)
                    put("unit", tempUnit)
                }
                putJsonObject("ignitionOnDuration") {
                    put("unit", 4)
                    put("value", 15)
                }
                putJsonObject("heatingAccessory") {
                    put("steeringWheel", 0)
                    put("rearWindow", 0)
                    put("sideMirror", 0)
                }
            }
        }
        return executeCommand { api.climateOn(it, body) }
    }

    suspend fun climateOff(): Boolean {
        return executeCommand { api.climateOff(it) }
    }

    private suspend fun executeCommand(apiCall: suspend (Map<String, String>) -> retrofit2.Response<okhttp3.ResponseBody>): Boolean {
        val headers = getBaseHeaders(includeAuth = true)
        Log.d(TAG, "Executing command with headers: ${headers.keys}")
        try {
            val response = apiCall(headers)
            if (response.isSuccessful) {
                val xid = response.headers()["xid"] ?: response.headers()["Xid"]
                Log.d(TAG, "Command response successful, xid: $xid")
                if (xid != null) {
                    return pollForCompletion(xid)
                } else {
                    Log.e(TAG, "Command successful but xid header is missing")
                }
            } else {
                Log.e(TAG, "Command failed: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "executeCommand exception", e)
        }
        return false
    }

    private suspend fun pollForCompletion(xid: String): Boolean {
        repeat(30) {
            delay(2000)
            val headers = getBaseHeaders(includeAuth = true)
            val body = buildJsonObject {
                put("xid", xid)
            }
            try {
                val response = api.getTransactionStatus(headers, body)
                if (response.isSuccessful) {
                    val statusResponse = response.body()
                    
                    // Check for error in the status object
                    val statusObj = statusResponse?.status
                    if (statusObj != null && statusObj.errorCode != null && statusObj.errorCode != 0) {
                        Log.e(TAG, "Transaction error: ${statusObj.errorMessage} (${statusObj.errorCode})")
                        // If we get 1005 here, it might mean the session was invalidated or something went wrong with the vehicleKey
                        return false
                    }

                    val payload = statusResponse?.payload
                    
                    if (payload != null) {
                        val isStillProcessing = payload.values.any { it == 1 }
                        val hasFailed = payload.values.any { it > 1 }
                        
                        if (hasFailed) return false
                        if (!isStillProcessing) return true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "pollForCompletion exception", e)
            }
        }
        return false
    }

    suspend fun getStatus(forceUpdate: Boolean = false): BluelinkStatus? {
        val tokens = dataStoreManager.tokensFlow.first()
        val authId = tokens["auth_id"] ?: ""

        if (authId.isEmpty()) {
            Log.w(TAG, "Cannot get status: auth_id (vinKey) is empty. User might not be logged in.")
            return null
        }

        val headers = getBaseHeaders(includeAuth = true)
        try {
            val response = if (forceUpdate) {
                val body = buildJsonObject {
                    put("requestType", 0)
                }
                api.refreshVehicleStatus(headers, body)
            } else {
                val body = buildJsonObject {
                    putJsonObject("vehicleConfigReq") {
                        put("airTempRange", "0")
                        put("maintenance", "0")
                        put("seatHeatCoolOption", "0")
                        put("vehicle", "1")
                        put("vehicleFeature", "0")
                    }
                    putJsonObject("vehicleInfoReq") {
                        put("drivingActivty", "0")
                        put("dtc", "1")
                        put("enrollment", "0")
                        put("functionalCards", "0")
                        put("location", "1")
                        put("vehicleStatus", "1")
                        put("weather", "0")
                    }
                    put("vinKey", buildJsonArray { add(authId) })
                }
                api.getVehicleInfo(headers, body)
            }

            if (response.isSuccessful) {
                val responseData = response.body()
                
                val status = if (forceUpdate) {
                    responseData?.payload?.vehicleStatusRpt?.vehicleStatus
                } else {
                    responseData?.payload?.vehicleInfoList?.firstOrNull()?.lastVehicleInfo?.vehicleStatusRpt?.vehicleStatus
                }

                val location = responseData?.payload?.vehicleInfoList?.firstOrNull()?.lastVehicleInfo?.location

                return status?.let { s ->
                    BluelinkStatus(
                        soc = s.evStatus?.batteryStatus ?: 0,
                        range = s.evStatus?.drvDistance?.firstOrNull()?.rangeByFuel?.evModeRange?.value?.toInt() ?: 0,
                        isCharging = s.evStatus?.batteryCharge ?: false,
                        isPluggedIn = (s.evStatus?.batteryPlugin ?: 0) > 0,
                        locked = s.doorLock,
                        climate = s.climate?.airCtrl ?: false,
                        twelveSoc = s.batteryStatus?.stateOfCharge ?: 0,
                        chargingPower = s.evStatus?.realTimePower ?: 0.0,
                        remainingChargeTimeMins = s.evStatus?.remainChargeTime?.firstOrNull()?.timeInterval?.value?.toInt() ?: 0,
                        location = location?.coord?.let { Location(it.lat, it.lon) }
                    )
                }
            } else {
                Log.e(TAG, "getStatus failed: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getStatus exception", e)
        }
        return null
    }
}
