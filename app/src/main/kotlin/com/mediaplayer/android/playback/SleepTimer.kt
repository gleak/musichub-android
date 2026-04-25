package com.mediaplayer.android.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepTimer(private val scope: CoroutineScope) {

    private var job: Job? = null

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun set(minutes: Int, onExpire: () -> Unit) {
        job?.cancel()
        if (minutes <= 0) {
            _isActive.value = false
            return
        }
        _isActive.value = true
        job = scope.launch {
            delay(minutes * 60_000L)
            _isActive.value = false
            onExpire()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _isActive.value = false
    }
}
