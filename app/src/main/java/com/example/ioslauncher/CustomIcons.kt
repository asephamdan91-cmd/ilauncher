package com.example.ioslauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Ikon custom. Urutan pencarian:
 * 1. File pribadi: res/drawable-nodpi/icon_<package_name>.png (titik jadi garis bawah)
 * 2. Pack "Mac & iOS Custom icon pack" (ic_<nama>.png) lewat package name atau nama aplikasi, lihat [brands]
 * 3. Pack glyph lama (pack_1..pack_30.png) lewat package name atau nama aplikasi
 * Kalau tidak ada yang cocok, dipakai ikon asli aplikasi.
 */
object CustomIcons {
    private const val SIZE = 192

    // nomor ikon di pack -> 1 home, 2 menu, 3 dokumen, 4 chat, 5 user, 6 plus, 7 refresh,
    // 8 lonceng, 9 centang, 10 silang, 11 musik, 12 folder, 13 lokasi, 14 kontak, 15 sparkle,
    // 16 pengaturan, 17 galeri, 18 cari, 19 telepon, 20 komunitas, 21 berita, 22 youtube,
    // 23 instagram, 24 X, 25 tas kerja, 26 email, 27 sampah, 28 daur ulang, 29 statistik, 30 lampu
    /** Satu ikon di pack iOS: nama resource (ic_<res>), package yang memakainya, dan nama aplikasi (huruf kecil, tanpa spasi). */
    private class Brand(val res: String, val packages: List<String> = emptyList(), val labels: List<String> = emptyList())

