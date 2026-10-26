package com.example.ioslauncher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val COLUMNS = 4
private const val ROWS = 6
private const val PER_PAGE = COLUMNS * ROWS

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
    home: HomeData,
    onLaunch: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
) {
    var editMode by remember { mutableStateOf(false) }
    BackHandler(enabled = editMode) { editMode = false }

    val pages = remember(home.grid) { home.grid.chunked(PER_PAGE).ifEmpty { listOf(emptyList()) } }
    val pagerState = rememberPagerState { pages.size }

    // Animasi goyang (jiggle) untuk mode edit
    val transition = rememberInfiniteTransition(label = "jiggle")
    val angle by transition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse),
        label = "angle",
    )

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        // Baris atas: tombol "Selesai" saat edit
        Box(Modifier.fillMaxWidth().height(40.dp).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
            if (editMode) {
                BasicText(
                    "Selesai",
                    modifier = Modifier.clickable { editMode = false },
                    style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { p ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMNS),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(pages[p], key = { it.component.flattenToString() }) { app ->
                    AppIcon(
                        app = app,
                        modifier = Modifier.fillMaxWidth(),
                        editMode = editMode,
                        angle = angle,
                        onClick = { if (editMode) editMode = false else onLaunch(app) },
                        onLongClick = { editMode = true },
                        onRemove = { onUninstall(app) },
                    )
                }
            }
        }

        // Indikator halaman
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(7.dp)
                        .background(
                            if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.4f),
                            CircleShape,
                        )
                )
            }
        }

        // Dock
        if (home.dock.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(32.dp))
                    .padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    home.dock.forEach { app ->
                        AppIcon(
                            app = app,
                            modifier = Modifier.width(60.dp),
                            showLabel = false,
                            editMode = false,
                            angle = 0f,
                            onClick = { onLaunch(app) },
                            onLongClick = { editMode = true },
                            onRemove = {},
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIcon(
    app: AppInfo,
    modifier: Modifier,
    editMode: Boolean,
    angle: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    showLabel: Boolean = true,
) {
    Column(
        modifier
            .rotate(if (editMode) angle else 0f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(60.dp)) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize().shadow(4.dp, SquircleShape),
            )
            if (editMode) {
                Box(
                    Modifier
                        .size(22.dp)
                        .align(Alignment.TopStart)
                        .offset((-6).dp, (-6).dp)
                        .background(Color(0xFF8E8E93), CircleShape)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText("−", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
        if (showLabel) {
            BasicText(
                app.label,
                modifier = Modifier.padding(top = 5.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 1f), 3f),
                ),
            )
        }
    }
}
