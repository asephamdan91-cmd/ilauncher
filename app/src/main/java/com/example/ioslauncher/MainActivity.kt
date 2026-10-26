package com.example.ioslauncher

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var state: HomeState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = HomeState(applicationContext)
        val transparent = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = transparent, navigationBarStyle = transparent)

        // Tombol Home ditekan saat launcher sudah terbuka -> kembali ke halaman 1
        addOnNewIntentListener { state.pressHome() }

        setContent {
            LauncherScreen(
                state = state,
                onLaunch = ::launchApp,
                onUninstall = ::uninstallApp,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang daftar aplikasi (menangkap aplikasi baru/terhapus)
        lifecycleScope.launch {
            val data = withContext(Dispatchers.Default) { AppRepository.load(this@MainActivity) }
            state.update(data)
        }
    }

    private fun launchApp(app: AppInfo, from: Rect?) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        // Animasi zoom dari posisi ikon
        val options = from?.let {
            ActivityOptions.makeScaleUpAnimation(
                window.decorView, it.left, it.top, it.width(), it.height(),
            ).toBundle()
        }
        startActivity(intent, options)
    }

    private fun uninstallApp(app: AppInfo) {
        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName)))
    }
}
