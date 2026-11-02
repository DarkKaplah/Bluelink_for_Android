package com.example.iosportattempt1.network

import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface BluelinkApi {
    @POST("prof/authUser")
    suspend fun login(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<KiaLoginResponse>

    @POST("cmm/sendOTP")
    suspend fun sendOTP(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<KiaLoginResponse>

    @POST("cmm/verifyOTP")
    suspend fun verifyOTP(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<KiaLoginResponse>

    @GET("ownr/gvl")
    suspend fun getVehicles(
        @HeaderMap headers: Map<String, String>
    ): Response<KiaVehicleInfoResponse>

    @POST("cmm/gvi")
    suspend fun getVehicleInfo(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<KiaVehicleInfoResponse>

    @POST("rems/rvs")
    suspend fun refreshVehicleStatus(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<KiaVehicleInfoResponse>

    @POST("cmm/gts")
    suspend fun getTransactionStatus(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<KiaTransactionStatusResponse>

    @GET("rems/door/lock")
    suspend fun lock(
        @HeaderMap headers: Map<String, String>
    ): Response<ResponseBody>

    @GET("rems/door/unlock")
    suspend fun unlock(
        @HeaderMap headers: Map<String, String>
    ): Response<ResponseBody>

    @GET("evc/charge")
    suspend fun startCharge(
        @HeaderMap headers: Map<String, String>,
        @Query("chargeRatio") chargeRatio: Int = 100
    ): Response<ResponseBody>

    @GET("evc/cancel")
    suspend fun stopCharge(
        @HeaderMap headers: Map<String, String>
    ): Response<ResponseBody>

    @POST("rems/start")
    suspend fun climateOn(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<ResponseBody>

    @GET("rems/stop")
    suspend fun climateOff(
        @HeaderMap headers: Map<String, String>
    ): Response<ResponseBody>

    @POST("evc/sts")
    suspend fun setChargeLimit(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<ResponseBody>
}
