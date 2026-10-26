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
) {
    val key: String get() = component.flattenToString()
}

data class HomeData(val grid: List<AppInfo>, val dock: List<AppInfo>)

object AppRepository {

    /** Cache ikon: key = komponen + waktu update paket, jadi ikon tidak dirender ulang tiap onResume. */
    private val iconCache = HashMap<String, ImageBitmap>()

    private fun updateTime(pm: PackageManager, pkg: String): Long =
        try { pm.getPackageInfo(pkg, 0).lastUpdateTime } catch (e: Exception) { 0L }

    @Synchronized
    fun load(ctx: Context): HomeData {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val all = pm.queryIntentActivities(main, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                val label = ri.loadLabel(pm).toString()
                val component = ComponentName(pkg, ri.activityInfo.name)
                val cacheKey = component.flattenToString() + "@" + updateTime(pm, pkg) + "|" + label
                val icon = iconCache.getOrPut(cacheKey) {
                    CustomIcons.find(ctx, pkg, label) ?: IconRenderer.render(ri.loadIcon(pm))
                }
                AppInfo(label = label, packageName = pkg, component = component, icon = icon)
            }
            .sortedBy { it.label.lowercase() }

        // Dock: telepon, pesan, browser, kamera (kalau ditemukan)
        val dockPackages = listOfNotNull(
            pkgFor(pm, Intent(Intent.ACTION_DIAL)),
            pkgFor(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)),
            pkgFor(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)),
            pkgFor(pm, Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
        ).distinct()

        // Buang cache untuk app yang sudah tidak terpasang
        val liveKeys = all.map { it.key }.toSet()
        iconCache.keys.removeAll { k -> k.substringBefore('@') !in liveKeys }

        val dock = dockPackages.mapNotNull { p -> all.firstOrNull { it.packageName == p } }
        val grid = all.filter { it !in dock }
        return HomeData(grid, dock)
    }

    private fun pkgFor(pm: PackageManager, intent: Intent): String? =
        pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?.takeIf { it != "android" }
}
