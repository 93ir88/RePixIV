package com.repixiv.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
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

@Stable
class Navigator {
    val stack = mutableStateListOf<Screen>()

    fun push(screen: Screen) {
        if (stack.lastOrNull() == screen) return
        stack.add(screen)
    }

    fun openUser(userId: Long) = push(Screen.User(userId))

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator provided!") }
