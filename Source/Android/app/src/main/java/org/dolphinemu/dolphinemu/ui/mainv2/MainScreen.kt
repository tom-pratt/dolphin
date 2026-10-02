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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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

private const val ScrimEnabled = false
private val ScrimFadeHeight = 0.dp
private const val ScrimMinContentAlpha = 0.05f

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

// The same profile by distance from a bottom corner of the bar, for the rounded ends.
private val ScrimEndFadeStops =
    ScrimFadeStops.reversedArray().map { (t, color) -> (1f - t) to color }.toTypedArray()

@Composable
fun MainScreen(
    gameFiles: Map<PlatformTab, List<GameFile>>,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val tabs = PlatformTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var navBarBounds by remember { mutableStateOf(Rect.Zero) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
                        painter = painterResource(R.drawable.ic_dolphin),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(64.dp)
                            .scale(scaleX = -1f, scaleY = 1f)
                    )
                },
                actions = {
                    Image(
                        painter = painterResource(R.drawable.ic_dolphin),
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
                        icon = { PlatformTabIcon(tab) },
                    )
                },
                selectedTabPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                onTabClick = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                actions = listOf(
                    DolphinNavAction(
                        label = stringResource(R.string.more_options),
                        icon = Icons.Filled.MoreVert,
                        onClick = {},
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
                    .wrapContentWidth()
                    .onGloballyPositioned { navBarBounds = it.boundsInRoot() }
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
                        Modifier.navBarScrim(
                            navBarBounds = { navBarBounds },
                            bottomPadding = innerPadding.calculateBottomPadding(),
                        )
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

/**
 * Fades the content behind the nav bar: a vertical fade across the bar's width, with a radial
 * fade around each bottom corner so it rounds off a little beyond the bar's ends.
 */
private fun Modifier.navBarScrim(navBarBounds: () -> Rect, bottomPadding: Dp): Modifier = this
    // Offscreen so DstOut fades only this layout's own pixels, not what's behind it.
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val bar = navBarBounds()
        if (bar.isEmpty) return@drawWithContent

        val scrimHeight = (bottomPadding + ScrimFadeHeight).toPx()
        val scrimTop = size.height - scrimHeight
        drawRect(
            brush = Brush.verticalGradient(
                *ScrimFadeStops,
                startY = scrimTop,
                endY = size.height,
            ),
            topLeft = Offset(bar.left, scrimTop),
            size = Size(bar.width, scrimHeight),
            blendMode = BlendMode.DstOut,
        )
        for ((cornerX, endLeft) in listOf(
            bar.left to bar.left - scrimHeight,
            bar.right to bar.right
        )) {
            drawRect(
                brush = Brush.radialGradient(
                    *ScrimEndFadeStops,
                    center = Offset(cornerX, size.height),
                    radius = scrimHeight,
                ),
                topLeft = Offset(endLeft, scrimTop),
                size = Size(scrimHeight, scrimHeight),
                blendMode = BlendMode.DstOut,
            )
        }
    }

@Composable
private fun PlatformTabIcon(tab: PlatformTab) {
    val icon = when (tab) {
        PlatformTab.GAMECUBE -> R.drawable.ic_gamecube
        PlatformTab.WII -> R.drawable.ic_wii
        PlatformTab.WIIWARE -> R.drawable.ic_folder
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
    )
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
        contentPadding = contentPadding + PaddingValues(horizontal = scaffoldPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            Text(
                text = gameFile.getTitle(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(8.dp)
            )
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
