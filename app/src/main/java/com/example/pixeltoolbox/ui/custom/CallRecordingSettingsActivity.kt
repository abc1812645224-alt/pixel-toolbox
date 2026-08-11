/*
 * pixel-toolbox - Call Recording Settings (secondary page).
 * Provides curated settings for the ShizuCallRecorder-ported recording pipeline:
 * audio codec, bit rate, anonymous-call filter and a configurable storage location.
 */
package com.example.pixeltoolbox.ui.custom

import com.example.pixeltoolbox.integrations.scrcpy.ScrcpyAudioSource
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixeltoolbox.data.AppPreferences
import com.example.pixeltoolbox.integrations.scrcpy.ScrcpyAudioCodec
import com.example.pixeltoolbox.system.storage.SafHelper
import com.example.pixeltoolbox.ui.theme.PixelToolboxTheme
import com.example.pixeltoolbox.ui.theme.GlassCard
import com.example.pixeltoolbox.ui.theme.iOSBackground
import com.example.pixeltoolbox.ui.theme.iOSBlue
import com.example.pixeltoolbox.ui.theme.iOSLabel
import com.example.pixeltoolbox.ui.theme.iOSSecondaryLabel
import com.example.pixeltoolbox.ui.theme.iOSSeparator
import com.example.pixeltoolbox.utils.AppLogger
import androidx.compose.material3.MaterialTheme

/** Secondary settings page for the call-recording feature. */
class CallRecordingSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelToolboxTheme {
                CallRecordingSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CallRecordingSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var codecKey by remember { mutableStateOf(prefs.getAudioCodec()) }
    var bitRate by remember { mutableStateOf(prefs.getAudioBitRate()) }
    var audioSource by remember { mutableStateOf(prefs.getAudioSource()) }
    var ignoreAnonymous by remember { mutableStateOf(prefs.isIgnoreAnonymousIncomingEnabled()) }
    var folderUri by remember { mutableStateOf(prefs.getRecordingFolderUri()) }

    // SAF 目录选择器：用户点「保存目录」卡片时弹出系统文件夹选择
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // 持久化读取/写入权限，否则重启后自定义目录会失效
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            prefs.setRecordingFolderUri(uri)
            folderUri = uri
            AppLogger.i("CallRecordingSettings: SAF folder selected: name=${SafHelper.getFolderDisplayNameOrNull(context, uri)} uri=$uri")
        } else {
            AppLogger.i("CallRecordingSettings: SAF picker cancelled")
        }
    }

    Scaffold(
        containerColor = iOSBackground,
        topBar = {
            TopAppBar(
                title = { Text("通话录音设置", color = iOSLabel, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = iOSBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = iOSBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ---- 编码格式 ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("编码格式", style = MaterialTheme.typography.titleMedium, color = iOSLabel)
                    Spacer(Modifier.height(4.dp))
                    Text("Opus 为默认编码，低码率下语音清晰；AAC 兼容性更好", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = iOSSecondaryLabel)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceChip(
                        label = "Opus (默认)",
                        selected = codecKey == ScrcpyAudioCodec.OPUS.cliKey,
                        onClick = {
                            prefs.setAudioCodec(ScrcpyAudioCodec.OPUS.cliKey)
                            codecKey = ScrcpyAudioCodec.OPUS.cliKey
                            // 同步默认码率 64kbps
                            if (prefs.getAudioBitRate() !in listOf(32000, 48000, 64000, 96000, 128000)) {
                                prefs.setAudioBitRate(ScrcpyAudioCodec.OPUS.defaultBitRate)
                                bitRate = ScrcpyAudioCodec.OPUS.defaultBitRate
                            }
                        }
                    )
                    ChoiceChip(
                        label = "AAC",
                        selected = codecKey == ScrcpyAudioCodec.AAC.cliKey,
                        onClick = {
                            prefs.setAudioCodec(ScrcpyAudioCodec.AAC.cliKey)
                            codecKey = ScrcpyAudioCodec.AAC.cliKey
                            if (prefs.getAudioBitRate() !in listOf(32000, 48000, 64000, 96000, 128000)) {
                                prefs.setAudioBitRate(ScrcpyAudioCodec.AAC.defaultBitRate)
                                bitRate = ScrcpyAudioCodec.AAC.defaultBitRate
                            }
                        }
                    )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- 码率 ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("码率", style = MaterialTheme.typography.titleMedium, color = iOSLabel)
                    Spacer(Modifier.height(4.dp))
                    Text("码率越高音质越好，文件体积越大", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = iOSSecondaryLabel)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(32000 to "32 kbps", 48000 to "48 kbps", 64000 to "64 kbps", 96000 to "96 kbps", 128000 to "128 kbps").forEach { (bps, label) ->
                        ChoiceChip(
                            label = label,
                            selected = bitRate == bps,
                            onClick = {
                                prefs.setAudioBitRate(bps)
                                bitRate = bps
                            }
                        )
                    }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- 音频源 ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("音频源", style = MaterialTheme.typography.titleMedium, color = iOSLabel)
                    Spacer(Modifier.height(4.dp))
                    Text("语音通信：麦克风+通话下行混音，带回声消除；通话：系统级上下行混音，双方均衡（推荐）；均已实测扩音/听筒切换不漏录", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = iOSSecondaryLabel)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val sourceOptions = listOf(
                        ScrcpyAudioSource.VOICE_COMMUNICATION to "语音通信",
                        ScrcpyAudioSource.VOICE_CALL to "通话"
                    )
                    sourceOptions.forEach { (source, label) ->
                        ChoiceChip(
                            label = label,
                            selected = audioSource == source.cliKey,
                            onClick = {
                                prefs.setAudioSource(source.cliKey)
                                audioSource = source.cliKey
                            }
                        )
                    }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- 忽略匿名来电 ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("忽略匿名来电", style = MaterialTheme.typography.bodyLarge, color = iOSLabel)
                        Text("不录制号码为空的来电", style = MaterialTheme.typography.labelMedium, color = iOSSecondaryLabel)
                    }
                    Switch(
                        checked = ignoreAnonymous,
                        onCheckedChange = {
                            prefs.setIgnoreAnonymousIncomingEnabled(it)
                            ignoreAnonymous = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = iOSBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = iOSSeparator
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- 保存目录（可点击自定义，默认回退系统下载目录）----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { folderPicker.launch(null) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("保存目录", style = MaterialTheme.typography.titleMedium, color = iOSLabel)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Edit, contentDescription = "选择目录", tint = iOSBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("点击选择", style = MaterialTheme.typography.labelSmall, color = iOSBlue)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = iOSSecondaryLabel)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = folderUri?.let { uri ->
                                SafHelper.getFolderDisplayNameOrNull(context, uri) ?: uri.toString()
                            } ?: "未设置（将使用系统默认下载目录）",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSecondaryLabel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (folderUri != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "恢复默认（保存到系统下载目录）",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSBlue,
                            modifier = Modifier
                                .clickable {
                                    prefs.setRecordingFolderUri(null)
                                    folderUri = null
                                    AppLogger.i("CallRecordingSettings: recording folder reset to default (Downloads fallback)")
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (selected) iOSBlue.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) iOSBlue else iOSSeparator,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) iOSBlue else iOSLabel,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal))
    }
}
