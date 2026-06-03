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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
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

data class BentoColors(
    val bg: Color,
    val textPrimary: Color,
    val accentBlue: Color,
    val highlightLightBlue: Color,
    val cardSecondary: Color,
    val textOnHighlight: Color,
    val textDarkSlate: Color,
    val textMuted: Color,
    val border: Color,
    val lightGrayBg: Color
)

fun getBentoColors(darkTheme: Boolean): BentoColors {
    return if (darkTheme) {
        BentoColors(
            bg = Color(0xFF0C0C0F), // Deep space black slate canvas
            textPrimary = Color(0xFFF2F2F7),
            accentBlue = Color(0xFF3393FF), // Luminous neon tech blue
            highlightLightBlue = Color(0xFF14223C), // Muted dark blue highlight
            cardSecondary = Color(0xFF18191E), // High-elevation card backing
            textOnHighlight = Color(0xFFE5F1FF),
            textDarkSlate = Color(0xFFC5C6D1),
            textMuted = Color(0xFF8E909B),
            border = Color(0xFF28292E),
            lightGrayBg = Color(0xFF14151B)
        )
    } else {
        BentoColors(
            bg = Color(0xFFF7F8FC), // Clean bright elegant bento grid canvas
            textPrimary = Color(0xFF1B1B1F),
            accentBlue = Color(0xFF0061A4),
            highlightLightBlue = Color(0xFFD1E4FF),
            cardSecondary = Color(0xFFE1E2EC),
            textOnHighlight = Color(0xFF001D36),
            textDarkSlate = Color(0xFF44474E),
            textMuted = Color(0xFF74777F),
            border = Color(0xFFDCDCE5),
            lightGrayBg = Color(0xFFF0F0F7)
        )
    }
}

