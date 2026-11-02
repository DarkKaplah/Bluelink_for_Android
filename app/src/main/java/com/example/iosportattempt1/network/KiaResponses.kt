package com.example.iosportattempt1.network

import kotlinx.serialization.Serializable

@Serializable
data class KiaVehicleInfoResponse(
    val payload: KiaPayload? = null
)

@Serializable
data class KiaLoginResponse(
    val payload: KiaLoginPayload? = null
)

@Serializable
data class KiaLoginPayload(
    val otpKey: String? = null,
    val phoneVerifyStatus: Boolean? = null
)

@Serializable
data class KiaTransactionStatusResponse(
    val status: KiaStatus? = null,
    val payload: Map<String, Int>? = null
)

@Serializable
data class KiaStatus(
    val statusCode: Int? = null,
    val errorType: Int? = null,
    val errorCode: Int? = null,
    val errorMessage: String? = null
)

@Serializable
data class KiaPayload(
    val vehicleInfoList: List<KiaVehicleInfo>? = null,
    val vehicleStatusRpt: KiaVehicleStatusRpt? = null, // For forced refresh
    val vehicleSummary: List<KiaVehicleSummary>? = null
)

@Serializable
data class KiaVehicleSummary(
    val vehicleKey: String? = null,
    val vin: String? = null,
    val nickName: String? = null
)

@Serializable
data class KiaVehicleInfo(
    val lastVehicleInfo: KiaLastVehicleInfo? = null
)

@Serializable
data class KiaLastVehicleInfo(
    val vehicleStatusRpt: KiaVehicleStatusRpt? = null,
    val location: KiaLocation? = null
)

@Serializable
data class KiaVehicleStatusRpt(
    val vehicleStatus: KiaVehicleStatus? = null
)

@Serializable
data class KiaVehicleStatus(
    val evStatus: KiaEvStatus? = null,
    val doorLock: Boolean = false,
    val climate: KiaClimate? = null,
    val batteryStatus: KiaBatteryStatus? = null,
    val syncDate: KiaSyncDate? = null
)

@Serializable
data class KiaEvStatus(
    val batteryCharge: Boolean = false,
    val batteryStatus: Int = 0,
    val batteryPlugin: Int = 0,
    val realTimePower: Double = 0.0,
    val remainChargeTime: List<KiaRemainTime>? = null,
    val drvDistance: List<KiaDrvDistance>? = null,
    val targetSOC: List<KiaTargetSoc>? = null
)

@Serializable
data class KiaClimate(
    val airCtrl: Boolean = false
)

@Serializable
data class KiaBatteryStatus(
    val stateOfCharge: Int = 0
)

@Serializable
data class KiaSyncDate(
    val utc: String = ""
)

@Serializable
data class KiaRemainTime(
    val timeInterval: KiaValue? = null
)

@Serializable
data class KiaDrvDistance(
    val rangeByFuel: KiaRangeByFuel? = null
)

@Serializable
data class KiaRangeByFuel(
    val evModeRange: KiaValue? = null
)

@Serializable
data class KiaValue(
    val value: Double = 0.0
)

@Serializable
data class KiaTargetSoc(
    val plugType: Int = 0,
    val targetSOClevel: Int = 0
)

@Serializable
data class KiaLocation(
    val coord: KiaCoord? = null
)

@Serializable
data class KiaCoord(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)
