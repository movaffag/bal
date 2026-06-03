package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class V2ViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val repository = V2Repository(db.v2rayDao())

    // Streams from DB
    val subscriptions: StateFlow<List<SubscriptionEntity>> = repository.subscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNodes: StateFlow<List<V2RayNodeEntity>> = repository.allNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Loading / Progress UI States
    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _pingProgress = MutableStateFlow(Pair(0, 0)) // current to total
    val pingProgress: StateFlow<Pair<Int, Int>> = _pingProgress.asStateFlow()

    private val _addSubscriptionError = MutableStateFlow<String?>(null)
    val addSubscriptionError: StateFlow<String?> = _addSubscriptionError.asStateFlow()

    private val _isAddingSubscription = MutableStateFlow(false)
    val isAddingSubscription: StateFlow<Boolean> = _isAddingSubscription.asStateFlow()

    // 5-Minutes Autorefresh Ping Timer State (300 seconds)
    private val _nextRefreshSeconds = MutableStateFlow(300)
    val nextRefreshSeconds: StateFlow<Int> = _nextRefreshSeconds.asStateFlow()

    // Connection states
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeNode = MutableStateFlow<V2RayNodeEntity?>(null)
    val activeNode: StateFlow<V2RayNodeEntity?> = _activeNode.asStateFlow()

    // Telemetry stats updated every second when connected
    private val _connectionDurationSeconds = MutableStateFlow(0)
    val connectionDurationSeconds: StateFlow<Int> = _connectionDurationSeconds.asStateFlow()

    private val _downloadSpeedKb = MutableStateFlow(0f) // KB/s
    val downloadSpeedKb: StateFlow<Float> = _downloadSpeedKb.asStateFlow()

    private val _uploadSpeedKb = MutableStateFlow(0f) // KB/s
    val uploadSpeedKb: StateFlow<Float> = _uploadSpeedKb.asStateFlow()

    private val _totalDownloadedMb = MutableStateFlow(0f) // MBs accumulated
    val totalDownloadedMb: StateFlow<Float> = _totalDownloadedMb.asStateFlow()

    private val _totalUploadedMb = MutableStateFlow(0f) // MBs accumulated
    val totalUploadedMb: StateFlow<Float> = _totalUploadedMb.asStateFlow()

    private var countdownJob: Job? = null
    private var telemetryJob: Job? = null

    init {
        startRefreshCountdown()
        
        // Auto-ping nodes once we load them if database has nodes but they are un-pinged
        viewModelScope.launch {
            allNodes.first { it.isNotEmpty() }
            if (allNodes.value.all { it.latencyMs == -1 }) {
                triggerPingSweep()
            }
        }
    }

    private fun startRefreshCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _nextRefreshSeconds.value
                if (current <= 1) {
                    _nextRefreshSeconds.value = 300
                    triggerPingSweep()
                } else {
                    _nextRefreshSeconds.value = current - 1
                }
            }
        }
    }

    fun triggerPingSweep() {
        if (_isPinging.value) return
        viewModelScope.launch {
            _isPinging.value = true
            _pingProgress.value = Pair(0, allNodes.value.size)
            
            repository.pingAllNodes(allNodes.value) { completed, total ->
                _pingProgress.value = Pair(completed, total)
            }
            
            _isPinging.value = false
            _nextRefreshSeconds.value = 300 // Reset timer because we just pinged
        }
    }

    fun addSubscription(url: String, customName: String, onSuccess: () -> Unit) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) {
            _addSubscriptionError.value = "آدرس ساب‌لینک نمی‌تواند خالی باشد"
            return
        }
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            _addSubscriptionError.value = "آدرس ساب‌لینک معتبر نیست (باید با http یا https شروع شود)"
            return
        }

        viewModelScope.launch {
            _isAddingSubscription.value = true
            _addSubscriptionError.value = null
            
            val result = repository.fetchAndAddSubscription(cleanUrl, customName)
            _isAddingSubscription.value = false
            
            if (result.isSuccess) {
                _addSubscriptionError.value = null
                onSuccess()
                // Auto ping sweep straight after imports to instantly bring best to top!
                triggerPingSweep()
            } else {
                _addSubscriptionError.value = result.exceptionOrNull()?.message ?: "خطا در بارگیری ساب‌لینک"
            }
        }
    }

    fun deleteSubscription(id: Int) {
        viewModelScope.launch {
            // If active connection belongs to a node of this sub, disconnect first
            val currentActive = _activeNode.value
            if (currentActive != null && currentActive.subscriptionId == id) {
                disconnect()
            }
            repository.deleteSubscription(id)
        }
    }

    fun connectToNode(node: V2RayNodeEntity) {
        viewModelScope.launch {
            disconnect() // disconnect any active before connecting new
            
            _activeNode.value = node
            _connectionState.value = ConnectionState.CONNECTING
            
            // Simulating authentic handshakes, tunnel preparation, routing setup
            delay(1200)
            _connectionState.value = ConnectionState.CONNECTED
            
            startTelemetry()
        }
    }

    fun disconnect() {
        telemetryJob?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTED
        _activeNode.value = null
        _connectionDurationSeconds.value = 0
        _downloadSpeedKb.value = 0f
        _uploadSpeedKb.value = 0f
        _totalDownloadedMb.value = 0f
        _totalUploadedMb.value = 0f
    }

    private fun startTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (_connectionState.value == ConnectionState.CONNECTED) {
                delay(1000)
                _connectionDurationSeconds.value += 1
                
                // Simulate throughput calculations with natural micro-fluctuations
                // e.g. normal proxy usage averages 150KB/s - 3MB/s download, 10KB/s - 150KB/s upload
                val downFactor = if (Math.random() > 0.85) 4.2f else 1.1f // sudden spikes
                val upFactor = if (Math.random() > 0.9) 3.1f else 0.8f
                
                val currentDown = (180f + (Math.random() * 450f).toFloat()) * downFactor
                val currentUp = (12f + (Math.random() * 45f).toFloat()) * upFactor
                
                _downloadSpeedKb.value = currentDown
                _uploadSpeedKb.value = currentUp
                
                // Accumulate volume in Megabytes (KB / 1024)
                _totalDownloadedMb.value += (currentDown / 1024f)
                _totalUploadedMb.value += (currentUp / 1024f)
            }
        }
    }

    fun clearAddSubscriptionError() {
        _addSubscriptionError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        telemetryJob?.cancel()
    }
}
