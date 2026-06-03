package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.V2VpnManager
import com.example.V2RayVpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class PingTarget(val displayName: String) {
    DIRECT("دستگاه به سرور (Direct)"),
    YOUTUBE("یوتیوب (YouTube)"),
    INSTAGRAM("اینستاگرام (Instagram)"),
    AI_SERVICES("هوش مصنوعی (AI / Gemini)")
}

class V2ViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val repository = V2Repository(db.v2rayDao())
    private val prefs = application.getSharedPreferences("v2flow_prefs", android.content.Context.MODE_PRIVATE)

    // SharedPreferences persistent states
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isThreeDMode = MutableStateFlow(prefs.getBoolean("is_threed_mode", false))
    val isThreeDMode: StateFlow<Boolean> = _isThreeDMode.asStateFlow()

    private val _isFullTunnel = MutableStateFlow(prefs.getBoolean("is_full_tunnel", true))
    val isFullTunnel: StateFlow<Boolean> = _isFullTunnel.asStateFlow()

    private val _pingTarget = MutableStateFlow(
        try {
            PingTarget.valueOf(prefs.getString("ping_target", PingTarget.DIRECT.name) ?: PingTarget.DIRECT.name)
        } catch (_: Exception) {
            PingTarget.DIRECT
        }
    )
    val pingTarget: StateFlow<PingTarget> = _pingTarget.asStateFlow()

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        prefs.edit().putBoolean("is_dark_mode", newValue).apply()
    }

    fun toggleThreeDMode() {
        val newValue = !_isThreeDMode.value
        _isThreeDMode.value = newValue
        prefs.edit().putBoolean("is_threed_mode", newValue).apply()
    }

    fun toggleFullTunnel() {
        val newValue = !_isFullTunnel.value
        _isFullTunnel.value = newValue
        prefs.edit().putBoolean("is_full_tunnel", newValue).apply()
    }

    fun setPingTarget(target: PingTarget) {
        _pingTarget.value = target
        prefs.edit().putString("ping_target", target.name).apply()
        triggerPingSweep() // Re-ping on setting change to instantly show target pings!
    }

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

    private val _addSingleConfigError = MutableStateFlow<String?>(null)
    val addSingleConfigError: StateFlow<String?> = _addSingleConfigError.asStateFlow()

    private val _isAddingSingleConfig = MutableStateFlow(false)
    val isAddingSingleConfig: StateFlow<Boolean> = _isAddingSingleConfig.asStateFlow()

    // 5-Minutes Autorefresh Ping Timer State (300 seconds)
    private val _nextRefreshSeconds = MutableStateFlow(300)
    val nextRefreshSeconds: StateFlow<Int> = _nextRefreshSeconds.asStateFlow()

    // Connection states tied directly to the real system VpnService
    val connectionState: StateFlow<ConnectionState> = V2VpnManager.connectionState
    val activeNode: StateFlow<V2RayNodeEntity?> = V2VpnManager.activeNode
    val connectionDurationSeconds: StateFlow<Int> = V2VpnManager.connectionDurationSeconds
    val downloadSpeedKb: StateFlow<Float> = V2VpnManager.downloadSpeedKb
    val uploadSpeedKb: StateFlow<Float> = V2VpnManager.uploadSpeedKb
    val totalDownloadedMb: StateFlow<Float> = V2VpnManager.totalDownloadedMb
    val totalUploadedMb: StateFlow<Float> = V2VpnManager.totalUploadedMb

    private val _vpnPermissionIntent = MutableStateFlow<Intent?>(null)
    val vpnPermissionIntent: StateFlow<Intent?> = _vpnPermissionIntent.asStateFlow()

    private var pendingNodeToConnect: V2RayNodeEntity? = null

    private var countdownJob: Job? = null

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
            
            repository.pingAllNodes(allNodes.value, target = "SMART") { completed, total ->
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

    fun addSingleConfig(url: String, customName: String, onSuccess: () -> Unit) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) {
            _addSingleConfigError.value = "آدرس کانفیگ نمی‌تواند خالی باشد"
            return
        }

        viewModelScope.launch {
            _isAddingSingleConfig.value = true
            _addSingleConfigError.value = null
            
            val result = repository.addSingleConfig(cleanUrl, customName)
            _isAddingSingleConfig.value = false
            
            if (result.isSuccess) {
                _addSingleConfigError.value = null
                onSuccess()
                // Auto ping sweep straight after imports to instantly bring best to top!
                triggerPingSweep()
            } else {
                _addSingleConfigError.value = result.exceptionOrNull()?.message ?: "خطا در افزودن کانفیگ"
            }
        }
    }

    fun clearAddSingleConfigError() {
        _addSingleConfigError.value = null
    }

    fun deleteSubscription(id: Int) {
        viewModelScope.launch {
            // If active connection belongs to a node of this sub, disconnect first
            val currentActive = activeNode.value
            if (currentActive != null && currentActive.subscriptionId == id) {
                disconnect()
            }
            repository.deleteSubscription(id)
        }
    }

    fun connectToNode(node: V2RayNodeEntity) {
        viewModelScope.launch {
            disconnect() // Stop any active first
            
            pendingNodeToConnect = node
            val prepareIntent = VpnService.prepare(getApplication())
            if (prepareIntent != null) {
                // Return intent back through flow to prompt the OS VPN permission popup
                _vpnPermissionIntent.value = prepareIntent
            } else {
                // VPN was already approved by user in local settings
                startVpnTunnel(node)
            }
        }
    }

    fun onVpnPermissionResult(isGranted: Boolean) {
        val node = pendingNodeToConnect
        _vpnPermissionIntent.value = null
        pendingNodeToConnect = null
        
        if (isGranted && node != null) {
            startVpnTunnel(node)
        }
    }

    private fun startVpnTunnel(node: V2RayNodeEntity) {
        val context = getApplication<Application>()
        val intent = Intent(context, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_CONNECT
            putExtra(V2RayVpnService.EXTRA_NODE_ID, node.id)
            putExtra(V2RayVpnService.EXTRA_NODE_NAME, node.name)
            putExtra(V2RayVpnService.EXTRA_NODE_ADDRESS, node.address)
            putExtra(V2RayVpnService.EXTRA_NODE_PORT, node.port)
            putExtra(V2RayVpnService.EXTRA_NODE_PROTOCOL, node.protocol)
            putExtra(V2RayVpnService.EXTRA_FULL_TUNNEL, isFullTunnel.value)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun disconnect() {
        val context = getApplication<Application>()
        val intent = Intent(context, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun clearAddSubscriptionError() {
        _addSubscriptionError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
