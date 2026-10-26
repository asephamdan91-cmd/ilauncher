package com.example.ioslauncher

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val IosBlue = Color(0xFF0A84FF)
private val IosGreen = Color(0xFF30D158)
private val IosOrange = Color(0xFFFF9F0A)

@Composable
fun ControlCenterOverlay(visible: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { -it / 4 },
        exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 4 },
    ) {
        ControlCenterContent(onClose)
    }
}

private data class SysState(
    val airplane: Boolean = false,
    val data: Boolean = false,
    val wifi: Boolean = false,
    val bluetooth: Boolean = false,
    val rotationLocked: Boolean = false,
)

@Suppress("DEPRECATION")
private fun readSys(ctx: Context): SysState {
    val cr = ctx.contentResolver
    val airplane = runCatching {
        Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    }.getOrDefault(false)
    val wifi = runCatching {
        (ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    }.getOrDefault(false)
    val bt = runCatching {
        (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true
    }.getOrDefault(false)
    val data = runCatching {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.allNetworks.any { n ->
            cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }
    }.getOrDefault(false)
    val rot = runCatching {
        Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1) == 0
    }.getOrDefault(false)
    return SysState(airplane, data, wifi, bt, rot)
}

private fun openIntent(ctx: Context, intent: Intent) {
    runCatching { ctx.startActivity(intent) }.onFailure {
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

private fun writeSettingsIntent(ctx: Context) =
    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName))

private fun internetIntent(): Intent =
    if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    else Intent(Settings.ACTION_WIRELESS_SETTINGS)

private fun wifiIntent(): Intent =
    if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    else Intent(Settings.ACTION_WIFI_SETTINGS)

private class Torch(val on: Boolean, val toggle: () -> Unit)

@Composable
private fun rememberTorch(ctx: Context): Torch {
    val cm = remember { ctx.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val camId = remember {
        runCatching {
            cm.cameraIdList.firstOrNull { id ->
                val c = cm.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }
    DisposableEffect(cm) {
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == camId) on = enabled
            }
        }
        cm.registerTorchCallback(cb, Handler(Looper.getMainLooper()))
        onDispose { cm.unregisterTorchCallback(cb) }
    }
    return Torch(on) {
        if (camId != null) runCatching { cm.setTorchMode(camId, !on) }
    }
}

@Composable
private fun ControlCenterContent(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext
    val cr = app.contentResolver

    // Status dibaca ulang tiap ~0,8 detik selama panel terbuka
    val sys by produceState(SysState()) {
        while (true) {
            value = readSys(app)
            delay(800)
        }
    }
    val torch = rememberTorch(ctx)

    val audio = remember { app.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    val startVol = remember { audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVol.toFloat() }
    val startBrightness = remember {
        runCatching { Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f }.getOrDefault(0.5f)
    }
    val askedWrite = remember { booleanArrayOf(false) }

    val closeAnd: (Intent) -> Unit = { intent ->
        onClose()
        openIntent(ctx, intent)
    }

    val onBrightness: (Float) -> Unit = { f ->
        if (Settings.System.canWrite(ctx)) {
            runCatching {
                Settings.System.putInt(
                    cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (f * 255).roundToInt().coerceIn(1, 255))
            }
        } else if (!askedWrite[0]) {
            // Izin "Ubah pengaturan sistem" harus diberikan sekali oleh pengguna
            askedWrite[0] = true
            closeAnd(writeSettingsIntent(ctx))
        }
    }
    val onVolume: (Float) -> Unit = { f ->
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, (f * maxVol).roundToInt(), 0) }
    }
    val onRotation: () -> Unit = {
        if (Settings.System.canWrite(ctx)) {
            runCatching {
                Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, if (sys.rotationLocked) 1 else 0)
            }
        } else {
            closeAnd(writeSettingsIntent(ctx))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .noRippleClickable(onClose)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .noRippleClickable { },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(168.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Blok konektivitas 2x2
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .glass(RoundedCornerShape(30.dp)),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RoundToggle("✈️", sys.airplane, IosOrange) {
                            closeAnd(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
                        }
                        RoundToggle("📶", sys.data, IosGreen) { closeAnd(internetIntent()) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RoundToggle("🌐", sys.wifi, IosBlue) { closeAnd(wifiIntent()) }
                        RoundToggle("🎧", sys.bluetooth, IosBlue) {
                            closeAnd(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }
                    }
                }

                Row(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VerticalSlider("☀️", startBrightness, onBrightness, Modifier.weight(1f).fillMaxHeight())
                    VerticalSlider("🔊", startVol, onVolume, Modifier.weight(1f).fillMaxHeight())
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutTile("🔦", torch.on, Modifier.weight(1f)) { torch.toggle() }
                ShortcutTile("🔒", sys.rotationLocked, Modifier.weight(1f)) { onRotation() }
                ShortcutTile("📷", false, Modifier.weight(1f)) {
                    closeAnd(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                }
                ShortcutTile("⏱️", false, Modifier.weight(1f)) {
                    closeAnd(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(50))
                    .noRippleClickable { closeAnd(Intent(Settings.ACTION_SETTINGS)) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    "⚙️  Pengaturan",
                    style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

@Composable
private fun RoundToggle(emoji: String, active: Boolean, activeColor: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .then(
                if (active) Modifier.background(activeColor, CircleShape)
                else Modifier.glass(CircleShape),
            )
            .noRippleClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(emoji, style = TextStyle(fontSize = 22.sp))
    }
}

@Composable
private fun ShortcutTile(emoji: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .then(
                if (active) Modifier.background(Color.White.copy(alpha = 0.92f), shape)
                else Modifier.glass(shape),
            )
            .noRippleClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(emoji, style = TextStyle(fontSize = 24.sp))
    }
}

/** Slider vertikal ala iOS: isian putih naik dari bawah, bisa diketuk atau diseret. */
@Composable
private fun VerticalSlider(
    emoji: String,
    initial: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    var level by remember { mutableFloatStateOf(initial.coerceIn(0f, 1f)) }
    val currentOnChange by rememberUpdatedState(onChange)

    Box(
        modifier
            .clip(shape)
            .glass(shape)
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    val f = (1f - off.y / size.height).coerceIn(0f, 1f)
                    level = f
                    currentOnChange(f)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { off ->
                        val f = (1f - off.y / size.height).coerceIn(0f, 1f)
                        level = f
                        currentOnChange(f)
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val f = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        level = f
                        currentOnChange(f)
                    },
                )
            },
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(level.coerceIn(0.02f, 1f))
                .background(Color.White.copy(alpha = 0.92f)),
        )
        BasicText(
            emoji,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            style = TextStyle(fontSize = 22.sp),
        )
    }
}
