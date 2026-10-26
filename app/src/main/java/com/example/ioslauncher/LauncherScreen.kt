package com.example.ioslauncher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val COLUMNS = 4
private const val FIRST_PAGE = 16   // halaman pertama memuat widget jam
private const val PER_PAGE = 24

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
    state: HomeState,
    onLaunch: (AppInfo, android.graphics.Rect?) -> Unit,
    onUninstall: (AppInfo) -> Unit,
) {
    val items = remember(state.entries, state.apps) { state.items() }
    val pages = remember(items) {
        listOf(items.take(FIRST_PAGE)) + items.drop(FIRST_PAGE).chunked(PER_PAGE)
    }
    val pagerState = rememberPagerState { pages.size }

    var editMode by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var folderVisible by remember { mutableStateOf(false) }
    var spotlightVisible by remember { mutableStateOf(false) }

    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var hoverKey by remember { mutableStateOf<String?>(null) }
    val bounds = remember { HashMap<String, Rect>() }

    val openFolder = items.firstOrNull { it.id == openFolderId } as? FolderItem
    val lastFolder = remember { arrayOfNulls<FolderItem>(1) }
    if (openFolder != null) lastFolder[0] = openFolder
    val shownFolder = openFolder ?: lastFolder[0]

    LaunchedEffect(openFolder) {
        if (openFolder == null) folderVisible = false
    }
    LaunchedEffect(editMode) {
        if (!editMode) {
            dragKey = null
            hoverKey = null
        }
    }
    LaunchedEffect(state.homeSignal) {
        if (state.homeSignal > 0) {
            editMode = false
            folderVisible = false
            spotlightVisible = false
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler(enabled = editMode) { editMode = false }
    BackHandler(enabled = folderVisible) { folderVisible = false }
    BackHandler(enabled = spotlightVisible) { spotlightVisible = false }

    val launch: (AppInfo, Rect?) -> Unit = { app, r -> onLaunch(app, r?.toAndroidRect()) }

    // Efek goyang
    val transition = rememberInfiniteTransition(label = "jiggle")
    val angle by transition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse),
        label = "angle",
    )

    // Pindah halaman otomatis kalau ikon diseret ke tepi layar
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    LaunchedEffect(dragKey) {
        if (dragKey == null) return@LaunchedEffect
        val edge = with(density) { 40.dp.toPx() }
        while (dragKey != null) {
            val x = dragPos.x
            if (x < edge && pagerState.currentPage > 0) {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                delay(800)
            } else if (x > screenWidthPx - edge && pagerState.currentPage < pages.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                delay(800)
            }
            delay(100)
        }
    }

    val overlayOpen = folderVisible || spotlightVisible
    val blurRadius by animateDpAsState(if (overlayOpen) 18.dp else 0.dp, label = "blur")

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
        ) {
            // Baris atas: tombol "Selesai" saat edit
            Box(
                Modifier.fillMaxWidth().height(40.dp).padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (editMode) {
                    BasicText(
                        "Selesai",
                        modifier = Modifier.clickable { editMode = false },
                        style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Tarik ke bawah -> Spotlight
                    .pointerInput(editMode) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total > 140f && !editMode && dragKey == null) spotlightVisible = true
                            },
                            onDragCancel = { },
                            onVerticalDrag = { _, amount -> total += amount },
                        )
                    },
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = if (dragKey != null) 8 else 0,
                ) { p ->
                    val pageItems = pages.getOrElse(p) { emptyList() }
                    Box(
                        Modifier.graphicsLayer {
                            val off = ((pagerState.currentPage - p) + pagerState.currentPageOffsetFraction)
                                .absoluteValue.coerceIn(0f, 1f)
                            val s = 1f - 0.15f * off
                            scaleX = s
                            scaleY = s
                            alpha = 1f - 0.6f * off
                        },
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(COLUMNS),
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = false,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            if (p == 0) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "clock") {
                                    ClockWidget(Modifier.padding(bottom = 4.dp))
                                }
                            }
                            items(pageItems, key = { it.id }) { item ->
                                HomeCell(
                                    item = item,
                                    editMode = editMode,
                                    angle = angle,
                                    faded = dragKey == item.id,
                                    hover = hoverKey == item.id,
                                    onBounds = { id, r ->
                                        if (r == null) bounds.remove(id) else bounds[id] = r
                                    },
                                    onClick = { r ->
                                        if (editMode) {
                                            editMode = false
                                        } else {
                                            when (item) {
                                                is AppItem -> launch(item.app, r)
                                                is FolderItem -> {
                                                    openFolderId = item.id
                                                    folderVisible = true
                                                }
                                            }
                                        }
                                    },
                                    onLongClick = { editMode = true },
                                    onRemove = {
                                        when (item) {
                                            is AppItem -> onUninstall(item.app)
                                            is FolderItem -> state.dissolveFolder(item.id)
                                        }
                                    },
                                    onDragStart = { key, pos ->
                                        dragKey = key
                                        dragPos = pos
                                        hoverKey = null
                                    },
                                    onDrag = { delta ->
                                        dragPos += delta
                                        val k = dragKey
                                        hoverKey = bounds.entries
                                            .firstOrNull { it.key != k && it.value.contains(dragPos) }
                                            ?.key
                                    },
                                    onDragEnd = {
                                        val k = dragKey
                                        val t = hoverKey
                                        if (k != null && t != null) state.dropOnto(k, t)
                                        dragKey = null
                                        hoverKey = null
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Indikator halaman
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
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
                            ),
                    )
                }
            }

            // Tombol cari ala iOS
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BasicText(
                    "🔍  Cari",
                    modifier = Modifier
                        .glass(RoundedCornerShape(50))
                        .clickable { spotlightVisible = true }
                        .padding(horizontal = 22.dp, vertical = 7.dp),
                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                )
            }

            // Dock kaca
            if (state.dock.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .glass(RoundedCornerShape(32.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    state.dock.forEach { app ->
                        DockIcon(
                            app = app,
                            onClick = { r -> if (editMode) { editMode = false } else { launch(app, r) } },
                            onLongClick = { editMode = true },
                        )
                    }
                }
            }
        }

        // Ikon yang sedang diseret
        val dragApp = dragKey?.let { k -> state.apps[k] }
        if (dragApp != null) {
            Image(
                bitmap = dragApp.icon,
                contentDescription = null,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragPos.x - 30.dp.toPx()).roundToInt(),
                            (dragPos.y - 30.dp.toPx()).roundToInt(),
                        )
                    }
                    .size(60.dp)
                    .graphicsLayer {
                        scaleX = 1.15f
                        scaleY = 1.15f
                    }
                    .shadow(12.dp, SquircleShape),
            )
        }

        FolderOverlay(
            folder = shownFolder,
            visible = folderVisible,
            onClose = { folderVisible = false },
            onLaunch = { app, r ->
                folderVisible = false
                launch(app, r)
            },
            onRename = { name -> openFolderId?.let { state.renameFolder(it, name) } },
            onEject = { app -> openFolderId?.let { state.removeFromFolder(it, app.key) } },
        )

        SpotlightOverlay(
            visible = spotlightVisible,
            apps = state.all,
            onClose = { spotlightVisible = false },
            onLaunch = { app, r ->
                spotlightVisible = false
                launch(app, r)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeCell(
    item: HomeItem,
    editMode: Boolean,
    angle: Float,
    faded: Boolean,
    hover: Boolean,
    onBounds: (String, Rect?) -> Unit,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val iconRect = remember { arrayOfNulls<Rect>(1) }
    val colPos = remember { arrayOf(Offset.Zero) }
    DisposableEffect(item.id) { onDispose { onBounds(item.id, null) } }
    val scale by animateFloatAsState(if (hover) 1.18f else 1f, label = "hover")

    val dragMod = if (editMode && item is AppItem) {
        Modifier.pointerInput(item.id) {
            detectDragGestures(
                onDragStart = { off -> onDragStart(item.id, colPos[0] + off) },
                onDrag = { change, amount ->
                    change.consume()
                    onDrag(amount)
                },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragEnd() },
            )
        }
    } else {
        Modifier
    }

    Column(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { colPos[0] = it.positionInRoot() }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (faded) 0.3f else 1f
            }
            .rotate(if (editMode) angle else 0f)
            .then(dragMod)
            .combinedClickable(onClick = { onClick(iconRect[0]) }, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .onGloballyPositioned {
                    val r = it.boundsInRoot()
                    iconRect[0] = r
                    onBounds(item.id, r)
                },
        ) {
            when (item) {
                is AppItem -> IconImage(item.app, Modifier.fillMaxSize())
                is FolderItem -> FolderIcon(item.apps, Modifier.fillMaxSize())
            }
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
                    BasicText(
                        "−",
                        style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
        IconLabel(
            when (item) {
                is AppItem -> item.app.label
                is FolderItem -> item.name
            },
            Modifier.padding(top = 5.dp),
        )
    }
}
