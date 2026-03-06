package com.example.triade_monitoramento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TemperatureVmFactory(
    private val repo: TemperatureRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemperatureViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TemperatureViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
