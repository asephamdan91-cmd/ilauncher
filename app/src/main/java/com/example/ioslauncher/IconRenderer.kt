package com.example.ioslauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/**
 * Menyeragamkan ikon: hasilnya selalu bitmap persegi penuh (tanpa sudut transparan),
 * lalu di-clip dengan SquircleShape saat digambar di Compose.
 */
object IconRenderer {
    private const val SIZE = 192

    fun render(d: Drawable): ImageBitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        if (d is AdaptiveIconDrawable) {
            // Layer adaptive icon berukuran 108dp, area terlihat 72dp (bagian tengah 2/3).
            val off = SIZE / 4
            d.background?.let { it.setBounds(-off, -off, SIZE + off, SIZE + off); it.draw(c) }
            d.foreground?.let { it.setBounds(-off, -off, SIZE + off, SIZE + off); it.draw(c) }
        } else {
            // Ikon biasa: render dulu, ambil warna dominan untuk latar gradien.
            val tmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, SIZE, SIZE)
            d.draw(Canvas(tmp))
            val base = Palette.from(tmp).generate().getDominantColor(Color.parseColor("#5A5F6B"))
            val top = ColorUtils.blendARGB(base, Color.WHITE, 0.25f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, SIZE.toFloat(), top, base, Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), paint)
            val inset = (SIZE * 0.15f).toInt()
            d.setBounds(inset, inset, SIZE - inset, SIZE - inset)
            d.draw(c)
        }
        return bmp.asImageBitmap()
    }
}
