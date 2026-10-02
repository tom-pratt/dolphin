// SPDX-License-Identifier: GPL-2.0-or-later

package org.dolphinemu.dolphinemu.ui.mainv2

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
class DolphinNavTab(
    val label: String,
    val icon: @Composable () -> Unit,
)

@Immutable
class DolphinNavAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val active: Boolean = false,
)

private val BarHeight = 64.dp
private val ItemSize = 52.dp
private val ItemSpacing = 12.dp
private val PillPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
private val GroupSpacing = 10.dp
private const val HighlightAlpha = 0.2f

private val BarElevation = 6.dp

private const val PanelStrokeAlpha = 0f
private val PanelStrokeWidth = 1.dp

/**
 * Floating bottom bar: a pill of tabs with a sliding selection indicator, followed by
 * any number of circular action buttons.
 *
 * @param selectedTabPosition fractional position of the selected tab (e.g. pager page + offset
 *   fraction), read only during layout so the indicator can track a swipe without recomposing.
 */
@Composable
fun DolphinNavBar(
    tabs: List<DolphinNavTab>,
    selectedTabPosition: () -> Float,
    onTabClick: (Int) -> Unit,
    actions: List<DolphinNavAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GroupSpacing, Alignment.CenterHorizontally),
        modifier = modifier
    ) {
        if (tabs.isNotEmpty()) {
            TabPill(tabs, selectedTabPosition, onTabClick)
        }
        actions.forEach { action ->
            ActionButton(action)
        }
    }
}

@Composable
private fun TabPill(
    tabs: List<DolphinNavTab>,
    selectedTabPosition: () -> Float,
    onTabClick: (Int) -> Unit,
) {
    val selectedIndex by remember(tabs.size) {
        derivedStateOf { selectedTabPosition().roundToInt().coerceIn(0, tabs.lastIndex) }
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = BarElevation,
        border = BorderStroke(PanelStrokeWidth, MaterialTheme.colorScheme.primary.copy(alpha = PanelStrokeAlpha)),
    ) {
        Box(Modifier.padding(PillPadding)) {
            Box(
                Modifier
                    .offset {
                        val position = selectedTabPosition().coerceIn(0f, tabs.lastIndex.toFloat())
                        IntOffset(((ItemSize + ItemSpacing).toPx() * position).roundToInt(), 0)
                    }
                    .size(ItemSize)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = HighlightAlpha), CircleShape)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ItemSpacing)) {
                tabs.forEachIndexed { index, tab ->
                    TabItem(
                        tab = tab,
                        selected = index == selectedIndex,
                        onClick = { onTabClick(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: DolphinNavTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(250),
        label = "tabContentColor",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ItemSize)
            .clip(CircleShape)
            .semantics { contentDescription = tab.label }
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            tab.icon()
        }
    }
}

@Composable
private fun ActionButton(action: DolphinNavAction) {
    val indicatorColor by animateColorAsState(
        targetValue = if (action.active) {
            MaterialTheme.colorScheme.primary.copy(alpha = HighlightAlpha)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "actionIndicatorColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (action.active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "actionContentColor",
    )

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = BarElevation,
        border = BorderStroke(PanelStrokeWidth, MaterialTheme.colorScheme.primary.copy(alpha = PanelStrokeAlpha)),
        modifier = Modifier.size(BarHeight),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(ItemSize)
                    .clip(CircleShape)
                    .background(indicatorColor)
                    .semantics { selected = action.active }
                    .clickable(onClick = action.onClick, role = Role.Button)
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = contentColor,
                )
            }
        }
    }
}
