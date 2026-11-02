package com.example.iosportattempt1.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.iosportattempt1.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val PIN = stringPreferencesKey("pin")
        val REGION = stringPreferencesKey("region")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val RM_TOKEN = stringPreferencesKey("rm_token")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val CLIENT_UUID = stringPreferencesKey("client_uuid")
        val VIN = stringPreferencesKey("vin")
        val AUTH_ID = stringPreferencesKey("auth_id")
        val MANUFACTURER = stringPreferencesKey("manufacturer")
        val TEMP_TYPE = stringPreferencesKey("temp_type")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val MFA_PREFERENCE = stringPreferencesKey("mfa_preference")
        val CLIMATE_WARM = doublePreferencesKey("climate_warm")
        val CLIMATE_COLD = doublePreferencesKey("climate_cold")
        val CAR_COLOR = stringPreferencesKey("car_color")
    }

    val appConfigFlow: Flow<AppConfig> = context.dataStore.data.map { preferences ->
        AppConfig(
            manufacturer = preferences[MANUFACTURER] ?: "Kia",
            region = preferences[REGION] ?: "USA",
            auth = AuthConfig(
                username = preferences[USERNAME] ?: "",
                password = preferences[PASSWORD] ?: "",
                pin = preferences[PIN] ?: "",
                region = preferences[REGION] ?: "USA"
            ),
            vin = preferences[VIN],
            tempType = preferences[TEMP_TYPE] ?: "F",
            distanceUnit = preferences[DISTANCE_UNIT] ?: "mi",
            mfaPreference = preferences[MFA_PREFERENCE] ?: "email",
            climateTempWarm = preferences[CLIMATE_WARM] ?: 71.0,
            climateTempCold = preferences[CLIMATE_COLD] ?: 66.0,
            carColor = preferences[CAR_COLOR] ?: "White"
        )
    }

    suspend fun saveAppConfig(config: AppConfig) {
        context.dataStore.edit { preferences ->
            preferences[MANUFACTURER] = config.manufacturer
            preferences[REGION] = config.region
            preferences[USERNAME] = config.auth.username
            preferences[PASSWORD] = config.auth.password
            preferences[PIN] = config.auth.pin
            preferences[VIN] = config.vin ?: ""
            preferences[TEMP_TYPE] = config.tempType
            preferences[DISTANCE_UNIT] = config.distanceUnit
            preferences[MFA_PREFERENCE] = config.mfaPreference
            preferences[CLIMATE_WARM] = config.climateTempWarm
            preferences[CLIMATE_COLD] = config.climateTempCold
            preferences[CAR_COLOR] = config.carColor
        }
    }

    suspend fun saveTokens(
        accessToken: String? = null,
        refreshToken: String? = null,
        rmToken: String? = null,
        deviceId: String? = null,
        clientUuid: String? = null,
        authId: String? = null
    ) {
        context.dataStore.edit { preferences ->
            accessToken?.let { preferences[ACCESS_TOKEN] = it }
            refreshToken?.let { preferences[REFRESH_TOKEN] = it }
            rmToken?.let { preferences[RM_TOKEN] = it }
            deviceId?.let { preferences[DEVICE_ID] = it }
            clientUuid?.let { preferences[CLIENT_UUID] = it }
            authId?.let { preferences[AUTH_ID] = it }
        }
    }

    val tokensFlow: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        mapOf(
            "access_token" to (preferences[ACCESS_TOKEN] ?: ""),
            "refresh_token" to (preferences[REFRESH_TOKEN] ?: ""),
            "rm_token" to (preferences[RM_TOKEN] ?: ""),
            "device_id" to (preferences[DEVICE_ID] ?: ""),
            "client_uuid" to (preferences[CLIENT_UUID] ?: ""),
            "auth_id" to (preferences[AUTH_ID] ?: "")
        )
    }

    suspend fun saveVin(vin: String) {
        context.dataStore.edit { preferences ->
            preferences[VIN] = vin
        }
    }

    val vinFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[VIN]
    }
}
