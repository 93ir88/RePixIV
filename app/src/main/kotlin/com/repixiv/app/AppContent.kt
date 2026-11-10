// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import com.repixiv.app.ui.isInDarkTheme
import com.repixiv.app.pages.AccountSettingsScreen
import com.repixiv.app.pages.CommentsScreen
import com.repixiv.app.pages.FeedListScreen
import com.repixiv.app.pages.UserScreen
import com.repixiv.app.pages.ViewerScreen
import com.repixiv.app.pages.FeedPage
import com.repixiv.app.pages.ProfilePage
import com.repixiv.app.pages.SearchPage
import com.repixiv.app.pages.SettingsPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

object UIConstants {
    val PAGE_TITLES = listOf("Home", "Search", "Profile", "Settings")
    const val PAGE_COUNT = 4
}

@Composable
fun AppContent(padding: PaddingValues) {
    val pagerState = rememberPagerState(pageCount = { UIConstants.PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val mainPagerState = remember(pagerState) { MainPagerState(pagerState, scope) }
    LaunchedEffect(mainPagerState.pagerState.currentPage) { mainPagerState.syncPage() }

    BackHandler(enabled = mainPagerState.selectedPage != 0) { mainPagerState.animateToPage(0) }

    val navigationItems = remember {
        listOf(
            NavigationItem(UIConstants.PAGE_TITLES[0], MiuixIcons.Home),
            NavigationItem(UIConstants.PAGE_TITLES[1], MiuixIcons.Search),
            NavigationItem(UIConstants.PAGE_TITLES[2], MiuixIcons.Contacts),
            NavigationItem(UIConstants.PAGE_TITLES[3], MiuixIcons.Settings),
        )
    }

    val nav = remember { Navigator() }
    CompositionLocalProvider(LocalNavigator provides nav) {
        Box(modifier = Modifier.fillMaxSize()) {
            CompactScreenLayout(
                navigationItems = navigationItems,
                padding = padding,
                mainPagerState = mainPagerState,
            )
            OverlayHost(nav)
        }
    }
    BackHandler(enabled = nav.stack.isNotEmpty()) { nav.pop() }

}

private data class OverlayEntry(val depth: Int, val screen: Screen?)

/** Miuix-style page transitions: new screens spring in from the right, the one below drifts left. */
@Composable
private fun OverlayHost(nav: Navigator) {
    val entry = OverlayEntry(nav.stack.size, nav.stack.lastOrNull())
    AnimatedContent(
        targetState = entry,
        contentKey = { it.screen },
        transitionSpec = {
            val push = targetState.depth > initialState.depth
            val offsetSpec = spring<IntOffset>(dampingRatio = 1f, stiffness = 380f)
            val transition = if (push) {
                (slideInHorizontally(offsetSpec) { it } + fadeIn(tween(120))) togetherWith
                    (slideOutHorizontally(offsetSpec) { -it / 4 } + fadeOut(tween(220)))
            } else {
                (slideInHorizontally(offsetSpec) { -it / 4 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(offsetSpec) { it } + fadeOut(tween(160)))
            }
            transition.apply { targetContentZIndex = if (push) 1f else -1f }
        },
        modifier = Modifier.fillMaxSize(),
        label = "overlay",
    ) { e ->
        val top = e.screen
        if (top != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface)
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                when (top) {
                    is Screen.Viewer -> ViewerScreen(top.illust, top.page)
                    is Screen.User -> UserScreen(top.userId)
                    is Screen.Comments -> CommentsScreen(top.illust)
                    is Screen.FeedList -> FeedListScreen(top.title, top.kind)
                    Screen.AccountSettings -> AccountSettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun CompactScreenLayout(
    navigationItems: List<NavigationItem>,
    padding: PaddingValues,
    mainPagerState: MainPagerState,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppNavigationBar(
                navigationItems = navigationItems,
                mainPagerState = mainPagerState,
                backdrop = backdrop,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            AppPager(
                padding = innerPadding,
                pagerState = mainPagerState.pagerState,
                modifier = Modifier
                    .padding(
                        top = padding.calculateTopPadding(),
                        start = padding.calculateStartPadding(LocalLayoutDirection.current),
                        end = padding.calculateEndPadding(LocalLayoutDirection.current),
                    )
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun AppNavigationBar(
    navigationItems: List<NavigationItem>,
    mainPagerState: MainPagerState,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val blurActive = appState.enableBlur && backdrop != null &&
        top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported()
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val page = mainPagerState.selectedPage

    AnimatedVisibility(
        visible = appState.showNavigationBar,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        AnimatedVisibility(
            visible = !appState.useFloatingNavigationBar,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (blurActive) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = RectangleShape,
                                blurRadius = 25f,
                                colors = BlurDefaults.blurColors(
                                    blendColors = listOf(
                                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
                                    ),
                                ),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .background(barColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .then(modifier),
            ) {
                NavigationBar(
                    modifier = Modifier,
                    color = barColor,
                    mode = NavigationBarDisplayMode.entries[appState.navigationBarMode],
                ) {
                    navigationItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = page == index,
                            onClick = { mainPagerState.animateToPage(index) },
                            icon = item.icon,
                            label = item.label,
                            badge = navigationItemBadge(index, appState.showNavigationBarBadge),
                        )
                    }
                }
            }
        }
        if (appState.useFloatingNavigationBar) {
            val floatingBarColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
            val floatingBarShape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius)
            val isDark = isInDarkTheme()
            val floatingHighlight = remember(isDark) {
                if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
            }
            Box(modifier = modifier) {
                FloatingNavigationBar(
                    modifier = if (blurActive) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = floatingBarShape,
                            blurRadius = 25f,
                            colors = BlurDefaults.blurColors(
                                blendColors = listOf(
                                    BlendColorEntry(color = MiuixTheme.colorScheme.surfaceContainer.copy(0.6f)),
                                ),
                            ),
                            highlight = floatingHighlight,
                        )
                    } else {
                        Modifier
                    },
                    color = floatingBarColor,
                    horizontalAlignment = FloatingNavigationBarAlignment.fromInt(appState.floatingNavigationBarPosition)
                        .toAlignment(),
                ) {
                    navigationItems.forEachIndexed { index, item ->
                        FloatingNavigationBarItem(
                            selected = page == index,
                            onClick = { mainPagerState.animateToPage(index) },
                            icon = item.icon,
                            label = item.label,
                            badge = navigationItemBadge(index, appState.showNavigationBarBadge),
                        )
                    }
                }
            }
        }
    }
}

enum class FloatingNavigationBarAlignment(val value: Int) {
    Center(0),
    Start(1),
    End(2),
    ;

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: Center
    }
}

private fun FloatingNavigationBarAlignment.toAlignment(): Alignment.Horizontal = when (this) {
    FloatingNavigationBarAlignment.Center -> Alignment.CenterHorizontally
    FloatingNavigationBarAlignment.Start -> Alignment.Start
    FloatingNavigationBarAlignment.End -> Alignment.End
}

private fun navigationItemBadge(index: Int, show: Boolean): (@Composable () -> Unit)? = when {
    !show -> null
    index == 1 -> ({ Badge { Text("8") } })
    index == 3 -> ({ Badge() })
    else -> null
}

@Composable
fun AppPager(
    padding: PaddingValues,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec,
    )
    val nativeConnection = PagerDefaults.pageNestedScrollConnection(pagerState, Orientation.Horizontal)
    HorizontalPager(
        state = pagerState,
        modifier = modifier.pagerGestureOverride(
            pagerState = pagerState,
            mode = appState.pagerInterceptionMode,
            enabled = appState.enablePageUserScroll,
        ),
        userScrollEnabled = appState.enablePageUserScroll &&
            appState.pagerInterceptionMode != PagerInterceptionMode.CrossAxisInterceptor.ordinal,
        pageNestedScrollConnection = if (
            appState.pagerInterceptionMode == PagerInterceptionMode.CrossAxisInterceptor.ordinal
        ) {
            PagerGestureNestedScrollConnection
        } else {
            nativeConnection
        },
        beyondViewportPageCount = UIConstants.PAGE_COUNT,
        verticalAlignment = Alignment.Top,
        flingBehavior = flingBehavior,
        pageContent = { page ->
            when (page) {
                0 -> FeedPage(padding = padding)
                1 -> SearchPage(padding = padding)
                2 -> ProfilePage(padding = padding)
                else -> SettingsPage(padding = padding)
            }
        },
    )
}

@Stable
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return
        navJob?.cancel()
        selectedPage = targetIndex
        isNavigating = true
        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.springAnimateToPage(targetIndex)
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}
