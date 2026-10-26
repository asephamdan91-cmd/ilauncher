# iLauncher

Launcher Android bergaya iOS (Kotlin + Jetpack Compose).

Fitur: ikon squircle seragam, halaman swipe + indikator, dock, mode edit goyang
(tahan lama ikon: seret ke ikon lain = buat folder), wallpaper sistem tetap tampil.

## Build lewat GitHub
1. Buat repo baru di GitHub, upload semua isi folder ini (termasuk folder `.github`).
2. Buka tab **Actions** -> workflow **Build APK** -> tunggu hijau (sekitar 3-6 menit).
3. Buka run tersebut, unduh artifact **ilauncher-debug-apk**, ekstrak, dapat `app-debug.apk`.

## Pasang
Install APK (izinkan "install dari sumber tidak dikenal"), tekan tombol Home,
pilih **iLauncher** -> "Selalu".

## Ikon custom
Pack ikon Figma (pack_1..pack_30.png) sudah ada di `app/src/main/res/drawable-nodpi/` dan
otomatis dipakai untuk aplikasi yang cocok (lihat tabel di `CustomIcons.kt`).
Untuk ikon pribadi, taruh `icon_<package_name>.png` (huruf kecil, titik jadi garis bawah),
contoh `icon_com_whatsapp.png`. File pribadi didahulukan dari pack.
