package com.example.ioslauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: ImageBitmap,
)

data class HomeData(val grid: List<AppInfo>, val dock: List<AppInfo>)

object AppRepository {

    fun load(ctx: Context): HomeData {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val all = pm.queryIntentActivities(main, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = pkg,
                    component = ComponentName(pkg, ri.activityInfo.name),
                    icon = IconRenderer.render(ri.loadIcon(pm)),
                )
            }
            .sortedBy { it.label.lowercase() }

        // Dock: telepon, pesan, browser, kamera (kalau ditemukan)
        val dockPackages = listOfNotNull(
            pkgFor(pm, Intent(Intent.ACTION_DIAL)),
            pkgFor(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)),
            pkgFor(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)),
            pkgFor(pm, Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
        ).distinct()

        val dock = dockPackages.mapNotNull { p -> all.firstOrNull { it.packageName == p } }
        val grid = all.filter { it !in dock }
        return HomeData(grid, dock)
    }

    private fun pkgFor(pm: PackageManager, intent: Intent): String? =
        pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?.takeIf { it != "android" }
}
