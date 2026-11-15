package com.repixiv.app.pages

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.LocalNavigator
import com.repixiv.app.Screen
import com.repixiv.app.pixiv.NotificationItem
import com.repixiv.app.pixiv.Notifications
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.util.RemoteImage
import com.repixiv.app.util.relativeTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val BellIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Bell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5" +
                "s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z",
        ).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

/** Bell for the Home top bar: opens the notifications page and shows a red badge with the unread count. */
@Composable
fun NotificationBell(modifier: Modifier = Modifier) {
    val nav = LocalNavigator.current
    val count = Notifications.unreadCount
    // Keep the last non-zero number around so the badge doesn't flash "0" while it shrinks away.
    var last by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) { if (count > 0) last = count }
    val shown = if (count > 0) count else last

    IconButton(onClick = { nav.push(Screen.NotificationList) }, modifier = modifier) {
        Box {
            Icon(imageVector = BellIcon, contentDescription = "Notifications", tint = MiuixTheme.colorScheme.onSurface)
            AnimatedVisibility(
                visible = count > 0,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-5).dp),
                enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 380f)) + fadeIn(tween(150)),
                exit = scaleOut(tween(150)) + fadeOut(tween(150)),
            ) {
                Box(
                    Modifier
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .background(Color(0xFFFF3B30), RoundedCornerShape(50))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = shown,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                        label = "badgeCount",
                    ) { n ->
                        Text(
                            if (n > 9) "9+" else n.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Staggered(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(8) * 40L)
        shown = true
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(260)) +
            slideInVertically(spring(dampingRatio = 1f, stiffness = 380f)) { it / 4 } +
            expandVertically(spring(dampingRatio = 1f, stiffness = 380f)),
    ) { content() }
}

@Composable
private fun NotificationRow(n: NotificationItem, opening: Boolean, onClick: () -> Unit) {
    val nav = LocalNavigator.current
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val tint by animateColorAsState(
        if (n.seen) Color.Transparent else MiuixTheme.colorScheme.primary.copy(alpha = 0.10f),
        tween(500),
        label = "notificationTint",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(tint)
            .graphicsLayer { alpha = if (opening) 0.6f else 1f }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape)) {
            RemoteImage(n.userAvatar, true, Modifier.fillMaxSize())
            TapOverlay(Modifier.matchParentSize()) { nav.openUser(n.userId) }
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    n.userName,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(" replied to your comment.", fontSize = 15.sp, maxLines = 1)
            }
            if (n.stampUrl != null) {
                RemoteImage(n.stampUrl, false, Modifier.padding(top = 6.dp).size(56.dp), crop = false)
            } else if (n.text.isNotBlank()) {
                Text(
                    n.text,
                    color = summary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(relativeTime(n.date), color = summary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        AnimatedVisibility(visible = !n.seen, enter = scaleIn(spring(0.5f, 380f)), exit = scaleOut(tween(200))) {
            Box(
                Modifier
                    .padding(start = 8.dp, top = 6.dp)
                    .size(8.dp)
                    .background(MiuixTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

@Composable
fun NotificationsScreen() {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    var opening by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { Notifications.refresh() }
    // Opening the page counts as seeing the notifications: the tint fades and the bell badge clears.
    LaunchedEffect(Notifications.items.size) {
        delay(1200)
        Notifications.markAllSeen()
    }

    PageScaffold(
        title = "Notifications",
        padding = PaddingValues(0.dp),
        overlay = true,
        navigationIcon = { BackButton() },
    ) { contentPadding, scrollModifier, _ ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
                items(Notifications.items, key = { it.id }) { n ->
                    Staggered(Notifications.items.indexOf(n)) {
                        NotificationRow(n, opening = opening == n.id) {
                            if (opening != null) return@NotificationRow
                            opening = n.id
                            scope.launch {
                                try {
                                    val illust = Pixiv.illustDetail(n.illustId)
                                    nav.push(Screen.Comments(illust))
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Could not open post", Toast.LENGTH_LONG).show()
                                } finally {
                                    opening = null
                                }
                            }
                        }
                    }
                }
                item(key = "footer") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        when {
                            Notifications.refreshing && Notifications.items.isEmpty() -> InfiniteProgressIndicator()
                            Notifications.items.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No notifications yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Replies to your comments will show up here.",
                                    color = summary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
            // Opaque strip behind the title bar so scrolled rows never show through the (transparent) title.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(contentPadding.calculateTopPadding())
                    .background(MiuixTheme.colorScheme.surface),
            )
        }
    }
}
