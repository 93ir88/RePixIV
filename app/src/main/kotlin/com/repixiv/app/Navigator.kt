package com.repixiv.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.repixiv.app.pixiv.Illust
import com.repixiv.app.pixiv.TrendTag

sealed interface Screen {
    data class Viewer(val illust: Illust, val page: Int = 0) : Screen
    data class User(val userId: Long) : Screen
    data class Post(val illust: Illust) : Screen
    data class Comments(val illust: Illust) : Screen
    data class Trending(val tags: List<TrendTag>) : Screen
    data class FeedList(val title: String, val kind: String) : Screen
    data object AccountSettings : Screen
}

/** One screen on the stack. Entries stay composed while covered, so going back keeps their state. */
@Stable
class NavEntry(val id: Long, val screen: Screen) {
    var closing by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
}

@Stable
class Navigator {
    /** Includes entries that are animating out. */
    val entries = mutableStateListOf<NavEntry>()
    private var nextId = 0L

    val hasOpen: Boolean get() = entries.any { !it.closing }

    fun push(screen: Screen) {
        if (entries.lastOrNull { !it.closing }?.screen == screen) return
        entries.add(NavEntry(nextId++, screen))
    }

    fun openUser(userId: Long) = push(Screen.User(userId))

    fun pop() {
        entries.lastOrNull { !it.closing }?.closing = true
    }

    fun remove(entry: NavEntry) {
        entries.remove(entry)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator provided!") }
