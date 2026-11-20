package com.example.flowclone

import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import kotlinx.coroutines.delay

/** 全屏半透明遮罩弹层 */
@Composable
fun Overlay(
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    val scrim = remember { MutableInteractionSource() }
    val inner = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable(scrim, null) { onDismiss() },
        contentAlignment = alignment,
    ) {
        Box(Modifier.clickable(inner, null) {}) { content() }
    }
}

@Composable
private fun MenuRow(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (danger) Color(0xFFFF8A80) else TextPrimary, fontSize = 16.sp)
    }
}

@Composable
private fun MenuDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
}

@Composable
fun ProjectMenuSheet(
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onGrid: () -> Unit,
    gridLabel: String = "网格：大",
    onTrash: () -> Unit,
    onDelete: () -> Unit,
) {
    AnchoredMenu(onDismiss) {
        MenuRow("重命名") { onRename(); onDismiss() }
        MenuDivider()
        MenuRow(gridLabel) { onGrid(); onDismiss() }
        MenuDivider()
        MenuRow("查看回收站") { onTrash(); onDismiss() }
        MenuDivider()
        MenuRow("删除项目", danger = true) { onDelete(); onDismiss() }
    }
}

@Composable
fun AssetMenuSheet(
    isVideo: Boolean,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
) {
    AnchoredMenu(onDismiss) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuIconButton(Icons.Outlined.ThumbUp, "我喜欢") { onAction("已标记喜欢"); onDismiss() }
            MenuIconButton(Icons.Outlined.ThumbDown, "不喜欢") { onAction("已标记不喜欢"); onDismiss() }
        }
        listOf("重命名", "重复使用提示", "收藏", "下载", "发送", "分享").forEach {
            MenuDivider()
            MenuRow(it) { onAction(it); onDismiss() }
        }
        if (isVideo) {
            MenuDivider()
            MenuRow("分享到 YouTube") { onAction("分享到 YouTube"); onDismiss() }
        }
        MenuDivider()
        MenuRow("报告") { onAction("已提交报告"); onDismiss() }
    }
}

@Composable
private fun RowScope.MenuIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { onClick() }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color(0xFFE8EAED), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color(0xFFE8EAED), fontSize = 14.sp)
    }
}

@Composable
fun UploadSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    Overlay(onDismiss, alignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)
                .clip(RoundedCornerShape(22.dp)).background(Surface),
        ) {
            listOf("拍摄", "图片", "视频").forEachIndexed { i, label ->
                if (i > 0) MenuDivider()
                MenuRow(label) { onDismiss(); onPick(label) }
            }
        }
    }
}

@Composable
fun RightsDialog(onCancel: () -> Unit, onAgree: () -> Unit) {
    Overlay(onCancel) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp).clip(RoundedCornerShape(28.dp))
                .background(Surface).padding(20.dp),
        ) {
            Text("通知", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(
                "请确保您对上传的所有内容或文件（包括涉及未成年人的内容）均拥有必要权利。请勿生成任何侵权内容，包括具有欺骗性、骚扰性或危害性的内容。使用 Flow 时，须遵守 Google 的《使用限制政策》。",
                color = Color(0xFFE8EAED), fontSize = 14.sp, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedPill("取消", Modifier.weight(1f), onCancel)
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(50)).background(Color.White)
                        .clickable { onAgree() },
                    contentAlignment = Alignment.Center,
                ) { Text("我同意", color = Color.Black, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
fun OutlinedPill(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(50)).border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = 15.sp) }
}

