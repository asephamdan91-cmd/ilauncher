package com.example.ioslauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Ikon custom. Urutan pencarian:
 * 1. File pribadi: res/drawable-nodpi/icon_<package_name>.png (titik jadi garis bawah)
 * 2. Pack bawaan (pack_1..pack_30.png) lewat package name atau nama aplikasi
 * Kalau tidak ada yang cocok, dipakai ikon asli aplikasi.
 */
object CustomIcons {
    private const val SIZE = 192

    // nomor ikon di pack -> 1 home, 2 menu, 3 dokumen, 4 chat, 5 user, 6 plus, 7 refresh,
    // 8 lonceng, 9 centang, 10 silang, 11 musik, 12 folder, 13 lokasi, 14 kontak, 15 sparkle,
    // 16 pengaturan, 17 galeri, 18 cari, 19 telepon, 20 komunitas, 21 berita, 22 youtube,
    // 23 instagram, 24 X, 25 tas kerja, 26 email, 27 sampah, 28 daur ulang, 29 statistik, 30 lampu
    private val byLabel: Map<String, Int> = mapOf(
        "phone" to 19, "telepon" to 19, "dialer" to 19,
        "contacts" to 14, "kontak" to 14,
        "gallery" to 17, "galeri" to 17, "photos" to 17, "foto" to 17,
        "filemanager" to 12, "files" to 12, "pengelolafile" to 12,
        "gmail" to 26, "email" to 26, "mail" to 26,
        "messages" to 4, "pesan" to 4, "messaging" to 4,
        "settings" to 16, "pengaturan" to 16,
        "fmradio" to 11, "music" to 11, "musik" to 11,
        "youtube" to 22, "instagram" to 23, "x" to 24, "twitter" to 24,
        "maps" to 13, "peta" to 13,
        "search" to 18, "cari" to 18,
        "gemini" to 15, "assistant" to 15,
        "digitalwellbeing" to 29,
        "notes" to 3, "catatan" to 3, "news" to 21, "berita" to 21,
        "tips" to 30, "trash" to 27, "sampah" to 27,
    )

    private val byPackage: Map<String, Int> = mapOf(
        "com.google.android.dialer" to 19, "com.android.dialer" to 19,
        "com.android.contacts" to 14, "com.google.android.contacts" to 14,
        "com.miui.gallery" to 17, "com.google.android.apps.photos" to 17,
        "com.mi.android.globalFileexplorer" to 12, "com.google.android.apps.nbu.files" to 12,
        "com.google.android.gm" to 26,
        "com.google.android.apps.messaging" to 4, "com.android.mms" to 4,
        "com.android.settings" to 16,
        "com.miui.fm" to 11, "com.android.fmradio" to 11,
        "com.google.android.youtube" to 22,
        "com.instagram.android" to 23,
        "com.twitter.android" to 24,
        "com.google.android.apps.maps" to 13,
        "com.google.android.apps.bard" to 15,
        "com.google.android.apps.wellbeing" to 29,
    )

    fun find(ctx: Context, packageName: String, label: String): ImageBitmap? {
        val own = "icon_" + packageName.lowercase().replace('.', '_').replace('-', '_')
        val ownId = drawableId(ctx, own)
        if (ownId != 0) return render(ctx, ownId)

        val n = byPackage[packageName]
            ?: byLabel[label.lowercase().filter { it.isLetterOrDigit() }]
            ?: return null
        val id = drawableId(ctx, "pack_$n")
        return if (id != 0) render(ctx, id) else null
    }

    private fun drawableId(ctx: Context, name: String): Int =
        ctx.resources.getIdentifier(name, "drawable", ctx.packageName)

    private fun render(ctx: Context, id: Int): ImageBitmap? {
        val d = ctx.getDrawable(id) ?: return null
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, SIZE, SIZE)
        d.draw(Canvas(bmp))
        return bmp.asImageBitmap()
    }
}
