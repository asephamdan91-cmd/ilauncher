package com.example.ioslauncher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LabelStyle = TextStyle(
    color = Color.White,
    fontSize = 11.sp,
    textAlign = TextAlign.Center,
    shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 1f), 3f),
)

@Composable
fun IconImage(app: AppInfo, modifier: Modifier = Modifier) {
    Image(
        bitmap = app.icon,
        contentDescription = app.label,
        modifier = modifier.shadow(4.dp, SquircleShape),
    )
}

@Composable
fun IconLabel(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = LabelStyle,
    )
}

/** Ikon folder: kotak kaca squircle dengan grid mini 3x3 isi aplikasinya. */
@Composable
fun FolderIcon(apps: List<AppInfo>, modifier: Modifier = Modifier) {
    Box(
        modifier
            .shadow(4.dp, SquircleShape)
            .glass(SquircleShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            apps.take(9).chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { a ->
                        Image(
                            bitmap = a.icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).shadow(0.dp, SquircleShape),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockIcon(app: AppInfo, onClick: (Rect?) -> Unit, onLongClick: () -> Unit) {
    val rect = remember { arrayOfNulls<Rect>(1) }
    IconImage(
        app,
        Modifier
            .size(60.dp)
            .onGloballyPositioned { rect[0] = it.boundsInRoot() }
            .combinedClickable(onClick = { onClick(rect[0]) }, onLongClick = onLongClick),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderApp(app: AppInfo, onLaunch: (AppInfo, Rect?) -> Unit, onEject: (AppInfo) -> Unit) {
    val rect = remember { arrayOfNulls<Rect>(1) }
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onLaunch(app, rect[0]) },
                onLongClick = { onEject(app) },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconImage(app, Modifier.size(60.dp).onGloballyPositioned { rect[0] = it.boundsInRoot() })
        IconLabel(app.label, Modifier.padding(top = 5.dp))
    }
}

@Composable
fun FolderOverlay(
    folder: FolderItem?,
    visible: Boolean,
    onClose: () -> Unit,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onRename: (String) -> Unit,
    onEject: (AppInfo) -> Unit,
) {
    AnimatedVisibility(
        visible = visible && folder != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {
        if (folder != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .noRippleClickable(onClose)
                    .systemBarsPadding()
                    .imePadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .animateEnterExit(
                            enter = scaleIn(initialScale = 0.7f) + fadeIn(),
                            exit = scaleOut(targetScale = 0.7f) + fadeOut(),
                        )
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(32.dp))
                        .noRippleClickable { }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    var text by remember(folder.id) { mutableStateOf(folder.name) }
                    BasicTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            onRename(it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(folder.apps, key = { it.key }) { app ->
                            FolderApp(app, onLaunch, onEject)
                        }
                    }
                    BasicText(
                        "Tahan lama ikon untuk mengeluarkannya dari folder",
                        modifier = Modifier.padding(top = 14.dp),
                        style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SpotlightOverlay(
    visible: Boolean,
    apps: List<AppInfo>,
    onClose: () -> Unit,
    onLaunch: (AppInfo, Rect?) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(120)),
    ) {
        var query by remember { mutableStateOf("") }
        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        val close: () -> Unit = {
            keyboard?.hide()
            onClose()
        }

        LaunchedEffect(Unit) {
            query = ""
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }

        val results = remember(query, apps) {
            if (query.isBlank()) emptyList()
            else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }.take(8)
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .noRippleClickable(close)
                .systemBarsPadding()
                .imePadding(),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText("🔍", style = TextStyle(fontSize = 16.sp))
                    Box(Modifier.padding(start = 10.dp).weight(1f)) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isEmpty()) {
                                        BasicText(
                                            "Cari aplikasi",
                                            style = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 17.sp),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }

                if (query.isNotBlank()) {
                    Column(
                        Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .glass(RoundedCornerShape(20.dp))
                            .noRippleClickable { }
                            .padding(vertical = 6.dp),
                    ) {
                        if (results.isEmpty()) {
                            BasicText(
                                "Tidak ada hasil",
                                modifier = Modifier.padding(16.dp),
                                style = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp),
                            )
                        }
                        results.forEach { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboard?.hide()
                                        onLaunch(app, null)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconImage(app, Modifier.size(44.dp))
                                BasicText(
                                    app.label,
                                    modifier = Modifier.padding(start = 14.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(color = Color.White, fontSize = 17.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
