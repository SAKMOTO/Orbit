package com.roubao.autopilot.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
//import com.google.firebase.crashlytics.FirebaseCrashlytics

import com.roubao.autopilot.R
import androidx.compose.ui.res.stringResource
import com.roubao.autopilot.BuildConfig

import com.roubao.autopilot.data.ApiProvider
import com.roubao.autopilot.data.AppSettings
import com.roubao.autopilot.ui.theme.OrbitTheme
import com.roubao.autopilot.ui.theme.ThemeMode
import com.roubao.autopilot.utils.CrashHandler

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateApiKey: (String) -> Unit,
    onUpdateBaseUrl: (String) -> Unit,
    onUpdateModel: (String) -> Unit,
    onUpdateCachedModels: (List<String>) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateMaxSteps: (Int) -> Unit,
    onUpdateCloudCrashReport: (Boolean) -> Unit,
    onUpdateRootModeEnabled: (Boolean) -> Unit,
    onUpdateSuCommandEnabled: (Boolean) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onSelectProvider: (ApiProvider) -> Unit,

    shizukuAvailable: Boolean,
    shizukuPrivilegeLevel: String = "ADB", // "ADB", "ROOT", "NONE"
    onFetchModels: ((onSuccess: (List<String>) -> Unit, onError: (String) -> Unit) -> Unit)? = null
) {
    val colors = OrbitTheme.colors
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showMaxStepsDialog by remember { mutableStateOf(false) }
    var showBaseUrlDialog by remember { mutableStateOf(false) }
    var showShizukuHelpDialog by remember { mutableStateOf(false) }
    var showOverlayHelpDialog by remember { mutableStateOf(false) }
    var showRootModeWarningDialog by remember { mutableStateOf(false) }
    var showSuCommandWarningDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 顶部标题
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )

                }
            }
        }

        // 连接状态卡片
        item {
            StatusCard(shizukuAvailable = shizukuAvailable)
        }

        // 外观设置分组
        item {
            SettingsSection(title = stringResource(R.string.settings_appearance))
        }

        // 主题设置
        item {
            SettingsItem(
                icon = Icons.Default.Star,
                title = stringResource(R.string.settings_theme),
                subtitle = when (settings.themeMode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                },
                onClick = { showThemeDialog = true }
            )
        }


        // 语言设置
        item {
            SettingsItem(
                icon = Icons.Default.Settings, // Replace with appropriate icon if needed
                title = stringResource(R.string.settings_language),
                subtitle = when (settings.language) {
                    "zh" -> stringResource(R.string.language_zh)
                    "en" -> stringResource(R.string.language_en)
                    else -> stringResource(R.string.language_auto)
                },
                onClick = { showLanguageDialog = true }
            )
        }


        // 执行设置分组
        item {
            SettingsSection(title = stringResource(R.string.settings_execution))
        }

        // 最大步数设置
        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_max_steps),
                subtitle = stringResource(R.string.history_steps, settings.maxSteps),
                onClick = { showMaxStepsDialog = true }
            )
        }

        // Shizuku 高级设置分组（仅在 Shizuku 可用时显示）
        if (shizukuAvailable) {
            item {
                SettingsSection(title = stringResource(R.string.settings_shizuku_advanced))
            }

            // 显示当前权限级别
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.backgroundCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when (shizukuPrivilegeLevel) {
                                        "ROOT" -> colors.error.copy(alpha = 0.15f)
                                        else -> colors.primary.copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = when (shizukuPrivilegeLevel) {
                                    "ROOT" -> colors.error
                                    else -> colors.primary
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_privilege_level),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = when (shizukuPrivilegeLevel) {
                                    "ROOT" -> stringResource(R.string.settings_root_mode_active)
                                    "ADB" -> stringResource(R.string.settings_adb_mode_active)
                                    else -> stringResource(R.string.shizuku_not_connected)
                                },
                                fontSize = 13.sp,
                                color = when (shizukuPrivilegeLevel) {
                                    "ROOT" -> colors.error
                                    else -> colors.textSecondary
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Root 模式开关（仅在 Shizuku 以 Root 权限运行时可用）
            item {
                val isShizukuRoot = shizukuPrivilegeLevel == "ROOT"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.backgroundCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isShizukuRoot) colors.error.copy(alpha = 0.15f)
                                    else colors.textHint.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isShizukuRoot) colors.error else colors.textHint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_root_mode),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isShizukuRoot) colors.textPrimary else colors.textHint
                            )
                            Text(
                                text = when {
                                    !isShizukuRoot -> stringResource(R.string.settings_root_mode_need_root)
                                    settings.rootModeEnabled -> stringResource(R.string.settings_root_mode_enabled_hint)
                                    else -> stringResource(R.string.settings_root_mode_desc)
                                },
                                fontSize = 13.sp,
                                color = when {
                                    !isShizukuRoot -> colors.textHint
                                    settings.rootModeEnabled -> colors.error
                                    else -> colors.textSecondary
                                },
                                maxLines = 1
                            )
                        }
                        Switch(
                            checked = settings.rootModeEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showRootModeWarningDialog = true
                                } else {
                                    onUpdateRootModeEnabled(false)
                                }
                            },
                            enabled = isShizukuRoot,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.error,
                                checkedTrackColor = colors.error.copy(alpha = 0.5f),
                                uncheckedThumbColor = colors.textHint,
                                uncheckedTrackColor = colors.backgroundInput,
                                disabledCheckedThumbColor = colors.textHint,
                                disabledCheckedTrackColor = colors.backgroundInput,
                                disabledUncheckedThumbColor = colors.textHint.copy(alpha = 0.5f),
                                disabledUncheckedTrackColor = colors.backgroundInput.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // su -c 开关（仅在 Root 模式开启时显示）
            if (settings.rootModeEnabled) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.backgroundCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_su_command),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (settings.suCommandEnabled) stringResource(R.string.settings_su_command_enabled) else stringResource(R.string.settings_su_command_disabled),
                                    fontSize = 13.sp,
                                    color = if (settings.suCommandEnabled) colors.error else colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                            Switch(
                                checked = settings.suCommandEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showSuCommandWarningDialog = true
                                    } else {
                                        onUpdateSuCommandEnabled(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.error,
                                    checkedTrackColor = colors.error.copy(alpha = 0.5f),
                                    uncheckedThumbColor = colors.textHint,
                                    uncheckedTrackColor = colors.backgroundInput
                                )
                            )
                        }
                    }
                }
            }
        }

        // API 设置分组
        item {
            SettingsSection(title = stringResource(R.string.settings_api))
        }

        // Base URL 设置
        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_provider),
                subtitle = settings.currentProvider.name,
                onClick = { showBaseUrlDialog = true }
            )
        }

        // API Key 设置
        item {
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "API Key",
                subtitle = if (settings.apiKey.isNotEmpty()) stringResource(R.string.settings_api_key_set, maskApiKey(settings.apiKey)) else stringResource(R.string.settings_api_key_unset),
                onClick = { showApiKeyDialog = true }
            )
        }

        // 模型设置
        item {
            SettingsItem(
                icon = Icons.Default.Build,
                title = "模型",
                subtitle = settings.model,
                onClick = { showModelDialog = true }
            )
        }

        // 反馈分组
        item {
            SettingsSection(title = stringResource(R.string.settings_feedback))
        }

        // 云端崩溃上报开关
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.backgroundCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_cloud_crash_report),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (settings.cloudCrashReportEnabled) stringResource(R.string.settings_cloud_crash_report_enabled) else stringResource(R.string.settings_cloud_crash_report_disabled),
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                    Switch(
                        checked = settings.cloudCrashReportEnabled,
                        onCheckedChange = { onUpdateCloudCrashReport(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = colors.textHint,
                            uncheckedTrackColor = colors.backgroundInput
                        )
                    )
                }
            }
        }

        item {
            val context = LocalContext.current
            val logStats = remember { mutableStateOf(CrashHandler.getLogStats(context)) }

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_export_logs),
                subtitle = logStats.value,
                onClick = {
                    CrashHandler.shareLogs(context)
                }
            )
        }

        item {
            val context = LocalContext.current
            var showClearDialog by remember { mutableStateOf(false) }

            SettingsItem(
                icon = Icons.Default.Close,
                title = stringResource(R.string.settings_clear_logs),
                subtitle = stringResource(R.string.settings_clear_logs_desc),
                onClick = { showClearDialog = true }
            )

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    containerColor = OrbitTheme.colors.backgroundCard,
                    title = { Text(stringResource(R.string.settings_clear_logs_confirm_title), color = OrbitTheme.colors.textPrimary) },
                    text = { Text(stringResource(R.string.settings_clear_logs_confirm_desc), color = OrbitTheme.colors.textSecondary) },
                    confirmButton = {
                        TextButton(onClick = {
                            CrashHandler.clearLogs(context)
                            showClearDialog = false
                            android.widget.Toast.makeText(context, context.getString(R.string.settings_logs_cleared), android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Text(stringResource(R.string.btn_confirm), color = OrbitTheme.colors.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text(stringResource(R.string.btn_cancel), color = OrbitTheme.colors.textSecondary)
                        }
                    }
                )
            }
        }

        // 帮助分组
        item {
            SettingsSection(title = stringResource(R.string.settings_help))
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_shizuku_guide),
                subtitle = stringResource(R.string.settings_shizuku_guide_desc),
                onClick = { showShizukuHelpDialog = true }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_overlay_help),
                subtitle = stringResource(R.string.settings_overlay_help_desc),
                onClick = { showOverlayHelpDialog = true }
            )
        }

        // 关于分组
        item {
            SettingsSection(title = stringResource(R.string.settings_about))
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "版本",
                subtitle = BuildConfig.VERSION_NAME,
                onClick = { }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Build,
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.settings_app_desc),
                onClick = { }
            )
        }


        // 底部间距
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectDialog(
            currentTheme = settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                onUpdateThemeMode(it)
                showThemeDialog = false
            }
        )
    }

    // 最大步数设置对话框
    if (showMaxStepsDialog) {
        MaxStepsDialog(
            currentSteps = settings.maxSteps,
            onDismiss = { showMaxStepsDialog = false },
            onConfirm = {
                onUpdateMaxSteps(it)
                showMaxStepsDialog = false
            }
        )
    }

    // API Key 编辑对话框
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = settings.apiKey,
            onDismiss = { showApiKeyDialog = false },
            onConfirm = {
                onUpdateApiKey(it)
                showApiKeyDialog = false
            }
        )
    }

    // 模型选择对话框（合并了自定义输入和从 API 获取）
    if (showModelDialog) {
        ModelSelectDialogWithFetch(
            currentModel = settings.model,
            cachedModels = settings.cachedModels,
            hasApiKey = settings.apiKey.isNotEmpty(),
            onDismiss = { showModelDialog = false },
            onSelect = {
                onUpdateModel(it)
                showModelDialog = false
            },
            onFetchModels = onFetchModels,
            onUpdateCachedModels = onUpdateCachedModels
        )
    }

    // 服务商选择对话框
    if (showBaseUrlDialog) {
        ProviderSelectDialog(
            currentProviderId = settings.currentProviderId,
            customBaseUrl = settings.currentConfig.customBaseUrl,
            onDismiss = { showBaseUrlDialog = false },
            onSelectProvider = { provider ->
                onSelectProvider(provider)
                showBaseUrlDialog = false
            },
            onUpdateCustomUrl = { url ->
                onUpdateBaseUrl(url)
            }
        )
    }

    // Shizuku 帮助对话框
    if (showShizukuHelpDialog) {
        ShizukuHelpDialog(onDismiss = { showShizukuHelpDialog = false })
    }

    // 悬浮窗权限帮助对话框
    if (showOverlayHelpDialog) {
        OverlayHelpDialog(onDismiss = { showOverlayHelpDialog = false })
    }

    // Root 模式警告对话框
    if (showRootModeWarningDialog) {
        RootModeWarningDialog(
            onDismiss = { showRootModeWarningDialog = false },
            onConfirm = {
                onUpdateRootModeEnabled(true)
                showRootModeWarningDialog = false
            }
        )
    }

    // su -c 命令警告对话框

    if (showSuCommandWarningDialog) {
        SuCommandWarningDialog(
            onDismiss = { showSuCommandWarningDialog = false },
            onConfirm = {
                onUpdateSuCommandEnabled(true)
                showSuCommandWarningDialog = false
            }
        )
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageSelectDialog(
            currentLanguage = settings.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                onUpdateLanguage(it)
                showLanguageDialog = false
            }
        )
    }
}