@Composable
fun ShareSheet(onDismiss: () -> Unit, prompt: String) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.Close, onDismiss)
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("包含输入内容", color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                SimpleSwitch(true) {}
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).padding(16.dp)) {
                Text(prompt, color = Color(0xFFE8EAED), fontSize = 14.sp, lineHeight = 22.sp)
            }
            OutlinedPill("复制链接", Modifier.fillMaxWidth()) { onDismiss() }
            Text("任何知道该链接的人都可以查看和使用您的创作内容", color = TextSecondary, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ExtendScreen(asset: FlowAsset, onBack: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(true) }
    val extendSec = 8   // 与原版一致：每次延长固定 8 秒
    val cost = if (asset.quality == "720p") 3 * extendSec else extendSec
    // 真实胶片条：从视频抽 10 帧
    val frames = remember(asset.localPath) {
        val path = asset.localPath
        if (path == null) emptyList<Bitmap>() else try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            (0 until 10).mapNotNull { i ->
                retriever.getFrameAtTime((durMs * i / 10) * 1000)
            }.also { retriever.release() }
        } catch (e: Exception) { emptyList() }
    }
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            RoundIconButton(Icons.Outlined.KeyboardArrowDown, onBack)
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            val previewBmp = frames.lastOrNull()
            if (previewBmp != null) {
                androidx.compose.foundation.Image(
                    previewBmp.asImageBitmap(), null,
                    Modifier.fillMaxWidth(0.66f).clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else Image2(asset, Modifier.fillMaxWidth(0.66f).clip(RoundedCornerShape(20.dp)))
        }
        // 胶片条
        androidx.compose.foundation.lazy.LazyRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(if (frames.isNotEmpty()) frames.size else 10) { idx ->
                val selected = idx == frames.size - 1 && frames.isNotEmpty()
                Box(
                    Modifier.size(54.dp, 40.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (selected) Color(0x33FFFFFF) else Color.Transparent)
                        .padding(if (selected) 2.dp else 0.dp),
                ) {
                    if (frames.isNotEmpty() && idx < frames.size) {
                        androidx.compose.foundation.Image(
                            frames[idx].asImageBitmap(), null,
                            Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painterResource(R.drawable.video_teapot), null,
                            Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).background(Surface2)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = text, onValueChange = { text = it },
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text("后续步骤", color = TextSecondary, fontSize = 16.sp)
                        inner()
                    }
                },
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Outlined.Add, {}, bg = Color.Transparent, size = 40)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(50)).border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
                        .clickable { settingsOpen = !settingsOpen }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("延长 $extendSec 秒", color = Color(0xFFE8EAED), fontSize = 14.sp) }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onSend(text.trim()) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Send, null, tint = Color.Black, modifier = Modifier.size(20.dp)) }
            }
        }
        if (settingsOpen) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)
                    .clip(RoundedCornerShape(24.dp)).background(Surface2).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("本次生成将消耗 $cost 个 AI 点数", color = TextPrimary, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(9) { i ->
                            Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (i <= 5) Color.White else Color(0x33FFFFFF)))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("$extendSec 秒", color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun SearchScreen(
    project: FlowProject?,
    onBack: () -> Unit,
    onOpenAsset: (String) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val results = project?.assets?.filter { a ->
        q.isBlank() || a.title.contains(q, true) || a.prompt.contains(q, true)
    } ?: emptyList()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        try { focus.requestFocus(); keyboard?.show() } catch (e: Exception) {}
    }
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.Close, onBack)
            Spacer(Modifier.width(12.dp))
            Row(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(50))
                    .background(Surface2).border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = q, onValueChange = { q = it },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    decorationBox = { inner ->
                        Box {
                            if (q.isEmpty()) Text("输入查询内容，即可在此项目中搜索。", color = TextSecondary, fontSize = 16.sp)
                            inner()
                        }
                    },
                )
            }
        }
        if (q.isBlank()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("输入查询内容，即可在此项目中搜索。", color = TextSecondary, fontSize = 16.sp)
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("没有找到匹配的内容", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results.size) { i ->
                    val a = results[i]
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                            .background(Surface3).clickable { onOpenAsset(a.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (a.versions.size > 1) {
                            // 原版搜索结果为多帧胶片条卡片
                            Row(Modifier.fillMaxSize()) {
                                a.versions.take(3).forEach { path ->
                                    Box(Modifier.weight(1f).fillMaxHeight()) {
                                        LocalAssetImage(path, Modifier.fillMaxSize())
                                    }
                                }
                            }
                        } else {
                            val lp = a.localPath
                            if (lp != null) LocalAssetImage(lp, Modifier.fillMaxSize())
                            else Image2(a, Modifier.fillMaxSize())
                        }
                        if (a.type == AssetType.VIDEO) {
                            Box(
                                Modifier.align(Alignment.TopStart).padding(8.dp).size(26.dp)
                                    .clip(CircleShape).background(Color(0x99000000)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCollectionSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.Close, onDismiss)
            Spacer(Modifier.width(12.dp))
            Text("创建集合", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f))
            Text("提交", color = if (title.isBlank()) TextSecondary else Color.White, fontSize = 16.sp,
                modifier = Modifier.clickable(enabled = title.isNotBlank()) { onSubmit(title.trim()) })
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp)) {
            Text("标题", color = TextSecondary, fontSize = 13.sp)
            BasicTextField(
                value = title, onValueChange = { title = it },
                textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text("给集合起个名字", color = TextSecondary, fontSize = 17.sp)
                        inner()
                    }
                },
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)).padding(top = 8.dp))
            Spacer(Modifier.height(12.dp))
            Text("已选择 $selectedCount 项", color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun AvatarScreen(onClose: () -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Box(Modifier.size(44.dp).clickable { onClose() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, "关闭", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            // 原版主视觉：头像 + 视频拼接预览
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(
                    painterResource(R.drawable.tpl3), null,
                    Modifier.width(210.dp).height(250.dp).clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                )
                // 小头像卡片 + My Voice
                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 0.dp)
                        .width(120.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2C2D30))
                        .padding(6.dp),
                ) {
                    androidx.compose.foundation.Image(
                        painterResource(R.drawable.tpl3), null,
                        Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Row(Modifier.padding(top = 6.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF1A73E8)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("My Voice", color = Color(0xFFE8EAED), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("您的面孔，您的声音，您的故事。", color = TextPrimary, fontSize = 30.sp, lineHeight = 40.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text("创建虚拟形象（实验性功能），让其在各类视频中领衔主演，将您的叙事创作水平提升至全新境界。",
                color = Color(0xFFE8EAED), fontSize = 16.sp, lineHeight = 26.sp)
            Spacer(Modifier.height(14.dp))
            Text("在下一步中，您需要录制一段简短的自拍视频并说几句话，以便生成您的拟真肖像。只需片刻即可完成。",
                color = Color(0xFFE8EAED), fontSize = 16.sp, lineHeight = 26.sp)
            Spacer(Modifier.height(14.dp))
            Text("免责声明：虚拟形象捕获服务仅支持英语，可能需要尝试几次。", color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(20.dp))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Box(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(50)).background(Color.White)
                    .clickable { onStart() },
                contentAlignment = Alignment.Center,
            ) { Text("开始使用", color = Color.Black, fontSize = 16.sp) }
        }
    }
}

@Composable
fun AnchoredMenu(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val scrim = remember { MutableInteractionSource() }
    val inner = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxSize().background(Color(0x33000000)).clickable(scrim, null) { onDismiss() }) {
        Column(
            Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 8.dp).width(300.dp)
                .clip(RoundedCornerShape(24.dp)).background(Surface)
                .clickable(inner, null) {},
            content = content,
        )
    }
}