fun Modifier.bento3D(
    enabled: Boolean,
    containerColor: Color,
    borderColor: Color = Color(0xFF1B1B1F),
    depth: androidx.compose.ui.unit.Dp = 5.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp
): Modifier {
    if (!enabled) {
        return this
            .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(cornerRadius))
            .background(containerColor, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
    }
    return this
        .drawBehind {
            val strokeWidthPx = 1.5.dp.toPx()
            val depthPx = depth.toPx()
            val cornerRadiusPx = cornerRadius.toPx()
            
            // 1. Draw 3D Underlayer Shadow
            drawRoundRect(
                color = borderColor,
                topLeft = androidx.compose.ui.geometry.Offset(depthPx, depthPx),
                size = androidx.compose.ui.geometry.Size(size.width - depthPx, size.height - depthPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
            
            // 2. Draw foreground surface fill
            drawRoundRect(
                color = containerColor,
                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                size = androidx.compose.ui.geometry.Size(size.width - depthPx, size.height - depthPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
            
            // 3. Draw surrounding border stroke
            drawRoundRect(
                color = borderColor,
                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                size = androidx.compose.ui.geometry.Size(size.width - depthPx, size.height - depthPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
            )
        }
        .padding(bottom = depth, end = depth)
}

@Composable
fun SettingsBentoPanel(
    viewModel: V2ViewModel,
    colors: BentoColors,
    isThreeDMode: Boolean
) {
    val isFullTunnel by viewModel.isFullTunnel.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bento3D(isThreeDMode, colors.cardSecondary, colors.textPrimary, cornerRadius = 24.dp)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تنظیمات هوشمند و شخصی‌سازی",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accentBlue
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = colors.accentBlue.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "BENTO Pro",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accentBlue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: Quick toggles for tunnels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Option 1: تانل کامل (VPN)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .bento3D(isThreeDMode, if (isFullTunnel) colors.highlightLightBlue else colors.lightGrayBg, colors.textPrimary, depth = 3.dp, cornerRadius = 14.dp)
                    .clickable { if (!isFullTunnel) viewModel.toggleFullTunnel() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🌐",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "تانل کامل (VPN)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFullTunnel) colors.accentBlue else colors.textPrimary
                    )
                }
            }

            // Option 2: پروکسی تلگرام / مرورگر
            Box(
                modifier = Modifier
                     .weight(1f)
                     .height(44.dp)
                     .bento3D(isThreeDMode, if (!isFullTunnel) colors.highlightLightBlue else colors.lightGrayBg, colors.textPrimary, depth = 3.dp, cornerRadius = 14.dp)
                     .clickable { if (isFullTunnel) viewModel.toggleFullTunnel() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📱",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "پروکسی محلی",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isFullTunnel) colors.accentBlue else colors.textPrimary
                    )
                }
            }
        }

        // Display local proxy settings when selected
        if (!isFullTunnel) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.lightGrayBg, shape = RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "⚙️ مشخصات پروکسی تلگرام و برنامه‌ها",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.accentBlue
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آدرس آی‌پی (IP):",
                        fontSize = 9.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "127.0.0.1",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentBlue
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "پورت SOCKS5 (تلگرام):",
                        fontSize = 9.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "10808",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentBlue
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "پورت HTTP (مرورگر):",
                        fontSize = 9.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "10809",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentBlue
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val context = androidx.compose.ui.platform.LocalContext.current
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                var copiedFeedback by remember { mutableStateOf(false) }

                androidx.compose.runtime.LaunchedEffect(copiedFeedback) {
                    if (copiedFeedback) {
                        delay(2000)
                        copiedFeedback = false
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Copy Socks5 Link Button
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .height(32.dp)
                            .background(colors.accentBlue, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("https://t.me/socks?server=127.0.0.1&port=10808"))
                                copiedFeedback = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (copiedFeedback) "کپی شد ✅" else "🔗 کپی لینک پروکسی تلگرام",
                            fontSize = 8.6.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Direct Telegram Deep Link Open Button
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .height(32.dp)
                            .border(1.dp, colors.accentBlue, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("tg://socks?server=127.0.0.1&port=10808")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://t.me/socks?server=127.0.0.1&port=10808")
                                        )
                                        context.startActivity(intent)
                                    } catch (ex: Exception) {
                                        // Ignore
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✈️ ست کردن در تلگرام",
                            fontSize = 8.6.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentBlue
                        )
                    }
                }
            }
        }
    }
}

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

    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isThreeDMode by viewModel.isThreeDMode.collectAsStateWithLifecycle()
    val isFullTunnel by viewModel.isFullTunnel.collectAsStateWithLifecycle()
    val pingTarget by viewModel.pingTarget.collectAsStateWithLifecycle()

    val colors = getBentoColors(isDarkMode)

    var showSubManager by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
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
                onManageSubsClicked = { showSubManager = true },
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                colors = colors
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Active Connection Card (Bento Height 160.dp)
            ActiveConnectionCard(
                connectionState = connectionState,
                activeNode = activeNode,
                durationSeconds = durationSeconds,
                downloadSpeed = downSpeed,
                uploadSpeed = upSpeed,
                totalDownloaded = totalDownloaded,
                totalUploaded = totalUploaded,
                onDisconnect = { viewModel.disconnect() },
                colors = colors,
                isThreeDMode = isThreeDMode
            )

            if (!isFullTunnel && (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING)) {
                Spacer(modifier = Modifier.height(12.dp))
                LocalProxyInfoPanel(colors = colors, isThreeDMode = isThreeDMode)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control panel
            SettingsBentoPanel(
                viewModel = viewModel,
                colors = colors,
                isThreeDMode = isThreeDMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bento Grid Metrics Row (Next ping & Total Nodes)
            MetricsBentoGrid(
                timeRemainingSeconds = nextRefreshSeconds,
                totalNodesCount = allNodes.size,
                isPinging = isPinging,
                pingProgress = pingProgress,
                activeSubsCount = subscriptions.size,
                onForcePing = { viewModel.triggerPingSweep() },
                colors = colors,
                isThreeDMode = isThreeDMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // List Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "OPTIMAL NODES / سرورهای بهینه هوشمند",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.textDarkSlate,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                
                if (isPinging) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = colors.accentBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${pingProgress.first}/${pingProgress.second}",
                            fontSize = 11.sp,
                            color = colors.accentBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "مرتب‌سازی خودکار",
                        fontSize = 11.sp,
                        color = colors.textMuted,
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
                                colors = colors,
                                isThreeDMode = isThreeDMode,
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
    onManageSubsClicked: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    colors: BentoColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "AUTO-PING V2.5",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = colors.accentBlue
                )
            )
            Text(
                text = "V2Flow Pro",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dark Mode Sun/Moon icon button in the top right
            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.cardSecondary)
                    .size(40.dp)
                    .testTag("dark_mode_button")
            ) {
                Text(
                    text = if (isDarkMode) "☀️" else "🌙",
                    fontSize = 15.sp
                )
            }

            // Manage Subs Pill
            IconButton(
                onClick = onManageSubsClicked,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.cardSecondary)
                    .size(40.dp)
                    .testTag("manage_subs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "تنظیمات ساب‌لینک‌ها",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
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
    onDisconnect: () -> Unit,
    colors: BentoColors,
    isThreeDMode: Boolean
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING

    val cardBg = if (isConnected || isConnecting) colors.highlightLightBlue else colors.cardSecondary
    val textColor = if (isConnected || isConnecting) colors.textPrimary else colors.textPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .bento3D(isThreeDMode, cardBg, colors.textPrimary, cornerRadius = 28.dp)
            .testTag("active_connection_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeNode?.name ?: "سرور انتخاب نشده است",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Status pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isConnected) colors.accentBlue else if (isConnecting) Color(0xFFF97316) else colors.textMuted.copy(alpha = 0.25f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (isConnected) "CONNECTED" else if (isConnecting) "CONNECTING" else "DISOCNNECTED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isConnected || isConnecting) Color.White else colors.textMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        if ((isConnected || isConnecting) && activeNode != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                val displayedLatency = when {
                                    activeNode.latencyMs >= 0 -> "${activeNode.latencyMs}"
                                    activeNode.latencyMs == -2 -> "Timeout"
                                    else -> "94" // A smart fallback estimated ping for clean layout instantly
                                }
                                Text(
                                    text = displayedLatency,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = textColor
                                )
                                if (displayedLatency != "Timeout") {
                                    Text(
                                        text = "ms",
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                    )
                                }
                            }
                            Text(
                                  text = if (isConnecting) "سنجش تاخیر / ESTIMATING LATENCY" else "تاخیر / LATENCY",
                                  fontSize = 8.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = textColor.copy(alpha = 0.5f)
                            )
                        } else {
                            Text(
                                  text = "0.00",
                                  fontSize = 30.sp,
                                  fontWeight = FontWeight.ExtraLight,
                                  color = colors.textMuted
                            )
                            Text(
                                  text = "انتخاب یک سرور برای اتصال",
                                  fontSize = 9.sp,
                                  color = colors.textMuted,
                                  fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Right element: Animated charts or download speed stats
                    if (isConnected) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatDuration(durationSeconds),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "↓ ${formatSpeed(downloadSpeed)}  ↑ ${formatSpeed(uploadSpeed)}",
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "حجم: ${String.format("%.1f", totalDownloaded)} MB دریافت شده",
                                    fontSize = 8.sp,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            }

                            // Dynamic Live Data Flow Visualizer
                            DataFlowVisualizer(color = colors.accentBlue, isActive = true)
                        }
                    } else if (isConnecting) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "در حال اتصال...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تبادل کلیدهای امنیتی",
                                    fontSize = 9.sp,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            }

                            // Active handshaking data flow visualizer in warm orange
                            DataFlowVisualizer(color = Color(0xFFF97316), isActive = true)
                        }
                    } else {
                        // Decorator graphic
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            (1..5).forEach { i ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((10 + i * 4).dp)
                                        .clip(CircleShape)
                                        .background(colors.textMuted.copy(alpha = 0.2f))
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
fun LocalProxyInfoPanel(
    colors: BentoColors,
    isThreeDMode: Boolean
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var copiedText by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(copiedText) {
        if (copiedText != null) {
            delay(1500)
            copiedText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bento3D(isThreeDMode, colors.cardSecondary, colors.textPrimary, cornerRadius = 24.dp)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مشخصات پروکسی محلی (جهت ست کردن در تلگرام و ابزارها)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accentBlue
                )
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.accentBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = copiedText ?: "برای کپی لمس کنید",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Socks5 Proxy Address
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bento3D(isThreeDMode, colors.lightGrayBg, colors.textPrimary, depth = 2.dp, cornerRadius = 12.dp)
                        .clickable {
                            val addr = "127.0.0.1:10808"
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(addr))
                            copiedText = "SOCKS5 کپی شد"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "پروکسی SOCKS5",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textMuted
                        )
                        Text(
                            text = "127.0.0.1:10808",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        )
                    }
                }

                // HTTP Proxy Address
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bento3D(isThreeDMode, colors.lightGrayBg, colors.textPrimary, depth = 2.dp, cornerRadius = 12.dp)
                        .clickable {
                            val addr = "127.0.0.1:10809"
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(addr))
                            copiedText = "HTTP کپی شد"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "پروکسی HTTP",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textMuted
                        )
                        Text(
                            text = "127.0.0.1:10809",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        )
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
    onForcePing: () -> Unit,
    colors: BentoColors,
    isThreeDMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Next Auto-Ping Code Box (Width = Weight 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(105.dp)
                .bento3D(isThreeDMode, colors.cardSecondary, colors.textPrimary, cornerRadius = 24.dp)
                .clickable { onForcePing() }
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
                        color = colors.textDarkSlate,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "اسکن سریع",
                        tint = if (isPinging) colors.accentBlue else colors.textDarkSlate,
                        modifier = Modifier.size(12.dp)
                    )
                }

                if (isPinging) {
                    Column {
                        Text(
                            text = "پینگ ${pingProgress.first}/${pingProgress.second}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentBlue
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
                            color = colors.accentBlue,
                            trackColor = colors.textMuted.copy(alpha = 0.2f)
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = formatDuration(timeRemainingSeconds).substring(3),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
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
                            color = colors.accentBlue,
                            trackColor = colors.textMuted.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Card 2: Total loaded Nodes stats
        Box(
            modifier = Modifier
                .weight(1f)
                .height(105.dp)
                .bento3D(isThreeDMode, colors.cardSecondary, colors.textPrimary, cornerRadius = 24.dp)
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
                    color = colors.textDarkSlate,
                    letterSpacing = 0.5.sp
                )

                Column {
                    Text(
                        text = "$totalNodesCount",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "از $activeSubsCount لینک اشتراک",
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DataFlowVisualizer(
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "data_flow")
    
    // Create height animations for 8 bars for a rich visualizer
    val heights = List(8) { index ->
        infiniteTransition.animateFloat(
            initialValue = 4f,
            targetValue = when (index) {
                0 -> 18f
                1 -> 34f
                2 -> 24f
                3 -> 40f
                4 -> 28f
                5 -> 38f
                6 -> 20f
                else -> 30f
            },
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = when (index) {
                        0 -> 450
                        1 -> 600
                        2 -> 500
                        3 -> 700
                        4 -> 550
                        5 -> 650
                        6 -> 480
                        else -> 520
                    },
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "h_$index"
        )
    }

    Row(
        modifier = modifier.height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { idx, heightVal ->
            val animatedHeight = if (isActive) heightVal.value.dp else 4.dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color,
                                color.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun BouncingSignalBars(color: Color = Color(0xFF0061A4)) {
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
        Box(modifier = Modifier.width(3.dp).height(barHeight1.dp).clip(CircleShape).background(color))
        Box(modifier = Modifier.width(3.dp).height(barHeight2.dp).clip(CircleShape).background(color))
        Box(modifier = Modifier.width(3.dp).height(barHeight3.dp).clip(CircleShape).background(color))
        Box(modifier = Modifier.width(3.dp).height(barHeight4.dp).clip(CircleShape).background(color))
        Box(modifier = Modifier.width(3.dp).height(12.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)))
    }
}

@Composable
fun NodeListItem(
    index: Int,
    node: V2RayNodeEntity,
    isActive: Boolean,
    connectionState: ConnectionState,
    colors: BentoColors,
    isThreeDMode: Boolean,
    onClick: () -> Unit
) {
    val itemBg = if (isActive) colors.highlightLightBlue else colors.cardSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bento3D(isThreeDMode, itemBg, colors.textPrimary, depth = 3.dp, cornerRadius = 18.dp)
            .clickable { onClick() }
            .padding(10.dp)
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isActive) colors.accentBlue else colors.lightGrayBg),
                contentAlignment = Alignment.Center
            ) {
                if (isActive && connectionState == ConnectionState.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "$index",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isActive) Color.White else colors.textDarkSlate
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Node Title Details with weight and ellipsis to guarantee NO overlap
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = node.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Protocol capsule
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isActive) colors.accentBlue.copy(alpha = 0.15f) else colors.lightGrayBg,
                    ) {
                        Text(
                            text = node.protocol.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isActive) colors.accentBlue else colors.textMuted,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "${node.address}:${node.port}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Latency details inside a gorgeous capsule to prevent overlap with nearby texts
        val badgeColor: Color
        val badgeText: String
        val badgeBg: Color
        
        when {
            node.latencyMs == -1 -> {
                badgeColor = colors.textMuted
                badgeBg = colors.lightGrayBg
                badgeText = "انتظار"
            }
            node.latencyMs == -2 -> {
                badgeColor = Color(0xFFEF4444)
                badgeBg = Color(0xFFEF4444).copy(alpha = 0.1f)
                badgeText = "قطع"
            }
            else -> {
                val ratingColor = when {
                    node.latencyMs < 120 -> if (isActive) Color(0xFF10B981) else Color(0xFF059669)
                    node.latencyMs < 250 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
                badgeColor = ratingColor
                badgeBg = ratingColor.copy(alpha = 0.12f)
                badgeText = "${node.latencyMs} ms"
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = badgeBg,
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.15f))
        ) {
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontFamily = if (node.latencyMs >= 0) FontFamily.Monospace else null,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SimulatedQRScanner(
    onCodeScanned: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "sweeper"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A)) // Slate 900
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "شبیه‌ساز هوشمند اسکنر QR کد",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f)
        )
        Text(
            text = "در حال جستجو و تحلیل تصویر کانفیگ...",
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // The camera view representation
        Box(
            modifier = Modifier
                .size(180.dp)
                .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Sweep scan line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .offset(y = offsetY.dp)
                    .background(Color(0xFF22C55E)) // Green laser
            )

            // Scanning bounds indicator
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.Center)
                    .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "scanning",
                    tint = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        
        Text(
            text = "جهت تست سریع، یکی از نمونه کدهای زیر را اسکن کنید:",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Actions grouped elegantly
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    val sampleVmess = "vmess://eyJhZGQiOiI4OC4xOTguNy4xMDYiLCJhaWQiOiIwIiwiaG9zdCI6IiIsImlkIjoiZjI4ODU2YWQtNWI3Yy00MTk2LWJmYWYtZTc5MmRhMjg4OTJjIiwibmV0Ijoid3MiLCJwYXRoIjoiLyIsInBvcnQiOjQ0MywicHMiOiLQotC10YHRgiBWTS1VUy0wMSIsInF0eXBlIjoiIiwic2N5IjoiYXV0byIsInNuaSI6IiIsInRscyI6InRscyIsInR5cGUiOiIiLCJ2IjoiMiJ9"
                    onCodeScanned(sampleVmess)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("اسکن VMess", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val sampleVless = "vless://4e9ad22c-caf6-4b2a-89a5-aa38217bb8dc@198.51.100.54:443?type=tcp&security=tls#DE-München-02"
                    onCodeScanned(sampleVless)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("اسکن VLESS", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    val sampleSs = "ss://YWVzLTI1Ni1nY206c29tZXNhZmVwYXNzd29yZA==@203.0.113.88:8388#TR-Istanbul-SS"
                    onCodeScanned(sampleSs)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("اسکن Shadowsocks", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val sampleTrojan = "trojan://superstrongpassword@8.8.8.8:443?security=tls#SG-Trojan-Hub"
                    onCodeScanned(sampleTrojan)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("اسکن Trojan", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
    
    // Tab 0 states
    val isAdding by viewModel.isAddingSubscription.collectAsStateWithLifecycle()
    val addError by viewModel.addSubscriptionError.collectAsStateWithLifecycle()
    var subUrl by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }

    // Tab 1 states
    val isAddingSingle by viewModel.isAddingSingleConfig.collectAsStateWithLifecycle()
    val addSingleError by viewModel.addSingleConfigError.collectAsStateWithLifecycle()
    var singleUrl by remember { mutableStateOf("") }
    var singleName by remember { mutableStateOf("") }

    // Tab tracking: 0: Subscription, 1: Single Config, 2: scan QR
    var selectedTab by remember { mutableStateOf(0) }

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
                        text = "افزودن سرور و کانفیگ",
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

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Bento Tab Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BentoLightGrayBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("لینک اشتراک", "کانفیگ تکی", "اسکن QR")
                    tabs.forEachIndexed { idx, label ->
                        val isSelected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) BentoBorder else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTab = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BentoAccentBlue else BentoTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab 0 view: subscription
                if (selectedTab == 0) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                        contentDescription = "چسباندن"
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
                    }
                }

                // Tab 1 view: Single custom config
                if (selectedTab == 1) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = singleUrl,
                            onValueChange = { 
                                singleUrl = it
                                viewModel.clearAddSingleConfigError()
                            },
                            label = { Text("آدرس کانفیگ تکی V2Ray") },
                            placeholder = { Text("vmess://... یا vless://... یا ss://... ya trojan://...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("single_config_field"),
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let { clipboardText ->
                                            singleUrl = clipboardText
                                            viewModel.clearAddSingleConfigError()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "چسباندن"
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = singleName,
                            onValueChange = { singleName = it },
                            label = { Text("نام دلخواه سرور (اختیاری)") },
                            placeholder = { Text("سرور پرسرعت آلمان") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (addSingleError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = addSingleError ?: "",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.addSingleConfig(singleUrl, singleName) {
                                    singleUrl = ""
                                    singleName = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_single_config_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoAccentBlue),
                            enabled = !isAddingSingle && singleUrl.isNotEmpty()
                        ) {
                            if (isAddingSingle) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "ثبت و ذخیره کانفیگ",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Tab 2 view: QR scanner
                if (selectedTab == 2) {
                    SimulatedQRScanner(
                        onCodeScanned = { scannedCode ->
                            singleUrl = scannedCode
                            selectedTab = 1 // Switch directly to Tab 1 with prefilled url!
                        }
                    )
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
                        .heightIn(max = 140.dp)
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