@Composable
fun StatusCard(shizukuAvailable: Boolean) {
    val colors = OrbitTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (shizukuAvailable) colors.success.copy(alpha = 0.15f) else colors.error.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (shizukuAvailable) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (shizukuAvailable) colors.success else colors.error,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (shizukuAvailable) stringResource(R.string.shizuku_connected) else stringResource(R.string.shizuku_not_connected),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (shizukuAvailable) colors.success else colors.error
                )
                Text(
                    text = if (shizukuAvailable) stringResource(R.string.shizuku_available) else stringResource(R.string.shizuku_unavailable),
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    val colors = OrbitTheme.colors
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = colors.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = OrbitTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.backgroundCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textHint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}



@Composable
fun LanguageSelectDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text(stringResource(R.string.language_select), color = colors.textPrimary)
        },
        text = {
            Column {
                listOf(
                    "auto" to stringResource(R.string.language_auto),
                    "zh" to stringResource(R.string.language_zh),
                    "en" to stringResource(R.string.language_en)
                ).forEach { (lang, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lang) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == lang,
                            onClick = { onSelect(lang) },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, color = colors.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = colors.primary)
            }
        }
    )
}

@Composable
fun ThemeSelectDialog(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    val colors = OrbitTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("选择主题", color = colors.textPrimary)
        },
        text = {
            Column {
                listOf(
                    ThemeMode.LIGHT to "浅色模式",
                    ThemeMode.DARK to "深色模式",
                    ThemeMode.SYSTEM to "跟随系统"
                ).forEach { (mode, label) ->
                    val isSelected = mode == currentTheme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(mode) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, colors.textHint, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                color = if (isSelected) colors.primary else colors.textPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun ApiKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    var key by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("API Key", color = colors.textPrimary)
        },
        text = {
            Column {
                Text(
                    text = "请输入您的 API Key",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.backgroundInput)
                        .padding(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = key,
                            onValueChange = { key = it },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp)
                        )
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(
                                text = if (showKey) "隐藏" else "显示",
                                fontSize = 12.sp,
                                color = colors.textHint
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(key) }) {
                Text("确定", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.textSecondary)
            }
        }
    )
}