@Composable
fun ColumnScope.AgentChat(messages: List<Pair<String, String>>, typing: Boolean, onRetry: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        messages.forEach { (role, text) ->
            if (role == "user") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF2F3034))
                        .padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(text, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            } else if (text == "__ERROR__") {
                Column(Modifier.fillMaxWidth()) {
                    Text("出了点问题", color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onRetry() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Refresh, null, tint = TextPrimary, modifier = Modifier.size(20.dp)) }
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    Text(text, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChatFeedback(Icons.Outlined.ThumbUp)
                        ChatFeedback(Icons.Outlined.ThumbDown)
                        ChatFeedback(Icons.Outlined.ContentCopy)
                        ChatFeedback(Icons.Outlined.Flag)
                    }
                }
            }
        }
        if (typing) Text("正在思考…", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChatFeedback(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).clickable {}, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun FlowToast(text: String?) {
    if (text == null) return
    Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE8EAED)).padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text(text, color = Color(0xFF111111), fontSize = 14.sp)
        }
    }
}

@Composable
fun Image2(a: FlowAsset, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painterResource(if (a.type == AssetType.IMAGE) R.drawable.teapot else R.drawable.video_teapot),
        contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit,
    )
}

@Composable
fun AgentHistoryScreen(
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onOpenConversation: (String) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
            Text("会话", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).padding(top = 12.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)
                .clickable { onOpenConversation("New Creative Visual Project") }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("New Creative Visual Project", color = TextPrimary, fontSize = 16.sp)
                    Text("2026年11月18日", color = TextSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Outlined.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50)).background(Surface)
                .clickable { onNewChat() }, contentAlignment = Alignment.Center) {
                Text("创建新会话", color = TextPrimary, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AgentSettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
            Text("智能体设置", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("请求许可", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)) {
                SettingsRow("始终", selected = Repo.agentAlways) {
                    Repo.agentAlways = true
                    Repo.persist()
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                SettingsRow("永不", selected = !Repo.agentAlways) {
                    Repo.agentAlways = false
                    Repo.persist()
                }
            }
            Text("默认模型", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).padding(16.dp)) {
                Text("您的首选模型。您仍然可以随时要求智能体使用其他模型。", color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("🖼 Grok Imagine Image 1.5", color = TextPrimary, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("默认图片模型", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("🎬 Grok Imagine Video 1.5", color = TextPrimary, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("默认视频模型", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Text("默认宽高比", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).padding(16.dp)) {
                Text("除非您要求使用其他宽高比，否则系统会应用此宽高比。", color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("横向 16:9", color = TextPrimary, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("默认宽高比", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsRow(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (selected) TextPrimary else TextSecondary, fontSize = 16.sp,
            modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Overlay(onCancel) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp).clip(RoundedCornerShape(28.dp)).background(Surface).padding(20.dp)) {
            Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFFE8EAED), fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedPill("取消", Modifier.weight(1f), onCancel)
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(50)).background(Color.White).clickable { onConfirm() },
                    contentAlignment = Alignment.Center,
                ) { Text(confirmLabel, color = Color.Black, fontSize = 15.sp) }
            }
        }
    }
}


@Composable
fun LocalAssetImage(path: String, modifier: Modifier = Modifier) {
    val bmp = remember(path) {
        BitmapFactory.decodeFile(path) ?: try {
            val r = MediaMetadataRetriever()
            r.setDataSource(path)
            val f = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            r.release()
            f
        } catch (e: Exception) { null }
    }
    if (bmp != null) {
        androidx.compose.foundation.Image(bmp.asImageBitmap(), null, modifier, contentScale = ContentScale.Fit)
    } else {
        Box(modifier.background(Surface3))
    }
}

@Composable
fun VideoAssetView(asset: FlowAsset, modifier: Modifier = Modifier, path: String? = null) {
    val context = LocalContext.current
    var muted by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    val exo = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = (path ?: asset.localPath)?.let { Uri.fromFile(File(it)) }
                ?: Uri.parse("android.resource://${context.packageName}/${R.raw.sample_video}")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(Unit) { onDispose { exo.release() } }
    LaunchedEffect(Unit) {
        while (true) {
            positionMs = exo.currentPosition.coerceAtLeast(0L)
            durationMs = exo.duration.coerceAtLeast(0L)
            playing = exo.isPlaying
            delay(250)
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { useController = false; player = exo } },
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable {
                    if (exo.isPlaying) exo.pause() else exo.play()
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null,
                    tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            // 进度条（可点按/拖动 seek）
            var barWidth by remember { mutableStateOf(1f) }
            val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            Box(
                Modifier.weight(1f).height(22.dp)
                    .onSizeChanged { barWidth = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(durationMs) {
                        detectTapGestures { offset ->
                            if (durationMs > 0) exo.seekTo((offset.x / size.width * durationMs).toLong())
                        }
                    }
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures { change, _ ->
                            if (durationMs > 0) {
                                exo.seekTo((change.position.x / size.width * durationMs).toLong())
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x59FFFFFF)))
                Box(Modifier.fillMaxWidth(fraction).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                val density = androidx.compose.ui.platform.LocalDensity.current
                Box(
                    Modifier.offset {
                        androidx.compose.ui.unit.IntOffset(
                            (barWidth * fraction - with(density) { 6.dp.toPx() }).toInt(), 0,
                        )
                    }.size(12.dp).clip(CircleShape).background(Color.White),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "${fmtDurationLabel((positionMs / 1000).toString())} / ${fmtDurationLabel((durationMs / 1000).toString())}",
                color = Color.White, fontSize = 12.sp,
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(36.dp).clickable {
                    muted = !muted
                    exo.volume = if (muted) 0f else 1f
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp, null,
                    tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

fun fmtDurationLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return "0:06"
    if (raw.contains(":")) return raw
    val sec = raw.toIntOrNull() ?: return "0:06"
    return "%d:%02d".format(sec / 60, sec % 60)
}


@Composable
fun EditImageScreen(
    asset: FlowAsset,
    mode: Mode = Mode.IMAGE,
    onClose: () -> Unit,
    onSubmit: (prompt: String, count: Int, aspect: String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(1) }
    var aspect by remember { mutableStateOf("9:16") }
    var settingsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            RoundIconButton(Icons.Outlined.KeyboardArrowDown, onClose)
        }
        // 预览区占剩余空间，键盘弹出时只压缩预览，不压缩底部操作区
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val lp = asset.localPath
            if (lp != null) LocalAssetImage(
                lp,
                Modifier.fillMaxWidth().fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)),
            )
            else Image2(asset, Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)))
        }
        // 底部操作区：固定高度，避免被键盘压扁
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(Surface2)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = text, onValueChange = { text = it },
                textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) onSubmit(text.trim(), count, aspect)
                }),
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp, max = 88.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text("您想要更改什么?", color = TextSecondary, fontSize = 17.sp)
                        inner()
                    }
                },
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp).height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Add, null, tint = TextPrimary, modifier = Modifier.size(24.dp)
                        .clickable {})
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(50))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
                        .clickable { settingsOpen = !settingsOpen }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text((if (mode == Mode.VIDEO) "视频" else "图片") + " $count 份", color = Color(0xFFE8EAED), fontSize = 13.sp)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(if (text.isNotBlank()) Color.White else Surface3)
                        .clickable(enabled = text.isNotBlank()) { onSubmit(text.trim(), count, aspect) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Send, null, tint = if (text.isNotBlank()) Color.Black else TextSecondary,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
        if (settingsOpen) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)
                    .clip(RoundedCornerShape(24.dp)).background(Surface2).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("本次生成将消耗 0 个 AI 点数", color = TextPrimary, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                SegRow(Repo.aspects, aspect) { aspect = it }
                SegRow(Repo.counts.map { "$it 份" }, "$count 份") { count = it.first().digitToInt() }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/* ---------------- 虚拟形象（数字人）流程 ---------------- */


@Composable
fun AvatarTipsScreen(onBack: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, "关闭", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(10.dp))
            Text("Tips for your selfie and voice recording", color = TextPrimary, fontSize = 30.sp, lineHeight = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text("You'll be asked to move your head in different directions and say a few words.",
                color = Color(0xFFE8EAED), fontSize = 17.sp, lineHeight = 27.sp)
            Spacer(Modifier.height(22.dp))
            listOf(
                Icons.Outlined.PhoneIphone to "Hold your phone at eye level",
                Icons.Outlined.LightMode to "Use lighting that isn't too dim or too bright",
                Icons.Outlined.Face to "Make sure your eyes, nose, and mouth are visible: glasses are fine to keep on, but no sunglasses, masks, or hats",
                Icons.Outlined.VolumeUp to "Choose a quiet area to record your voice, with no other voices in the background",
                Icons.Outlined.People to "Check that there are no people or images of faces in your background",
                Icons.Outlined.Person to "This process must be completed by the account owner. Do not allow minors to record themselves.",
            ).forEach { (icon, text) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.padding(top = 3.dp).size(26.dp))
                    Spacer(Modifier.width(18.dp))
                    Text(text, color = Color(0xFFE8EAED), fontSize = 17.sp, lineHeight = 26.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Back", color = Color(0xFF8AB4F8), fontSize = 17.sp,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 12.dp, horizontal = 8.dp))
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.height(52.dp).width(160.dp).clip(RoundedCornerShape(50)).background(Color(0xFFA8C7FA))
                    .clickable { onNext() },
                contentAlignment = Alignment.Center,
            ) { Text("Next", color = Color(0xFF0B1020), fontSize = 17.sp) }
        }
    }
}
@Composable
fun AvatarConsentScreen(onBack: () -> Unit, onAgree: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, "关闭", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(18.dp))
            Text("Take a selfie video to create an avatar", color = TextPrimary, fontSize = 30.sp, lineHeight = 40.sp)
            Spacer(Modifier.height(22.dp))
            Text("Record your face and voice to easily add yourself to your own content. We'll use this data to:",
                color = Color(0xFFE8EAED), fontSize = 17.sp, lineHeight = 27.sp)
            Spacer(Modifier.height(16.dp))
            Text("• Generate content with your avatar for you in Gemini App and other Google services",
                color = Color(0xFFE8EAED), fontSize = 17.sp, lineHeight = 27.sp)
            Spacer(Modifier.height(12.dp))
            Text("• Check that you're a real person, and keep you, our users, and the public safe",
                color = Color(0xFFE8EAED), fontSize = 17.sp, lineHeight = 27.sp)
            Spacer(Modifier.height(18.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = TextSecondary)) {
                        append("You're in control of your avatar and are responsible for content you create. You can delete your recording anytime. ")
                    }
                    withStyle(SpanStyle(color = Color(0xFF8AB4F8))) { append("Learn more about how we use your data.") }
                } as AnnotatedString,
                fontSize = 16.sp, lineHeight = 26.sp,
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(50))
                    .border(1.dp, Color(0xFF5F6368), RoundedCornerShape(50)).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("No thanks", color = Color(0xFF8AB4F8), fontSize = 16.sp) }
            Box(
                Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(50))
                    .border(1.dp, Color(0xFF5F6368), RoundedCornerShape(50)).clickable { onAgree() },
                contentAlignment = Alignment.Center,
            ) { Text("I agree", color = Color(0xFF8AB4F8), fontSize = 16.sp) }
        }
    }
}

