@file:OptIn(ExperimentalMaterial3Api::class)

package org.dolphinemu.dolphinemu.ui.mainv2

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.dolphinemu.dolphinemu.R
import org.dolphinemu.dolphinemu.model.GameFile
import org.dolphinemu.dolphinemu.ui.platform.PlatformTab
import org.dolphinemu.dolphinemu.ui.theme.DolphinTheme.scaffoldPadding
import org.dolphinemu.dolphinemu.ui.theme.PreviewTheme
import org.dolphinemu.dolphinemu.utils.CoilUtils

private const val ScrimEnabled = true
private val ScrimFadeHeight = 0.dp
private const val ScrimMinContentAlpha = 0.00f
// Portion of the scrim, from its top, that the fade spans. Below that content is fully faded.
private const val ScrimFadeFraction = 0.6f

// How strongly content is faded, from none at the top of the scrim to full at the bottom. Many
// stops along an easing curve, so the fade has no visible edge where it starts and ends.
private val ScrimFadeStops = Array(16) { i ->
    val t = i / 15f
    t to Color.Black.copy(
        alpha = lerp(
            0f,
            1f - ScrimMinContentAlpha,
            FastOutSlowInEasing.transform(t)
        )
    )
}

@Composable
fun MainScreen(
    gameFiles: Map<PlatformTab, List<GameFile>>,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val tabs = PlatformTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dolphin",
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Default,
                        fontSize = 24.sp,
                        letterSpacing = 0.10.em,
                    )
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_dolphin_tertiary),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(64.dp)
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            DolphinNavBar(
                tabs = tabs.map { tab ->
                    DolphinNavTab(
                        label = stringResource(tab.headerName),
                        icon = {
                            Icon(
                                painter = painterResource(
                                    when (tab) {
                                        PlatformTab.GAMECUBE -> R.drawable.ic_gamecube
                                        PlatformTab.WII -> R.drawable.ic_wii
                                        PlatformTab.WIIWARE -> R.drawable.ic_folder
                                    }
                                ),
                                contentDescription = null,
                            )
                        },
                    )
                },
                selectedTabPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                onTabClick = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                actions = listOf(
                    DolphinNavAction(
                        label = stringResource(R.string.more_options),
                        icon = Icons.Filled.MoreVert,
                        onClick = { moreMenuExpanded = !moreMenuExpanded },
                        active = moreMenuExpanded,
                        popup = {
                            MoreMenu(
                                expanded = moreMenuExpanded,
                                onDismissRequest = { moreMenuExpanded = false },
                            )
                        },
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
                    .wrapContentWidth()
            )
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .then(
                    if (ScrimEnabled) {
                        Modifier.navBarScrim(bottomPadding = innerPadding.calculateBottomPadding())
                    } else {
                        Modifier
                    }
                )
        ) { page ->
            GameList(
                gameFiles = gameFiles[tabs[page]].orEmpty(),
                onGameSelected = {},
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Fades the content behind the nav bar with a full-width vertical fade. */
private fun Modifier.navBarScrim(bottomPadding: Dp): Modifier = this
    // Offscreen so DstOut fades only this layout's own pixels, not what's behind it.
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val scrimHeight = (bottomPadding + ScrimFadeHeight).toPx()
        val scrimTop = size.height - scrimHeight
        drawRect(
            brush = Brush.verticalGradient(
                *ScrimFadeStops,
                startY = scrimTop,
                endY = scrimTop + scrimHeight * ScrimFadeFraction,
            ),
            topLeft = Offset(0f, scrimTop),
            size = Size(size.width, scrimHeight),
            blendMode = BlendMode.DstOut,
        )
    }

private class MoreMenuItem(val label: Int, val icon: @Composable () -> Painter)

private val MoreMenuItems = listOf(
    MoreMenuItem(R.string.grid_menu_about) { rememberVectorPainter(Icons.Filled.Info) },
    MoreMenuItem(R.string.more_menu_add_games) { rememberVectorPainter(Icons.Filled.Add) },
    MoreMenuItem(R.string.more_menu_view_options) { rememberVectorPainter(Icons.Outlined.GridView) },
    MoreMenuItem(R.string.more_menu_wii_system) { painterResource(R.drawable.ic_wii) },
    MoreMenuItem(R.string.grid_menu_netplay) { rememberVectorPainter(Icons.Filled.People) },
    MoreMenuItem(R.string.grid_menu_settings) { rememberVectorPainter(Icons.Filled.Settings) },
)

/** Popup menu that opens upward from the nav bar's more action. Items aren't hooked up yet. */
@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        // Negative so the upward-opening menu sits 12dp above the button.
        offset = DpOffset(0.dp, (-12).dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.widthIn(min = 200.dp),
    ) {
        MoreMenuItems.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(item.label),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = item.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onDismissRequest,
                contentPadding = PaddingValues(horizontal = 18.dp),
            )
        }
    }
}

@Composable
private fun GameList(
    gameFiles: List<GameFile>,
    onGameSelected: (GameFile) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = contentPadding + PaddingValues(horizontal = scaffoldPadding) + PaddingValues(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
    ) {
        items(gameFiles, key = { it.getPath() }) { gameFile ->
            GameGridItem(
                gameFile = gameFile,
                onClick = { onGameSelected(gameFile) },
            )
        }
    }
}

@Composable
private fun GameGridItem(
    gameFile: GameFile,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(gameFile)
                    .error(R.drawable.no_banner)
                    .build(),
                contentDescription = gameFile.getTitle(),
                contentScale = ContentScale.Crop,
                imageLoader = CoilUtils.imageLoader,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
            )
//            Text(
//                text = gameFile.getTitle(),
//                style = MaterialTheme.typography.bodySmall,
//                maxLines = 2,
//                minLines = 2,
//                overflow = TextOverflow.Ellipsis,
//                modifier = Modifier
//                    .padding(8.dp)
//            )
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    PreviewTheme(darkTheme = false) {
        PreviewMainScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MainScreenDarkPreview() {
    PreviewTheme(darkTheme = true) {
        PreviewMainScreen()
    }
}

@Composable
private fun PreviewMainScreen() {
    MainScreen(
        gameFiles = emptyMap()
    )
}
