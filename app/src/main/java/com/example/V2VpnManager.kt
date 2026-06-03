package com.example

import com.example.data.V2RayNodeEntity
import com.example.ui.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object V2VpnManager {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _activeNode = MutableStateFlow<V2RayNodeEntity?>(null)
    val activeNode = _activeNode.asStateFlow()

    private val _downloadSpeedKb = MutableStateFlow(0f)
    val downloadSpeedKb = _downloadSpeedKb.asStateFlow()

    private val _uploadSpeedKb = MutableStateFlow(0f)
    val uploadSpeedKb = _uploadSpeedKb.asStateFlow()

    private val _totalDownloadedMb = MutableStateFlow(0f)
    val totalDownloadedMb = _totalDownloadedMb.asStateFlow()

    private val _totalUploadedMb = MutableStateFlow(0f)
    val totalUploadedMb = _totalUploadedMb.asStateFlow()

    private val _connectionDurationSeconds = MutableStateFlow(0)
    val connectionDurationSeconds = _connectionDurationSeconds.asStateFlow()

    fun updateState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun updateActiveNode(node: V2RayNodeEntity?) {
        _activeNode.value = node
    }

    fun updateSpeed(downKb: Float, upKb: Float) {
        _downloadSpeedKb.value = downKb
        _uploadSpeedKb.value = upKb
        _totalDownloadedMb.value += (downKb / 1024f)
        _totalUploadedMb.value += (upKb / 1024f)
    }

    fun incrementDuration() {
        _connectionDurationSeconds.value += 1
    }

    fun resetStats() {
        _connectionDurationSeconds.value = 0
        _downloadSpeedKb.value = 0f
        _uploadSpeedKb.value = 0f
        _totalDownloadedMb.value = 0f
        _totalUploadedMb.value = 0f
    }
}