@Composable
fun AvatarProcessingScreen(progress: Int, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            RoundIconButton(Icons.Outlined.Close, onCancel)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(Surface),
            contentAlignment = Alignment.Center,
        ) { Text("👤", fontSize = 52.sp) }
        Spacer(Modifier.height(24.dp))
        Text("正在创建您的虚拟形象…", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Text("$progress%", color = TextSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 48.dp).height(4.dp)
            .clip(RoundedCornerShape(2.dp)).background(Surface)) {
            Box(Modifier.fillMaxWidth(progress / 100f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
        }
        Spacer(Modifier.height(12.dp))
        Text("大约需要 1 分钟，请保持 App 在前台", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun AvatarReadyScreen(
    onClose: () -> Unit,
    onUse: () -> Unit,
    onRecreate: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.Close, onClose)
            Spacer(Modifier.width(12.dp))
            Text("您的虚拟形象", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            val img = Repo.avatarImagePath
            Box(
                Modifier.size(180.dp, 260.dp).clip(RoundedCornerShape(24.dp)).background(Surface),
                contentAlignment = Alignment.Center,
            ) {
                if (img != null) {
                    androidx.compose.foundation.Image(
                        painterResource(android.R.drawable.ic_menu_camera), null, Modifier.fillMaxSize(),
                    )
                }
                // 真实头像帧用 Bitmap 解码显示
                if (img != null) LocalAssetImage(img, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(18.dp))
            Text("虚拟形象已就绪", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text("可以在创建面板中使用您的虚拟形象生成视频", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50)).background(Color.White)
                    .clickable { onUse() },
                contentAlignment = Alignment.Center,
            ) { Text("用虚拟形象生视频", color = Color.Black, fontSize = 16.sp) }
            OutlinedPill("重新创建", Modifier.fillMaxWidth(), onRecreate)
        }
    }
}


@Composable
fun TrashScreen(
    project: FlowProject,
    onBack: () -> Unit,
    onRestoreAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var clearConfirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
            Text("回收站", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f))
            RoundIconButton(Icons.Outlined.MoreVert, { menuOpen = true })
        }
        if (project.trashed.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("此项目的回收站为空", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(project.trashed.size) { i ->
                    val a = project.trashed[i]
                    Box(Modifier.fillMaxWidth().aspectRatio(0.5625f).clip(RoundedCornerShape(12.dp)).background(Surface3)) {
                        val lp = a.localPath
                        if (lp != null) LocalAssetImage(lp, Modifier.fillMaxSize()) else Image2(a, Modifier.fillMaxSize())
                        Icon(Icons.Outlined.PlayArrow, null, tint = Color.Transparent, modifier = Modifier.size(1.dp))
                    }
                }
            }
        }
    }
    if (menuOpen) {
        Overlay({ menuOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp).clip(RoundedCornerShape(28.dp)).background(Surface)) {
                Box(Modifier.fillMaxWidth().clickable { menuOpen = false; onRestoreAll() }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center) { Text("全部恢复", color = TextPrimary, fontSize = 16.sp) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                Box(Modifier.fillMaxWidth().clickable { menuOpen = false; clearConfirm = true }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center) { Text("清空回收站", color = Color(0xFFFF8A80), fontSize = 16.sp) }
            }
        }
    }
    if (clearConfirm) {
        ConfirmDialog(
            title = "清空回收站",
            message = "确定要永久删除回收站中的全部内容吗？此操作不可撤销。",
            confirmLabel = "清空",
            onConfirm = { clearConfirm = false; onClearAll() },
            onCancel = { clearConfirm = false },
        )
    }
}

