package com.example.iosportattempt1.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.iosportattempt1.repository.BluelinkRepository
import com.example.iosportattempt1.storage.DataStoreManager

class ViewModelFactory(
    private val repository: BluelinkRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
