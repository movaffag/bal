package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SubscriptionEntity
import com.example.data.V2RayNodeEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun V2RayScreen(
    viewModel: V2ViewModel,
    modifier: Modifier = Modifier
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val allNodes by viewModel.allNodes.collectAsStateWithLifecycle()
    val isPinging by viewModel.isPinging.collectAsStateWithLifecycle()
    val pingProgress by viewModel.pingProgress.collectAsStateWithLifecycle()
    val nextRefreshSeconds by viewModel.nextRefreshSeconds.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val activeNode by viewModel.activeNode.collectAsStateWithLifecycle()

    val durationSeconds by viewModel.connectionDurationSeconds.collectAsStateWithLifecycle()
    val downSpeed by viewModel.downloadSpeedKb.collectAsStateWithLifecycle()
    val upSpeed by viewModel.uploadSpeedKb.collectAsStateWithLifecycle()
    val totalDownloaded by viewModel.totalDownloadedMb.collectAsStateWithLifecycle()
    val totalUploaded by viewModel.totalUploadedMb.collectAsStateWithLifecycle()

    var showSubManager by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Block
            HeaderView(
                onManageSubsClicked = { showSubManager = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Active Connection Card (Bento Height 160.dp)
            ActiveConnectionCard(
                connectionState = connectionState,
                activeNode = activeNode,
                durationSeconds = durationSeconds,
                downloadSpeed = downSpeed,
                uploadSpeed = upSpeed,
                totalDownloaded = totalDownloaded,
                totalUploaded = totalUploaded,
                onDisconnect = { viewModel.disconnect() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bento Grid Metrics Row (Next ping & Total Nodes)
            MetricsBentoGrid(
                timeRemainingSeconds = nextRefreshSeconds,
                totalNodesCount = allNodes.size,
                isPinging = isPinging,
                pingProgress = pingProgress,
                activeSubsCount = subscriptions.size,
                onForcePing = { viewModel.triggerPingSweep() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // List Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "OPTIMAL NODES / سرورهای برتر",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = BentoTextDarkSlate,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                
                if (isPinging) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = BentoAccentBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${pingProgress.first}/${pingProgress.second}",
                            fontSize = 11.sp,
                            color = BentoAccentBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "مرتب‌سازی خودکار",
                        fontSize = 11.sp,
                        color = BentoTextMuted,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Node List
            Box(modifier = Modifier.weight(1f)) {
                if (allNodes.isEmpty()) {
                    EmptyNodesPlaceholder(onAddClicked = { showSubManager = true })
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = allNodes,
                            key = { _, node -> node.id }
                        ) { index, node ->
                            val isActive = activeNode?.id == node.id
                            NodeListItem(
                                index = index + 1,
                                node = node,
                                isActive = isActive,
                                connectionState = connectionState,
                                onClick = {
                                    if (isActive && connectionState == ConnectionState.CONNECTED) {
                                        viewModel.disconnect()
                                    } else {
                                        viewModel.connectToNode(node)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Sub-manager Sheet
        if (showSubManager) {
            SubscriptionManagerDialog(
                viewModel = viewModel,
                onDismiss = { showSubManager = false }
            )
        }
    }
}

@Composable
fun HeaderView(
    onManageSubsClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "AUTO-PING V2.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = BentoAccentBlue
                )
            )
            Text(
                text = "V2Flow Pro",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary
                )
            )
        }

        // Manage Subs Pill
        IconButton(
            onClick = onManageSubsClicked,
            modifier = Modifier
                .clip(CircleShape)
                .background(BentoCardSecondary)
                .testTag("manage_subs_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "تنظیمات ساب‌لینک‌ها",
                tint = BentoTextDarkSlate
            )
        }
    }
}

@Composable
fun ActiveConnectionCard(
    connectionState: ConnectionState,
    activeNode: V2RayNodeEntity?,
    durationSeconds: Int,
    downloadSpeed: Float,
    uploadSpeed: Float,
    totalDownloaded: Float,
    totalUploaded: Float,
    onDisconnect: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING

    val cardBg = if (isConnected || isConnecting) BentoHighlightLightBlue else BentoCardSecondary
    val textColor = if (isConnected || isConnecting) BentoTextOnHighlight else BentoTextPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("active_connection_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top part
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) "اتصال فعال / Active Node" else if (isConnecting) "در حال اتصال به" else "اتصال برقرار نیست",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeNode?.name ?: "سرور انتخاب نشده است",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Status pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isConnected) BentoAccentBlue else if (isConnecting) Color(0xFFF97316) else Color(0x2074777F),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (isConnected) "CONNECTED" else if (isConnecting) "CONNECTING" else "DISCONNECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isConnected || isConnecting) Color.White else BentoTextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Middle & Bottom parts combined
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left element: Big latency text or idle indicator
                    Column {
                        if (isConnected && activeNode != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = if (activeNode.latencyMs >= 0) "${activeNode.latencyMs}" else "84",
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Light,
                                    fontStyle = FontStyle.Italic,
                                    color = textColor
                                )
                                Text(
                                    text = "ms",
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                                )
                            }
                            Text(
                                text = "تاخیر / LATENCY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        } else if (isConnecting) {
                            Text(
                                text = "...",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "برقراری دسترسی",
                                fontSize = 9.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "0.00",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraLight,
                                color = BentoTextMuted
                            )
                            Text(
                                text = "انتخاب یک سرور برای اتصال",
                                fontSize = 10.sp,
                                color = BentoTextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Right element: Animated charts or download speed stats
                    if (isConnected) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatDuration(durationSeconds),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "↓ ${formatSpeed(downloadSpeed)}  ↑ ${formatSpeed(uploadSpeed)}",
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "حجم: ${String.format("%.1f", totalDownloaded)} MB دریافت شده",
                                    fontSize = 9.sp,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            }

                            // Signal bouncing bars
                            BouncingSignalBars()
                        }
                    } else if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = Color(0xFFF97316)
                        )
                    } else {
                        // Decorator graphic
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            (1..5).forEach { i ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((10 + i * 4).dp)
                                        .clip(CircleShape)
                                        .background(BentoTextMuted.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricsBentoGrid(
    timeRemainingSeconds: Int,
    totalNodesCount: Int,
    isPinging: Boolean,
    pingProgress: Pair<Int, Int>,
    activeSubsCount: Int,
    onForcePing: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Next Auto-Ping Code Box (Width = Weight 1)
        Card(
            modifier = Modifier
                .weight(1f)
                .height(105.dp)
                .clickable { onForcePing() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoCardSecondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PING COOLDOWN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDarkSlate,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "اسکن سریع",
                        tint = if (isPinging) BentoAccentBlue else BentoTextDarkSlate,
                        modifier = Modifier.size(12.dp)
                    )
                }

                if (isPinging) {
                    Column {
                        Text(
                            text = "پینگ ${pingProgress.first}/${pingProgress.second}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoAccentBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (pingProgress.second > 0) {
                                    pingProgress.first.toFloat() / pingProgress.second.toFloat()
                                } else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = BentoAccentBlue,
                            trackColor = BentoTextMuted.copy(alpha = 0.2f)
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = formatDuration(timeRemainingSeconds).substring(3),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = BentoTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress of countdown (5 minutes = 300s)
                        val progress = (300 - timeRemainingSeconds).toFloat() / 300f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = BentoAccentBlue,
                            trackColor = BentoTextMuted.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Card 2: Total loaded Nodes stats
        Card(
            modifier = Modifier
                .weight(1f)
                .height(105.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoCardSecondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TOTAL PROXY NODES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextDarkSlate,
                    letterSpacing = 0.5.sp
                )

                Column {
                    Text(
                        text = "$totalNodesCount",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "از $activeSubsCount لینک اشتراک",
                        fontSize = 10.sp,
                        color = BentoTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun BouncingSignalBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "bars")
    
    // Animate heights of bar elements
    val barHeight1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b1"
    )

    val barHeight2 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b2"
    )

    val barHeight3 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b3"
    )

    val barHeight4 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 44f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b4"
    )

    Row(
        modifier = Modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.width(3.dp).height(barHeight1.dp).clip(CircleShape).background(BentoAccentBlue))
        Box(modifier = Modifier.width(3.dp).height(barHeight2.dp).clip(CircleShape).background(BentoAccentBlue))
        Box(modifier = Modifier.width(3.dp).height(barHeight3.dp).clip(CircleShape).background(BentoAccentBlue))
        Box(modifier = Modifier.width(3.dp).height(barHeight4.dp).clip(CircleShape).background(BentoAccentBlue))
        Box(modifier = Modifier.width(3.dp).height(12.dp).clip(CircleShape).background(BentoTextOnHighlight.copy(alpha = 0.2f)))
    }
}

@Composable
fun NodeListItem(
    index: Int,
    node: V2RayNodeEntity,
    isActive: Boolean,
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    val itemBorder = if (isActive) BentoAccentBlue else BentoBorder
    val itemBg = if (isActive) BentoHighlightLightBlue.copy(alpha = 0.2f) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(itemBg)
            .border(1.dp, itemBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("node_item"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Rounded Index
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isActive) BentoAccentBlue else BentoLightGrayBg),
                contentAlignment = Alignment.Center
            ) {
                if (isActive && connectionState == ConnectionState.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "$index",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else BentoTextDarkSlate
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Node Title Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Protocol label
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BentoLightGrayBg,
                    ) {
                        Text(
                            text = node.protocol,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextMuted,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${node.address}:${node.port}",
                        fontSize = 10.sp,
                        color = BentoTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Latency details
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                node.latencyMs == -1 -> {
                    Text(
                        text = "بررسی نشده",
                        fontSize = 11.sp,
                        color = BentoTextMuted
                    )
                }
                node.latencyMs == -2 -> {
                    Text(
                        text = "Timeout",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626) // Red
                    )
                }
                else -> {
                    val color = when {
                        node.latencyMs < 120 -> Color(0xFF059669) // Emerald Green
                        node.latencyMs < 250 -> Color(0xFFD97706) // Orange
                        else -> Color(0xFFDC2626) // Red
                    }
                    Text(
                        text = "${node.latencyMs} ms",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagerDialog(
    viewModel: V2ViewModel,
    onDismiss: () -> Unit
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val isAdding by viewModel.isAddingSubscription.collectAsStateWithLifecycle()
    val addError by viewModel.addSubscriptionError.collectAsStateWithLifecycle()

    var subUrl by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مدیریت ساب‌لینک‌ها",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = BentoTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Paste zone
                OutlinedTextField(
                    value = subUrl,
                    onValueChange = { 
                        subUrl = it
                        viewModel.clearAddSubscriptionError()
                    },
                    label = { Text("لینک ساب V2Ray") },
                    placeholder = { Text("https://example.com/sub/...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sub_url_field"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clipboardText ->
                                    subUrl = clipboardText
                                    viewModel.clearAddSubscriptionError()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "چسباندن از حافظه"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subName,
                    onValueChange = { subName = it },
                    label = { Text("نام اشتراک (اختیاری)") },
                    placeholder = { Text("سرورهای من") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (addError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = addError ?: "",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.addSubscription(subUrl, subName) {
                            subUrl = ""
                            subName = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_sub_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoAccentBlue),
                    enabled = !isAdding && subUrl.isNotEmpty()
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "بارگیری و اضافه کردن",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = BentoBorder)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "اشتراک‌های فعلی (${subscriptions.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextDarkSlate
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    if (subscriptions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "هیچ اشتراکی اضافه نشده است",
                                fontSize = 11.sp,
                                color = BentoTextMuted
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subscriptions.size) { index ->
                                val sub = subscriptions[index]
                                SubscriptionItem(
                                    sub = sub,
                                    onDelete = { viewModel.deleteSubscription(sub.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionItem(
    sub: SubscriptionEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BentoLightGrayBg)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sub.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub.url,
                fontSize = 9.sp,
                color = BentoTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "حذف ساب‌لینک",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun EmptyNodesPlaceholder(
    onAddClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "افزودن ساب‌لینک",
                tint = BentoAccentBlue,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "هنوز سروری بارگذاری نشده است",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "برای شروع، یک ساب‌لینک V2Ray اضافه کنید تا سرورها به طور خودکار استخراج و مرتب‌سازی شوند.",
                fontSize = 11.sp,
                color = BentoTextMuted,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClicked,
                colors = ButtonDefaults.buttonColors(containerColor = BentoAccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "افزودن اولین ساب‌لینک",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("00:%02d:%02d", m, s)
    }
}

fun formatSpeed(speedKb: Float): String {
    return if (speedKb >= 1024f) {
        val speedMb = speedKb / 1024f
        String.format("%.2f MB/s", speedMb)
    } else {
        String.format("%.1f KB/s", speedKb)
    }
}