/* ---------------- 创建面板：添加媒体 / 资源库 ---------------- */

@Composable
fun AttachSheet(onDismiss: () -> Unit, onCamera: () -> Unit, onImage: () -> Unit, onLibrary: () -> Unit) {
    Overlay(onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(Surface)) {
            listOf(
                Triple(Icons.Outlined.PhotoCamera, "拍摄", onCamera),
                Triple(Icons.Outlined.Image, "图片", onImage),
                Triple(Icons.Outlined.PhotoLibrary, "资源库", onLibrary),
            ).forEachIndexed { i, (icon, label, act) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                Row(
                    Modifier.fillMaxWidth().clickable { onDismiss(); act() }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(label, color = TextPrimary, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ResourceLibraryScreen(
    projects: List<FlowProject>,
    onClose: () -> Unit,
    onPick: (FlowAsset) -> Unit,
    onUpload: () -> Unit,
) {
    var projId by remember { mutableStateOf(projects.firstOrNull()?.id ?: "") }
    var category by remember { mutableStateOf("全部") }
    var projMenu by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val project = projects.firstOrNull { it.id == projId } ?: projects.firstOrNull()
    val assets = (project?.assets ?: emptyList()).filter { a ->
        (category == "全部" || (category == "图片" && a.type == AssetType.IMAGE) || (category == "视频" && a.type == AssetType.VIDEO)) &&
            (query.isBlank() || a.title.contains(query, true))
    }
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable { onClose() }) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Bg).clickable(enabled = false) {},
        ) {
        Column(Modifier.fillMaxSize().padding(top = 10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(50)).background(Surface)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = query, onValueChange = { query = it },
                        textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) Text("搜索资源", color = TextSecondary, fontSize = 16.sp)
                                inner()
                            }
                        },
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.height(48.dp).clip(RoundedCornerShape(50)).background(Color.White)
                        .clickable { onUpload() }.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("上传", color = Color.Black, fontSize = 15.sp) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(50)).background(Surface)
                        .clickable { projMenu = !projMenu }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(project?.title ?: "项目", color = TextPrimary, fontSize = 14.sp)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(50)).background(Surface)
                        .clickable { catMenu = !catMenu }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(category + " ▾", color = TextPrimary, fontSize = 14.sp) }
            }
            if (assets.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("暂无资源", color = TextSecondary, fontSize = 16.sp)
                }
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(assets.size) { i ->
                        val a = assets[i]
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                                .background(Surface3).clickable { onPick(a) },
                            contentAlignment = Alignment.Center,
                        ) {
                            val lp = a.localPath
                            if (lp != null) LocalAssetImage(lp, Modifier.fillMaxSize()) else Image2(a, Modifier.fillMaxSize())
                            if (a.type == AssetType.VIDEO) {
                                Box(
                                    Modifier.align(Alignment.TopStart).padding(8.dp).size(26.dp)
                                        .clip(CircleShape).background(Color(0x99000000)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Outlined.Close, onClose)
                Spacer(Modifier.width(12.dp))
                Text("资源库", color = TextSecondary, fontSize = 14.sp)
            }
        }
        if (projMenu) {
            Overlay({ projMenu = false }) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp).clip(RoundedCornerShape(24.dp)).background(Surface)) {
                    projects.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { projId = p.id; projMenu = false }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        ) {
                            Text(p.title, color = if (p.id == projId) Color.White else TextSecondary, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        if (catMenu) {
            Overlay({ catMenu = false }) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp).clip(RoundedCornerShape(24.dp)).background(Surface)) {
                    listOf("全部", "图片", "视频").forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable { category = c; catMenu = false }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        ) { Text(c, color = if (c == category) Color.White else TextSecondary, fontSize = 16.sp) }
                    }
                }
            }
        }
        }
    }
}

