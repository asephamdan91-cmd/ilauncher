package com.example.ioslauncher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

sealed interface Entry
data class AppEntry(val key: String) : Entry
data class FolderEntry(val id: String, val name: String, val keys: List<String>) : Entry

/** Menyimpan susunan home screen (urutan ikon + folder) di SharedPreferences. */
object LayoutStore {
    private const val PREFS = "layout"
    private const val KEY = "entries"

    fun load(ctx: Context): List<Entry> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                when (o.getString("t")) {
                    "app" -> AppEntry(o.getString("k"))
                    "folder" -> {
                        val ks = o.getJSONArray("a")
                        FolderEntry(
                            o.getString("id"),
                            o.getString("n"),
                            (0 until ks.length()).map { ks.getString(it) },
                        )
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(ctx: Context, entries: List<Entry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            val o = JSONObject()
            when (e) {
                is AppEntry -> {
                    o.put("t", "app")
                    o.put("k", e.key)
                }
                is FolderEntry -> {
                    o.put("t", "folder")
                    o.put("id", e.id)
                    o.put("n", e.name)
                    o.put("a", JSONArray(e.keys))
                }
            }
            arr.put(o)
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