    private val brands = listOf(
        Brand("1password", listOf("com.onepassword.android", "com.agilebits.onepassword"), listOf("1password")),
        Brand("airmail", labels = listOf("airmail")),
        Brand("beelinguapp", labels = listOf("beelinguapp")),
        Brand("brawlstars", listOf("com.supercell.brawlstars"), listOf("brawlstars")),
        Brand("calcbot", labels = listOf("calcbot")),
        Brand(
            "calculator",
            listOf(
                "com.google.android.calculator", "com.miui.calculator",
                "com.android.calculator2", "com.sec.android.app.popupcalculator",
            ),
            listOf("calculator", "kalkulator"),
        ),
        Brand("craft", labels = listOf("craft")),
        Brand("dayone", labels = listOf("dayone")),
        Brand("dribbble", labels = listOf("dribbble")),
        Brand("duolingo", listOf("com.duolingo"), listOf("duolingo")),
        Brand("excel", listOf("com.microsoft.office.excel"), listOf("excel")),
        Brand("figma", listOf("com.figma.mirror"), listOf("figma")),
        Brand("findmy", listOf("com.google.android.apps.adm"), listOf("findhub", "finddevice", "findmydevice", "findmy")),
        Brand(
            "finder",
            listOf(
                "com.google.android.apps.nbu.files", "com.mi.android.globalFileexplorer",
                "com.android.fileexplorer", "com.sec.android.app.myfiles",
            ),
            listOf("files", "filemanager", "fileexplorer", "pengelolafile"),
        ),
        Brand("fortnite", listOf("com.epicgames.fortnite"), listOf("fortnite")),
        Brand("home", listOf("com.google.android.apps.chromecast.app"), listOf("googlehome")),
        Brand("launchcenter", labels = listOf("launchcenter")),
        Brand("maps", listOf("com.google.android.apps.maps"), listOf("maps", "peta")),
        Brand("messenger", listOf("com.facebook.orca"), listOf("messenger")),
        Brand("mimo", listOf("com.getmimo"), listOf("mimo")),
        Brand("minecraft", listOf("com.mojang.minecraftpe"), listOf("minecraft")),
        Brand(
            "music",
            listOf("com.miui.player", "com.sec.android.app.music"),
            listOf("music", "musik", "mi musik"),
        ),
        Brand("netflix", listOf("com.netflix.mediaclient"), listOf("netflix")),
        Brand("notability", labels = listOf("notability")),
        Brand("onedrive", listOf("com.microsoft.skydrive"), listOf("onedrive")),
        Brand("onenote", listOf("com.microsoft.office.onenote"), listOf("onenote")),
        Brand("outlook", listOf("com.microsoft.office.outlook"), listOf("outlook")),
        Brand(
            "photos",
            listOf(
                "com.google.android.apps.photos", "com.miui.gallery",
                "com.sec.android.gallery3d", "com.android.gallery3d",
            ),
            listOf("photos", "foto", "gallery", "galeri"),
        ),
        Brand("picstart", labels = listOf("picstart")),
        Brand("podcasts", listOf("com.google.android.apps.podcasts"), listOf("podcasts")),
        Brand("powerpoint", listOf("com.microsoft.office.powerpoint"), listOf("powerpoint")),
        Brand("rainbowsix", labels = listOf("rainbowsixmobile", "rainbowsix")),
        Brand("reddit", listOf("com.reddit.frontpage"), listOf("reddit")),
        Brand("reminder", listOf("com.google.android.apps.tasks"), listOf("reminders", "pengingat", "tasks")),
        Brand(
            "safari",
            listOf(
                "com.android.chrome", "com.mi.globalbrowser",
                "com.android.browser", "com.sec.android.app.sbrowser",
            ),
            listOf("chrome", "browser"),
        ),
        Brand("settings", listOf("com.android.settings"), listOf("settings", "pengaturan")),
        Brand("sketch", labels = listOf("sketch")),
        Brand("slack", listOf("com.Slack"), listOf("slack")),
        Brand("spark", listOf("com.readdle.spark"), listOf("spark")),
        Brand("spotify", listOf("com.spotify.music"), listOf("spotify")),
        Brand("swift", labels = listOf("swift")),
        Brand("teams", listOf("com.microsoft.teams"), listOf("teams")),
        Brand("things", labels = listOf("things")),
        Brand(
            "tiktok",
            listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
            listOf("tiktok"),
        ),
        Brand("twitch", listOf("tv.twitch.android.app"), listOf("twitch")),
        Brand("vectornator", labels = listOf("vectornator")),
        Brand(
            "voice",
            listOf(
                "com.google.android.apps.recorder", "com.android.soundrecorder",
                "com.sec.android.app.voicenote", "com.miui.soundrecorder",
            ),
            listOf("voicememos", "recorder", "perekamsuara", "perekam"),
        ),
        Brand("word", listOf("com.microsoft.office.word"), listOf("word")),

        // Ikon iOS 13 (dari file Sketch Apple App Icon iOS 13)
        Brand(
            "phone",
            listOf("com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer"),
            listOf("phone", "telepon", "dialer"),
        ),
        Brand(
            "messages",
            listOf("com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging"),
            listOf("messages", "pesan", "messaging"),
        ),
        Brand(
            "camera",
            listOf("com.android.camera", "com.android.camera2", "com.google.android.GoogleCamera", "com.sec.android.app.camera"),
            listOf("camera", "kamera"),
        ),
        Brand(
            "clock",
            listOf("com.google.android.deskclock", "com.android.deskclock", "com.sec.android.app.clockpackage"),
            listOf("clock", "jam"),
        ),
        Brand(
            "notes",
            listOf("com.miui.notes", "com.google.android.keep", "com.samsung.android.app.notes"),
            listOf("notes", "catatan", "keep"),
        ),
        Brand("weather", listOf("com.miui.weather2"), listOf("weather", "cuaca")),
        Brand("mail", listOf("com.google.android.gm"), listOf("gmail", "mail", "email")),
        Brand("wallet", listOf("com.google.android.apps.walletnfcrel"), listOf("wallet", "dompet")),
        Brand("tips", labels = listOf("tips")),
        Brand("appstore", listOf("com.android.vending"), listOf("playstore", "googleplay")),
        Brand("numbers", listOf("com.google.android.apps.docs.editors.sheets"), listOf("sheets")),
        Brand("pages", listOf("com.google.android.apps.docs.editors.docs"), listOf("docs")),
        Brand("keynote", listOf("com.google.android.apps.docs.editors.slides"), listOf("slides")),
        Brand("health", listOf("com.google.android.apps.fitness"), listOf("fit", "health")),
    )

    private val brandByPackage: Map<String, String> =
        brands.flatMap { b -> b.packages.map { it to b.res } }.toMap()

    private val brandByLabel: Map<String, String> =
        brands.flatMap { b -> b.labels.map { l -> l.filter { it.isLetterOrDigit() } to b.res } }.toMap()

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

        val norm = label.lowercase().filter { it.isLetterOrDigit() }

        // Pack iOS didahulukan dari pack glyph lama
        val brand = brandByPackage[packageName] ?: brandByLabel[norm]
        if (brand != null) {
            val brandId = drawableId(ctx, "ic_$brand")
            if (brandId != 0) return render(ctx, brandId)
        }

        val n = byPackage[packageName]
            ?: byLabel[norm]
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
