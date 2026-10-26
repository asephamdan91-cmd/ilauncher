package com.example.ioslauncher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val home = mutableStateOf(HomeData(emptyList(), emptyList()))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparent = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = transparent, navigationBarStyle = transparent)
        setContent {
            LauncherScreen(
                home = home.value,
                onLaunch = ::launchApp,
                onUninstall = ::uninstallApp,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang daftar aplikasi (menangkap aplikasi baru/terhapus)
        lifecycleScope.launch {
            home.value = withContext(Dispatchers.Default) { AppRepository.load(this@MainActivity) }
        }
    }

    private fun launchApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        startActivity(intent)
    }

    private fun uninstallApp(app: AppInfo) {
        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
    }
}
