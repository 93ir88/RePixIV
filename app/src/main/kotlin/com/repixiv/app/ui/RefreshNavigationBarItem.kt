package com.repixiv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Like Miuix's NavigationBarItem, but its icon can turn into a spinning refresh icon ([spinning]). */
@Composable
fun RowScope.RefreshNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    spinning: Boolean,
    mode: NavigationBarDisplayMode,
    badge: (@Composable () -> Unit)? = null,
) {
    val base = MiuixTheme.colorScheme.onSurface
    val tint = if (selected) base else base.copy(alpha = NavigationBarDefaults.UnselectedAlpha)
    val showLabel = mode == NavigationBarDisplayMode.IconAndText ||
        (mode == NavigationBarDisplayMode.IconWithSelectedLabel && selected)
    val labelRoom = mode != NavigationBarDisplayMode.IconOnly

    Column(
        modifier = Modifier
            .height(NavigationBarDefaults.ItemHeight)
            .weight(1f)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (labelRoom) Arrangement.Top else Arrangement.Center,
    ) {
        Box(Modifier.padding(top = if (labelRoom) NavigationBarDefaults.IconTopPadding else 0.dp)) {
            RefreshableIcon(
                icon = icon,
                spinning = spinning,
                tint = tint,
                modifier = Modifier.size(NavigationBarDefaults.IconSize),
                contentDescription = label,
            )
            badge?.let { Box(Modifier.align(Alignment.TopEnd)) { it() } }
        }
        if (labelRoom) {
            Text(
                modifier = Modifier
                    .padding(bottom = NavigationBarDefaults.BottomPadding)
                    .graphicsLayer { alpha = if (showLabel) 1f else 0f },
                text = label,
                color = tint,
                textAlign = TextAlign.Center,
                fontSize = NavigationBarDefaults.LabelFontSize,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
