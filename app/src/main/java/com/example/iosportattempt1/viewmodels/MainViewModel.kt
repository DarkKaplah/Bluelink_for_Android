package com.example.iosportattempt1.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iosportattempt1.models.AppConfig
import com.example.iosportattempt1.models.BluelinkStatus
import com.example.iosportattempt1.repository.BluelinkRepository
import com.example.iosportattempt1.storage.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class UiState(
    val carName: String = "My Kia EV6",
    val status: BluelinkStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val mfaRequired: Boolean = false,
    val xid: String? = null,
    val otpKey: String? = null,
    val config: AppConfig = AppConfig()
)

class MainViewModel(
    private val repository: BluelinkRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreManager.appConfigFlow.collectLatest { config ->
                _uiState.value = _uiState.value.copy(config = config)
            }
        }
        viewModelScope.launch {
            dataStoreManager.tokensFlow.collectLatest { tokens ->
                val accessToken = tokens["access_token"]
                val authId = tokens["auth_id"]
                if (!accessToken.isNullOrEmpty() && !authId.isNullOrEmpty() && _uiState.value.status == null) {
                    refreshStatus()
                }
            }
        }
    }

    fun saveConfig(config: AppConfig) {
        viewModelScope.launch {
            dataStoreManager.saveAppConfig(config)
        }
    }

    fun lockUnlock() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val isLocked = _uiState.value.status?.locked ?: false
            val success = if (isLocked) repository.unlock() else repository.lock()
            if (success) refreshStatus(forceUpdate = true)
            else _uiState.value = _uiState.value.copy(isLoading = false, error = "Lock/Unlock failed")
        }
    }

    fun toggleClimate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val isClimateOn = _uiState.value.status?.climate ?: false
            val success = if (isClimateOn) repository.climateOff() else repository.climateOn(_uiState.value.config.climateTempWarm)
            if (success) refreshStatus(forceUpdate = true)
            else _uiState.value = _uiState.value.copy(isLoading = false, error = "Climate toggle failed")
        }
    }

    fun toggleCharge() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val isCharging = _uiState.value.status?.isCharging ?: false
            val success = if (isCharging) repository.stopCharge() else repository.startCharge()
            if (success) refreshStatus(forceUpdate = true)
            else _uiState.value = _uiState.value.copy(isLoading = false, error = "Charging toggle failed")
        }
    }

    fun refreshStatus(forceUpdate: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val status = repository.getStatus(forceUpdate)
                _uiState.value = _uiState.value.copy(status = status, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.login()
            if (result?.startsWith("MFA_REQUIRED") == true) {
                val parts = result.split(":")
                val xid = parts[1]
                val otpKey = parts[2]
                _uiState.value = _uiState.value.copy(
                    mfaRequired = true, 
                    xid = xid, 
                    otpKey = otpKey,
                    isLoading = false
                )
            } else if (result == "SUCCESS") {
                _uiState.value = _uiState.value.copy(mfaRequired = false, isLoading = false)
                refreshStatus()
            } else {
                _uiState.value = _uiState.value.copy(error = "Login Failed: $result", isLoading = false)
            }
        }
    }

    fun verifyMfa(code: String) {
        val xid = _uiState.value.xid ?: return
        val otpKey = _uiState.value.otpKey ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = repository.verifyMfa(code, xid, otpKey)
            if (success) {
                _uiState.value = _uiState.value.copy(mfaRequired = false, isLoading = false)
                refreshStatus()
            } else {
                _uiState.value = _uiState.value.copy(error = "MFA Verification Failed", isLoading = false)
            }
        }
    }
}
