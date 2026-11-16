package com.repixiv.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow

/** Fired with the page index when the user taps the bottom-bar icon of the page they're already on. */
object TabReselect {
    val events = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    fun fire(index: Int) {
        events.tryEmit(index)
    }

    private val tapped = HashSet<Int>()

    /** True only the first time this tab is tapped since the app process started. */
    fun firstTap(index: Int): Boolean = tapped.add(index)

    /** Forget which tabs/sections were already opened, so the first-open scroll + refresh happens again (new login). */
    fun resetFirsts() {
        tapped.clear()
        sections.clear()
    }

    private val sections = HashSet<Int>()

    /** True only the first time this section (sub-tab) of a page is opened since the app process started. */
    fun firstSection(page: Int, section: Int): Boolean = sections.add(page * 100 + section)

    /** Pages that are refreshing right now; their bottom-bar icon turns into a spinning refresh icon. */
    private var refreshingPages by mutableStateOf(emptySet<Int>())

    fun setRefreshing(index: Int, value: Boolean) {
        refreshingPages = if (value) refreshingPages + index else refreshingPages - index
    }

    fun isRefreshing(index: Int) = index in refreshingPages
}
