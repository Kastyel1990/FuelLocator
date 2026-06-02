package com.crimeafuel.app.util

import kotlinx.coroutines.flow.MutableStateFlow

object SharedIntentManager {
    val sharedText = MutableStateFlow<String?>(null)
    
    fun clear() {
        sharedText.value = null
    }
}
