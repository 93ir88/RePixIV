// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.repixiv.app.LocalAppState
import com.repixiv.app.model.Artwork
import com.repixiv.app.model.MockArtworks
import com.repixiv.app.util.AdaptiveTopAppBar
import com.repixiv.app.util.BlurredBar
import com.repixiv.app.util.pageContentPadding
import com.repixiv.app.util.pageScrollModifiers
import com.repixiv.app.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun PageScaffold(
    title: String,
    padding: PaddingValues,
    content: @Composable (PaddingValues, androidx.compose.ui.Modifier) -> Unit,
) {
    val appState = LocalAppState.current
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, scrollBehavior) {
                AdaptiveTopAppBar(
                    title = title,
                    showTopAppBar = appState.showTopAppBar,
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                )
            }
        },
    ) { inner ->
        val contentPadding = pageContentPadding(
            PaddingValues(top = inner.calculateTopPadding()),
            padding,
        )
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            content(
                contentPadding,
                Modifier.pageScrollModifiers(
                    appState.enableScrollEndHaptic,
                    appState.showTopAppBar,
                    scrollBehavior,
                ),
            )
        }
    }
}

@Composable
fun FeedPage(padding: PaddingValues) {
    PageScaffold("Home", padding) { contentPadding, scrollModifier ->
        LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
            items(MockArtworks, key = { it.id }) { ArtworkCard(it) }
        }
    }
}

@Composable
private fun RemoteImage(url: String, circle: Boolean, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { view ->
            val req = Glide.with(view).load(url)
            (if (circle) req.circleCrop() else req.centerCrop()).into(view)
        },
    )
}

@Composable
private fun ArtworkCard(a: Artwork) {
    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(a.authorAvatarUrl, true, Modifier.size(40.dp).clip(CircleShape))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(a.authorName, style = MiuixTheme.textStyles.body1, fontSize = 15.sp)
                    Text(
                        a.title,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 13.sp,
                    )
                }
            }
            RemoteImage(
                a.imageUrl, false,
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .aspectRatio(3f / 2f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Text(
                a.tags.joinToString("  ") { "#$it" },
                color = MiuixTheme.colorScheme.primary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MiuixIcons.Favorites, null, Modifier.size(16.dp), MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text(fmt(a.likeCount), color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
                Icon(MiuixIcons.Show, null, Modifier.size(16.dp), MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text(fmt(a.viewCount), color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
            }
        }
    }
}

private fun fmt(n: Int): String = when {
    n >= 1_000_000 -> "${"%.1f".format(n / 1_000_000.0)}M"
    n >= 1_000 -> "${"%.1f".format(n / 1_000.0)}K"
    else -> n.toString()
}

@Composable
fun SearchPage(padding: PaddingValues) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    PageScaffold("Search", padding) { contentPadding, _ ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(12.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = "Search artworks, artists, tags…",
                useLabelAsPlaceholder = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("Pixiv search — coming soon", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}

@Composable
fun ProfilePage(padding: PaddingValues) {
    PageScaffold("Profile", padding) { contentPadding, _ ->
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Text(
                "Connect your Pixiv account — coming soon",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
