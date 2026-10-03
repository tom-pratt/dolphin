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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
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
    /** Optional popup (e.g. a DropdownMenu) anchored to this action's button. */
    val popup: (@Composable () -> Unit)? = null,
)

private val BarHeight = 64.dp
private val ItemSize = 52.dp
private val ItemSpacing = 12.dp
private val PillPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
private val GroupSpacing = 10.dp

private val BarElevation = 6.dp
private const val PanelStrokeAlpha = 0f
private val PanelStrokeWidth = 1.dp

private val IsLight @Composable get() = MaterialTheme.colorScheme.surface.luminance() > 0.5f

private val PanelColor @Composable get() =
    if (IsLight) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
private val ContentColor @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val HighlightColor @Composable get() =
    if (IsLight) MaterialTheme.colorScheme.secondaryFixedDim else MaterialTheme.colorScheme.secondaryContainer
private const val HighlightAlpha = 1f// 0.6f
private val SelectedContentColor @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer

private val PanelStrokeColor @Composable get() = MaterialTheme.colorScheme.primary

/**
 * Floating bottom bar: a pill of tabs, each with its own selection highlight that crossfades to
 * its neighbour's as the selection moves, followed by any number of circular action buttons.
 *
 * @param selectedTabPosition fractional position of the selected tab (e.g. pager page + offset
 *   fraction), read only during drawing so the highlights can track a swipe without recomposing.
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
        color = PanelColor,
        shadowElevation = BarElevation,
        border = BorderStroke(PanelStrokeWidth, PanelStrokeColor.copy(alpha = PanelStrokeAlpha)),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
            modifier = Modifier.padding(PillPadding)
        ) {
            tabs.forEachIndexed { index, tab ->
                TabItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    highlightFraction = {
                        (1f - abs(selectedTabPosition() - index)).coerceIn(0f, 1f)
                    },
                    onClick = { onTabClick(index) },
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: DolphinNavTab,
    selected: Boolean,
    highlightFraction: () -> Float,
    onClick: () -> Unit,
) {
    val highlightColor = HighlightColor
    val contentColor by animateColorAsState(
        targetValue = if (selected) SelectedContentColor else ContentColor,
        animationSpec = tween(250),
        label = "tabContentColor",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ItemSize)
            .drawBehind {
                drawCircle(highlightColor.copy(alpha = HighlightAlpha * highlightFraction()))
            }
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
            HighlightColor.copy(alpha = HighlightAlpha)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "actionIndicatorColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (action.active) SelectedContentColor else ContentColor,
        animationSpec = tween(200),
        label = "actionContentColor",
    )

    Surface(
        shape = CircleShape,
        color = PanelColor,
        shadowElevation = BarElevation,
        border = BorderStroke(PanelStrokeWidth, PanelStrokeColor.copy(alpha = PanelStrokeAlpha)),
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
            action.popup?.invoke()
        }
    }
}
