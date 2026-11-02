package com.example.iosportattempt1.models

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val manufacturer: String = "Kia",
    val region: String = "USA",
    val auth: AuthConfig = AuthConfig(),
    val vin: String? = null,
    val tempType: String = "F",
    val distanceUnit: String = "mi",
    val mfaPreference: String = "email",
    val climateTempWarm: Double = 71.0,
    val climateTempCold: Double = 66.0,
    val carColor: String = "White"
)

@Serializable
data class AuthConfig(
    val username: String = "",
    val password: String = "",
    val pin: String = "",
    val region: String = ""
)

@Serializable
data class ClimateRequest(
    val enable: Boolean = false,
    val temp: Double = 0.0,
    val frontDefrost: Boolean = false,
    val rearDefrost: Boolean = false,
    val steering: Boolean = false,
    val durationMinutes: Int = 15,
    val seatClimateOption: SeatClimateOption? = null
)

@Serializable
data class SeatClimateOption(
    val driver: Int = 0,
    val passenger: Int = 0,
    val rearLeft: Int = 0,
    val rearRight: Int = 0
)

@Serializable
data class ChargeLimit(
    val acPercent: Int = 80,
    val dcPercent: Int = 80
)

@Serializable
data class BluelinkStatus(
    val soc: Int = 0,
    val range: Int = 0,
    val isCharging: Boolean = false,
    val isPluggedIn: Boolean = false,
    val locked: Boolean = false,
    val climate: Boolean = false,
    val twelveSoc: Int = 0,
    val odometer: Int = 0,
    val lastStatusCheck: Long = 0,
    val lastRemoteStatusCheck: Long = 0,
    val chargingPower: Double = 0.0,
    val remainingChargeTimeMins: Int = 0,
    val location: Location? = null,
    val chargeLimit: ChargeLimit? = null
)

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class BluelinkCar(
    val id: String,
    val vin: String,
    val nickName: String,
    val modelName: String,
    val modelYear: String,
    val odometer: Int = 0,
    val modelColour: String = "",
    val modelTrim: String = ""
)