/**
 * 模型选择对话框（合并了自定义输入和从 API 获取）
 */
@Composable
fun ModelSelectDialogWithFetch(
    currentModel: String,
    cachedModels: List<String>,
    hasApiKey: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onFetchModels: ((onSuccess: (List<String>) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onUpdateCachedModels: (List<String>) -> Unit
) {
    val colors = OrbitTheme.colors
    val context = LocalContext.current
    var customModel by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 默认推荐模型
    val defaultModel = "qwen3-vl-plus"

    // 过滤后的模型列表
    val filteredModels = remember(cachedModels, searchQuery) {
        if (searchQuery.isBlank()) {
            cachedModels
        } else {
            cachedModels.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("选择模型", color = colors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 默认推荐模型
                Text(
                    text = "推荐模型",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textHint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val isDefaultSelected = currentModel == defaultModel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(defaultModel) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDefaultSelected) colors.primary.copy(alpha = 0.15f) else colors.backgroundInput
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDefaultSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(2.dp, colors.textHint, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = defaultModel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDefaultSelected) colors.primary else colors.textPrimary
                            )
                            Text(
                                text = "阿里云通义千问视觉模型",
                                fontSize = 11.sp,
                                color = colors.textHint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 自定义模型输入
                Text(
                    text = "自定义模型",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textHint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.backgroundInput)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        if (customModel.isEmpty()) {
                            Text(
                                text = "输入模型名称，如 gpt-4o",
                                color = colors.textHint,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = customModel,
                            onValueChange = { customModel = it },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 确认按钮
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(enabled = customModel.isNotBlank()) {
                                onSelect(customModel.trim())
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (customModel.isNotBlank()) colors.primary else colors.backgroundInput
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "确认",
                                tint = if (customModel.isNotBlank()) Color.White else colors.textHint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 从 API 获取模型 - 更明显的按钮
                if (onFetchModels != null) {
                    Button(
                        onClick = {
                            if (!hasApiKey) {
                                android.widget.Toast.makeText(context, "请先设置 API Key", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            onFetchModels(
                                { models ->
                                    isLoading = false
                                    onUpdateCachedModels(models)
                                    android.widget.Toast.makeText(context, "获取到 ${models.size} 个模型", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                { error ->
                                    isLoading = false
                                    android.widget.Toast.makeText(context, "获取失败: $error", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = !isLoading && hasApiKey,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            disabledContainerColor = colors.backgroundInput
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("获取中...", fontSize = 14.sp, color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (hasApiKey) Color.White else colors.textHint
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "从 API 获取可用模型",
                                fontSize = 14.sp,
                                color = if (hasApiKey) Color.White else colors.textHint
                            )
                        }
                    }

                    if (cachedModels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "API 模型列表 (${cachedModels.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textHint,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 搜索框（模型数量超过 10 个时显示）
                    if (cachedModels.size > 10) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.backgroundInput)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colors.textHint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "搜索模型...",
                                            color = colors.textHint,
                                            fontSize = 14.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = TextStyle(
                                            color = colors.textPrimary,
                                            fontSize = 14.sp
                                        ),
                                        cursorBrush = SolidColor(colors.primary),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清除",
                                        tint = colors.textHint,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 模型列表
                if (cachedModels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasApiKey) "点击「从 API 获取」加载模型列表" else "请先设置 API Key",
                            fontSize = 13.sp,
                            color = colors.textHint
                        )
                    }
                } else if (filteredModels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "没有匹配「$searchQuery」的模型",
                            fontSize = 13.sp,
                            color = colors.textHint
                        )
                    }
                } else {
                    // 显示过滤结果数量
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "找到 ${filteredModels.size} 个模型",
                            fontSize = 11.sp,
                            color = colors.textHint,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    filteredModels.forEach { model ->
                        val isSelected = model == currentModel
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { onSelect(model) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .border(2.dp, colors.textHint, CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = model,
                                    fontSize = 14.sp,
                                    color = if (isSelected) colors.primary else colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = colors.textSecondary)
            }
        }
    )
}

private fun maskApiKey(key: String): String {
    return if (key.length > 8) {
        "${key.take(4)}****${key.takeLast(4)}"
    } else {
        "****"
    }
}

@Composable
fun ShizukuHelpDialog(onDismiss: () -> Unit) {
    val colors = OrbitTheme.colors
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("Shizuku 使用指南", color = colors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                HelpStep(
                    number = "1",
                    title = "下载 Shizuku",
                    description = "从 Google Play 或 GitHub 下载 Shizuku 应用"
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 下载按钮
                Button(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("前往下载 Shizuku", color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HelpStep(
                    number = "2",
                    title = "启动 Shizuku",
                    description = "打开 Shizuku 应用，根据您的设备选择启动方式：\n\n• 无线调试（推荐）：需要 Android 11+，在开发者选项中开启无线调试\n• 连接电脑：通过 ADB 命令启动"
                )
                Spacer(modifier = Modifier.height(16.dp))
                HelpStep(
                    number = "3",
                    title = stringResource(R.string.shizuku_help_step3_title),
                    description = stringResource(R.string.shizuku_help_step3_desc)

                )
                Spacer(modifier = Modifier.height(16.dp))
                HelpStep(
                    number = "4",
                    title = "开始使用",
                    description = stringResource(R.string.shizuku_help_step4_desc)

                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = colors.primary)
            }
        }
    )
}

@Composable
fun OverlayHelpDialog(onDismiss: () -> Unit) {
    val colors = OrbitTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("悬浮窗权限说明", color = colors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "为什么需要悬浮窗权限？",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.overlay_reason),

                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint("显示当前执行进度")
                BulletPoint("提供停止按钮，随时中断任务")
                BulletPoint("在其他应用上方显示状态信息")

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "如何开启？",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.overlay_how_desc),

                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "隐私安全",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "悬浮窗仅在任务执行期间显示，不会收集任何个人信息。任务完成后悬浮窗会自动消失。",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = colors.primary)
            }
        }
    )
}

@Composable
private fun HelpStep(
    number: String,
    title: String,
    description: String
) {
    val colors = OrbitTheme.colors
    Row {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    val colors = OrbitTheme.colors
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = colors.textPrimary
        )
    }
}

@Composable
fun MaxStepsDialog(
    currentSteps: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val colors = OrbitTheme.colors
    var steps by remember { mutableStateOf(currentSteps.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("最大执行步数", color = colors.textPrimary)
        },
        text = {
            Column {
                Text(
                    text = "设置 Agent 单次任务的最大执行步数。步数越多，能完成的任务越复杂，但消耗的 token 也越多。",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 当前值显示
                Text(
                    text = "${steps.toInt()} 步",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // 滑块
                Slider(
                    value = steps,
                    onValueChange = { steps = it },
                    valueRange = 5f..100f,
                    steps = 18, // (100-5)/5 - 1 = 18 个刻度点，每 5 步一个
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.backgroundInput
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 范围提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "5",
                        fontSize = 12.sp,
                        color = colors.textHint
                    )
                    Text(
                        text = "100",
                        fontSize = 12.sp,
                        color = colors.textHint
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 快捷选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 50).forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { steps = preset.toFloat() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (steps.toInt() == preset) colors.primary else colors.backgroundInput
                        ) {
                            Text(
                                text = "$preset",
                                fontSize = 14.sp,
                                color = if (steps.toInt() == preset) Color.White else colors.textSecondary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(steps.toInt()) }) {
                Text("确定", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.textSecondary)
            }
        }
    )
}

/**
 * 服务商选择对话框
 */
@Composable
fun ProviderSelectDialog(
    currentProviderId: String,
    customBaseUrl: String,
    onDismiss: () -> Unit,
    onSelectProvider: (ApiProvider) -> Unit,
    onUpdateCustomUrl: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    var selectedProviderId by remember { mutableStateOf(currentProviderId) }
    var customUrl by remember { mutableStateOf(customBaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text(stringResource(R.string.provider_select_title), color = colors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.provider_select_desc),
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 服务商列表
                ApiProvider.ALL.forEach { provider ->
                    val isSelected = provider.id == selectedProviderId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedProviderId = provider.id
                                onSelectProvider(provider)
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .border(2.dp, colors.textHint, CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                val context = LocalContext.current
                                Text(
                                    text = provider.getName(context),
                                    fontSize = 14.sp,
                                    color = if (isSelected) colors.primary else colors.textPrimary
                                )
                            }
                            // 对于非自定义服务商，显示其 URL
                            if (provider.id != "custom") {
                                Text(
                                    text = provider.baseUrl,
                                    fontSize = 11.sp,
                                    color = colors.textHint,
                                    modifier = Modifier.padding(start = 28.dp, top = 2.dp)
                                )
                            }
                        }
                    }

                    // 自定义服务商或 MAI-UI 选中时显示 URL 输入框
                    if ((provider.id == "custom" || provider.id == "mai_ui") && isSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.backgroundInput)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            if (customUrl.isEmpty()) {
                                Text(
                                    text = if (provider.id == "mai_ui") "http://your-server:8000/v1" else "https://api.example.com/v1",
                                    color = colors.textHint,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = customUrl,
                                onValueChange = { newUrl ->
                                    customUrl = newUrl
                                    onUpdateCustomUrl(newUrl)
                                },
                                textStyle = TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(colors.primary),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        Text(
                            text = if (provider.id == "mai_ui") stringResource(R.string.provider_mai_ui_url_desc) else stringResource(R.string.provider_custom_url_desc),
                            fontSize = 11.sp,
                            color = colors.textHint,
                            modifier = Modifier.padding(start = 28.dp, top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_complete), color = colors.primary)
            }
        }
    )
}

/**
 * Root 模式警告对话框
 */
@Composable
fun RootModeWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = OrbitTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                stringResource(R.string.settings_root_mode_warning_title),
                color = colors.error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_root_mode_warning_desc),
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = stringResource(R.string.settings_danger_warning),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(stringResource(R.string.settings_root_mode_risk1))
                BulletPoint(stringResource(R.string.settings_root_mode_risk2))
                BulletPoint(stringResource(R.string.settings_root_mode_risk3))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_root_mode_footer),
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error)
            ) {
                Text(stringResource(R.string.btn_understand_risk), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = colors.textSecondary)
            }
        }
    )
}

/**
 * su -c 命令警告对话框
 */
@Composable
fun SuCommandWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = OrbitTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                stringResource(R.string.settings_su_command_warning_title),
                color = colors.error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_su_command_warning_desc),
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = stringResource(R.string.settings_extreme_danger),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(stringResource(R.string.settings_su_risk1))
                BulletPoint(stringResource(R.string.settings_su_risk2))
                BulletPoint(stringResource(R.string.settings_su_risk3))
                BulletPoint(stringResource(R.string.settings_su_risk4))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_su_footer),
                    fontSize = 13.sp,
                    color = colors.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error)
            ) {
                Text(stringResource(R.string.btn_understand_risk), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = colors.textSecondary)
            }
        }
    )
}