/* ---------------- 登录 / 注册（自建 Server） ---------------- */

@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGuest: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onForgot: () -> Unit = {},
) {
    var tab by remember { mutableStateOf(0) } // 0 登录 1 注册
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLogin = tab == 0
    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().imePadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(96.dp))
        Text("Google Flow", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("复刻版 · 登录后开始创作", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(40.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Surface2).padding(4.dp),
        ) {
            listOf("登录", "注册").forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (tab == i) Color.White else Color.Transparent)
                        .clickable { tab = i },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (tab == i) Color.Black else TextSecondary,
                        fontSize = 15.sp, fontWeight = if (tab == i) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        @Composable
        fun field(value: String, onChange: (String) -> Unit, label: String, password: Boolean = false) {
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(Surface2)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (password) KeyboardType.Password else KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) Text(label, color = TextSecondary, fontSize = 16.sp)
                            inner()
                        }
                    },
                )
            }
        }
        field(email, { email = it }, "邮箱")
        Spacer(Modifier.height(12.dp))
        field(password, { password = it }, "密码（至少 8 位）", password = true)
        val emailValid = email.contains("@") && email.length >= 5
        val passwordValid = password.length >= 8
        val canSubmit = emailValid && passwordValid
        if (password.isNotEmpty() && !passwordValid) {
            Spacer(Modifier.height(8.dp))
            Text("密码至少 8 位", color = Color(0xFFFFB4A9), fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth())
        }
        if (email.isNotEmpty() && !emailValid) {
            Spacer(Modifier.height(8.dp))
            Text("请输入有效邮箱", color = Color(0xFFFFB4A9), fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth())
        }
        if (isLogin) {
            Spacer(Modifier.height(10.dp))
            Text("忘记密码？", color = Color(0xFF9AA0A6), fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().clickable { onForgot() })
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = Color(0xFFFF8A80), fontSize = 13.sp)
        }
        Spacer(Modifier.height(22.dp))
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50))
                .background(if (loading || !canSubmit) Surface3 else Color.White)
                .clickable(enabled = !loading && canSubmit) {
                    if (isLogin) onLogin(email.trim(), password) else onRegister(email.trim(), password)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (loading) "请稍候…" else if (isLogin) "登录" else "注册并登录",
                color = if (loading || !canSubmit) TextSecondary else Color.Black,
                fontSize = 16.sp, fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "游客模式（设备账号）",
            color = TextSecondary, fontSize = 14.sp,
            modifier = Modifier.clickable(enabled = !loading) { onGuest() }.padding(8.dp),
        )
        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("继续即表示同意", color = Color(0xFF6B6F76), fontSize = 11.sp)
            Text("服务条款", color = Color(0xFF9AA0A6), fontSize = 11.sp,
                modifier = Modifier.clickable { onOpenTerms() }.padding(horizontal = 3.dp))
            Text("与", color = Color(0xFF6B6F76), fontSize = 11.sp)
            Text("隐私政策", color = Color(0xFF9AA0A6), fontSize = 11.sp,
                modifier = Modifier.clickable { onOpenPrivacy() }.padding(horizontal = 3.dp))
        }
    }
}


@Composable
fun LegalScreen(title: String, body: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
            Text(title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(body, color = Color(0xFFE8EAED), fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(40.dp))
        }
    }
}
