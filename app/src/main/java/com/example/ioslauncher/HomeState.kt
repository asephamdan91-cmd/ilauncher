package com.example.ioslauncher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface HomeItem {
    val id: String
}

data class AppItem(val app: AppInfo) : HomeItem {
    override val id: String get() = app.key
}

data class FolderItem(override val id: String, val name: String, val apps: List<AppInfo>) : HomeItem

class HomeState(private val ctx: Context) {

    /** Aplikasi yang tampil di grid (di luar dock), berdasarkan key. */
    var apps by mutableStateOf<Map<String, AppInfo>>(emptyMap())
        private set

    var dock by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    /** Semua aplikasi (untuk pencarian). */
    var all by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var entries by mutableStateOf<List<Entry>>(LayoutStore.load(ctx))
        private set

    private var homeSignalValue by mutableIntStateOf(0)
    val homeSignal: Int get() = homeSignalValue

    fun pressHome() {
        homeSignalValue += 1
    }

    fun update(data: HomeData) {
        dock = data.dock
        apps = data.grid.associateBy { it.key }
        all = (data.grid + data.dock).sortedBy { it.label.lowercase() }
        val reconciled = reconcile(entries, data.grid.map { it.key })
        if (reconciled != entries) commit(reconciled)
    }

    fun items(): List<HomeItem> = entries.mapNotNull { e ->
        when (e) {
            is AppEntry -> apps[e.key]?.let { AppItem(it) }
            is FolderEntry -> {
                val list = e.keys.mapNotNull { apps[it] }
                if (list.isEmpty()) null else FolderItem(e.id, e.name, list)
            }
        }
    }

    /** Seret [dragKey] ke atas [targetKey]: buat folder baru, atau masuk ke folder. */
    fun dropOnto(dragKey: String, targetKey: String) {
        if (dragKey == targetKey) return
        val list = entries.toMutableList()
        val dragIdx = list.indexOfFirst { it is AppEntry && it.key == dragKey }
        if (dragIdx < 0) return
        val targetIdx = list.indexOfFirst {
            (it is AppEntry && it.key == targetKey) || (it is FolderEntry && it.id == targetKey)
        }
        if (targetIdx < 0) return
        val newEntry: Entry = when (val target = list[targetIdx]) {
            is FolderEntry -> target.copy(keys = target.keys + dragKey)
            is AppEntry -> FolderEntry("f" + System.currentTimeMillis(), "Folder", listOf(target.key, dragKey))
        }
        list[targetIdx] = newEntry
        list.removeAt(dragIdx)
        commit(list)
    }

    fun removeFromFolder(folderId: String, key: String) {
        val list = entries.toMutableList()
        val idx = list.indexOfFirst { it is FolderEntry && it.id == folderId }
        if (idx < 0) return
        val f = list[idx] as FolderEntry
        val remaining = f.keys.filter { it != key }
        list.removeAt(idx)
        val insert = mutableListOf<Entry>()
        if (remaining.size >= 2) {
            insert.add(f.copy(keys = remaining))
        } else if (remaining.size == 1) {
            insert.add(AppEntry(remaining[0]))
        }
        insert.add(AppEntry(key))
        list.addAll(idx, insert)
        commit(list)
    }

    fun dissolveFolder(folderId: String) {
        val list = entries.toMutableList()
        val idx = list.indexOfFirst { it is FolderEntry && it.id == folderId }
        if (idx < 0) return
        val f = list[idx] as FolderEntry
        list.removeAt(idx)
        list.addAll(idx, f.keys.map { AppEntry(it) })
        commit(list)
    }

    fun renameFolder(folderId: String, name: String) {
        commit(entries.map { if (it is FolderEntry && it.id == folderId) it.copy(name = name) else it })
    }

    private fun commit(list: List<Entry>) {
        entries = list
        LayoutStore.save(ctx, list)
    }

    private fun reconcile(current: List<Entry>, validKeys: List<String>): List<Entry> {
        val valid = validKeys.toSet()
        val cleaned = current.mapNotNull { e ->
            when (e) {
                is AppEntry -> if (e.key in valid) e else null
                is FolderEntry -> {
                    val ks = e.keys.filter { it in valid }
                    when {
                        ks.size >= 2 -> e.copy(keys = ks)
                        ks.size == 1 -> AppEntry(ks[0])
                        else -> null
                    }
                }
            }
        }
        val used = HashSet<String>()
        cleaned.forEach { e ->
            when (e) {
                is AppEntry -> used.add(e.key)
                is FolderEntry -> used.addAll(e.keys)
            }
        }
        val fresh = validKeys.filter { it !in used }.map { AppEntry(it) }
        return cleaned + fresh
    }
}
