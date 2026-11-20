package com.example.flowclone

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FlowTheme { FlowCloneApp() } }
    }
}

sealed interface Screen {
    data object Home : Screen
    data class ProjectScreen(val id: String) : Screen
    data class AssetScreen(val assetId: String) : Screen
    data class SetScreen(val setId: String) : Screen
    data object Notifications : Screen
    data object Account : Screen
}

private val TERMS_TEXT = """
FlowClone 服务条款（学习复刻版草稿）

1. 服务说明
本应用提供 AI 图像/视频生成功能，生成任务由第三方 AI 模型网关完成。本应用为个人学习复刻项目，不提供任何商业担保。

2. 账号
- 你需要注册账号（邮箱+密码）或使用游客模式；
- 请妥善保管密码，账号下的生成内容与额度由你负责。

3. 使用规范
- 禁止生成违法、暴力、色情、侵权、仇恨、虚假信息等内容；
- 禁止利用本应用从事欺诈、骚扰或其他违法活动；
- 上传的参考素材必须拥有合法使用权。

4. AI 生成内容
生成结果由 AI 产生，可能不准确或包含意外元素。你需对提示词与使用后果负责；我们有权对违规内容进行删除或限制账号。

5. 付费与额度
当前点数由服务端账本记录，示例规则：图片 1 点/次，视频 10 点/次。正式商业化时以应用内购买条款为准。

6. 免责
服务按现状提供，可能中断或调整；在法律允许范围内不承担间接损失。

7. 变更
条款更新将在应用内展示，继续使用视为接受更新。
"""

private val PRIVACY_TEXT = """
FlowClone 隐私政策（学习复刻版草稿）

我们收集：
- 邮箱与密码哈希（账号登录）
- 设备标识（游客模式）
- 提示词、参考图片/视频、生成结果（提供服务必需）
- 推送 token（如启用通知）

用途：
- 账号管理、内容生成、历史记录、生成完成通知、防滥用。

共享：
- 提示词与参考素材会发送至 AI 模型网关用于生成；密钥仅保存在服务端。
- 不出售你的个人信息。

存储与删除：
- 账号、任务、账本存储在服务端数据库；
- 可随时退出登录；可联系支持邮箱申请删除账号及全部数据。

未成年人：
- 本服务禁止未成年人使用。

联系方式：
- 上线前填写 support@example.com
"""

data class ComposerUi(
    val mode: Mode = Mode.AGENT,
    val text: String = "",
    val aspect: String = "9:16",
    val count: Int = 1,
    val res: String = "720p",
    val duration: Int = 8,
    val modelId: String = "grok-imagine-image-1.5",
    val settingsOpen: Boolean = false,
    val modelListOpen: Boolean = false,
    val refAsset: FlowAsset? = null,
    val attach: FlowAsset? = null,
    val animate: Boolean = false,
)

@Composable
fun FlowCloneApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var composer by remember { mutableStateOf<ComposerUi?>(null) }
    var filter by remember { mutableStateOf("all") }
    var chat by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var chatTyping by remember { mutableStateOf(false) }
    var chatJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var chatProjectId by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var rightsPend by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var gridSize by remember { mutableStateOf(2) }
    var selectedAssets by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCreateSet by remember { mutableStateOf(false) }
    var renameRequested by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(Mode.IMAGE) }
    var trashOpen by remember { mutableStateOf(false) }
    var moveConfirm by remember { mutableStateOf(false) }
    var attachOpen by remember { mutableStateOf(false) }
    var resourceOpen by remember { mutableStateOf(false) }
    var avatarOpen by remember { mutableStateOf(false) }
    var extendAsset by remember { mutableStateOf<FlowAsset?>(null) }
    var shareAsset by remember { mutableStateOf<FlowAsset?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var agentHistory by remember { mutableStateOf(false) }
    var agentSettings by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<FlowProject?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    AuthStore.init(context)
    val scope = rememberCoroutineScope()
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var serverJobs by remember { mutableStateOf<List<Gateway.JobInfo>>(emptyList()) }
    var serverNotifs by remember { mutableStateOf<List<Gateway.ServerNotification>>(emptyList()) }
    var legalKind by remember { mutableStateOf<String?>(null) }
    var deleteAccountConfirm by remember { mutableStateOf(false) }

    fun refreshServerJobs() {
        if (!AuthStore.isLoggedIn) return
        scope.launch {
            serverJobs = Gateway.jobs(20)
            serverNotifs = Gateway.notifications(20)
        }
    }

    fun afterAuth(r: Gateway.AuthResult) {
        AuthStore.save(r.token, r.refreshToken, r.email, r.userId, r.credits)
        Repo.credits = r.credits
        authError = null
        authLoading = false
        refreshServerJobs()
    }

    fun runAuth(block: suspend () -> Gateway.AuthResult) {
        authLoading = true
        authError = null
        scope.launch {
            try { afterAuth(block()) } catch (e: Exception) {
                authLoading = false
                authError = e.message?.take(120) ?: "登录失败"
            }
        }
    }

    var editAsset by remember { mutableStateOf<FlowAsset?>(null) }
    var avatarStage by remember { mutableStateOf(0) }
    var avatarProgress by remember { mutableStateOf(0) }
    val avatarFileRef = remember { mutableStateOf<File?>(null) }
    BackHandler(enabled = true) {
        when {
            deleteAccountConfirm -> deleteAccountConfirm = false
            legalKind != null -> legalKind = null
            composer != null -> composer = null
            searchOpen -> searchOpen = false
            trashOpen -> trashOpen = false
            showCreateSet -> showCreateSet = false
            moveConfirm -> moveConfirm = false
            menu != null -> menu = null
            sheet != null -> sheet = null
            editAsset != null -> editAsset = null
            extendAsset != null -> extendAsset = null
            shareAsset != null -> shareAsset = null
            avatarOpen -> avatarOpen = false
            avatarStage != 0 -> avatarStage = 0
            agentHistory -> agentHistory = false
            agentSettings -> agentSettings = false
            else -> when (val s = screen) {
                is Screen.AssetScreen -> {
                    val pid = Repo.projectOf(s.assetId)?.id
                    screen = if (pid != null) Screen.ProjectScreen(pid) else Screen.Home
                }
                is Screen.SetScreen -> screen = Repo.findSet(s.setId)?.let { Screen.ProjectScreen(it.projectId) } ?: Screen.Home
                is Screen.ProjectScreen -> screen = Screen.Home
                else -> (context as? android.app.Activity)?.finish()
            }
        }
    }

    var pendingPick by remember { mutableStateOf<String?>(null) }

    fun importPicked(uri: Uri) {
        val kind = rightsPend ?: pendingPick
        val isVideo = kind == "capture-video" || kind == "video-edit" || kind == "upload-video"
        try {
            val f = File(context.filesDir, "picked_${System.currentTimeMillis()}." + if (isVideo) "mp4" else "jpg")
            context.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
            val proj = Repo.findProject((screen as? Screen.ProjectScreen)?.id)
                ?: Repo.projects.firstOrNull() ?: Repo.createProject()
            Repo.addLocalAsset(proj, if (isVideo) AssetType.VIDEO else AssetType.IMAGE, f.absolutePath, f.nameWithoutExtension)
            screen = Screen.ProjectScreen(proj.id)
            toast = if (isVideo) "已导入视频素材" else "已导入图片素材"
        } catch (e: Exception) {
            toast = "导入失败：${e.message}"
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importPicked(uri)
    }
    val animatePickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val f = File(context.filesDir, "animate_ref_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { out -> input.copyTo(out) }
                }
                val ref = FlowAsset(
                    id = "animate_ref", type = AssetType.IMAGE, title = "照片", model = "本地素材",
                    res = "原始", prompt = "photo animation", status0 = GenStatus.COMPLETED,
                    progress0 = 100f, localPath = f.absolutePath,
                )
                composer = ComposerUi(mode = Mode.VIDEO, attach = ref, animate = true)
            } catch (e: Exception) {
                toast = "读取图片失败"
            }
        }
    }
    val pickVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importPicked(uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uri = cameraUri
        if (res.resultCode == Activity.RESULT_OK && uri != null) importPicked(uri) else toast = "已取消拍摄"
    }

    fun addGenerationNotification(project: FlowProject?, kind: AssetType, title: String) {
        // 原版实测：只有视频生成/编辑/延长会发通知；图片（0 点）不发
        if (kind != AssetType.VIDEO) return
        val kindText = "视频"
        val projectLine = project?.title?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: ""
        Repo.notifications.add(0, ("成功！ 您的${kindText}“${title}”已生成完毕。点按即可查看。" + projectLine) to "刚刚")
    }

    fun logGenError(where: String, e: Exception) {
        android.util.Log.e("FlowClone", "生成失败[$where]: ${e.message}", e)
    }

    fun friendlyError(e: Exception): String {
        val raw = e.message ?: return "生成失败"
        return when {
            raw.contains("402") || raw.contains("insufficient") -> "点数不足：当前 ${Repo.credits} 点，请充值或联系管理员"
            raw.contains("401") -> "登录已过期，请重新登录"
            raw.contains("403") -> "没有权限执行该操作"
            raw.contains("429") -> "操作太频繁，请稍后再试"
            raw.contains("HTTP 5") -> "服务暂时不可用，请稍后再试"
            else -> raw.take(80)
        }
    }

    fun enoughCredits(cost: Int): Boolean {
        if (cost > Repo.credits) {
            toast = "点数不足：本次需要 ${cost} 点，当前 ${Repo.credits} 点"
            return false
        }
        return true
    }

    fun refreshCredits() {
        scope.launch {
            val c = Gateway.meCredits()
            if (c >= 0 && AuthStore.isLoggedIn) { AuthStore.updateCredits(c); Repo.credits = c }
        }
    }

    fun applyGenerationOutputs(group: List<FlowAsset>, files: List<File>, appendTo: FlowAsset?, notifTitle: String? = null) {
        if (appendTo != null) {
            files.forEach { appendTo.versions.add(it.absolutePath) }
            if (files.isNotEmpty()) {
                appendTo.localPath = files.last().absolutePath
                appendTo.status = GenStatus.COMPLETED
                appendTo.progress = 100f
            } else appendTo.status = GenStatus.FAILED
            appendTo.jobId = null
            if (files.isNotEmpty()) {
                addGenerationNotification(Repo.projectOf(appendTo.id), appendTo.type,
                    notifTitle ?: appendTo.title)
            }
            if (appendTo.type == AssetType.VIDEO && files.isNotEmpty()) {
                toast = "已保存为新版本（共 ${appendTo.versions.size} 个版本，原视频保留）"
            }
        } else {
            group.forEachIndexed { i, a ->
                if (i < files.size) {
                    a.localPath = files[i].absolutePath
                    a.status = GenStatus.COMPLETED
                    a.progress = 100f
                    a.versions.add(files[i].absolutePath)
                } else a.status = GenStatus.FAILED
                a.jobId = null
            }
        }
        Repo.persist()
        refreshCredits()
        refreshServerJobs()
        group.firstOrNull()?.let { first ->
            if (first.status == GenStatus.COMPLETED) {
                addGenerationNotification(Repo.projectOf(first.id), first.type,
                    notifTitle ?: first.prompt.ifBlank { first.title })
            }
        }
    }

    fun launchGeneration(
        project: FlowProject,
        assets: List<FlowAsset>,
        refAsset: FlowAsset?,
        appendTo: FlowAsset? = null,
        promptOverride: String? = null,
        aspectOverride: String? = null,
        countOverride: Int? = null,
        durationOverride: Int? = null,
    ) {
        if (assets.isEmpty()) return
        scope.launch {
            val dir = File(context.filesDir, "generated")
            val refPath: String? = when {
                refAsset?.type == AssetType.VIDEO && refAsset?.localPath != null -> {
                    try {
                        val f = File(context.filesDir, "ref_frame_${System.currentTimeMillis()}.jpg")
                        val r = MediaMetadataRetriever()
                        r.setDataSource(refAsset!!.localPath)
                        val bmp = r.getFrameAtTime(500_000)
                        r.release()
                        if (bmp != null) {
                            f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
                            f.absolutePath
                        } else null
                    } catch (e: Exception) { null }
                }
                refAsset?.localPath != null -> refAsset!!.localPath
                refAsset != null -> {
                    val f = File(context.filesDir, "ref_default.jpg")
                    if (!f.exists()) {
                        BitmapFactory.decodeResource(context.resources, R.drawable.teapot)
                            ?.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, f.outputStream())
                    }
                    f.absolutePath
                }
                else -> null
            }
            try {
                val a0 = assets.first()
                val isVideo = a0.type == AssetType.VIDEO
                val jobId = if (isVideo) {
                    Gateway.submitVideo(
                        prompt = promptOverride ?: a0.prompt,
                        aspectRatio = aspectOverride ?: a0.aspect,
                        durationSeconds = durationOverride ?: (a0.duration?.toIntOrNull() ?: 8),
                        quality = if (a0.quality == "720p") "720p" else "480p",
                        referencePath = refPath,
                    )
                } else {
                    Gateway.submitImage(
                        prompt = promptOverride ?: a0.prompt,
                        size = aspectOverride ?: a0.aspect,
                        count = countOverride ?: assets.size,
                        referencePath = refPath,
                    )
                }
                assets.forEach { it.jobId = jobId }
                (appendTo ?: a0).jobId = jobId
                Repo.persist()
                val files = Gateway.waitAndDownload(jobId, isVideo, dir) { p -> assets.forEach { it.progress = p } }
                applyGenerationOutputs(assets, files, appendTo, notifTitle = promptOverride)
            } catch (e: Exception) {
                // 保留 jobId：下次启动/回前台自动续查（对齐原版自动恢复），手动重试也走续查
                assets.forEach { it.status = GenStatus.FAILED; it.lastError = e.toString().take(300) }
                logGenError("launchGeneration", e)
                toast = "生成失败：${friendlyError(e)}"
                Repo.persist()
            }
        }
    }

    /** 真正的「延长」：生成续写片段后与原视频拼接成更长视频，作为新版本 */
    fun launchExtend(project: FlowProject, asset: FlowAsset, followUp: String) {
        scope.launch {
            val dir = File(context.filesDir, "generated")
            val prevPath = asset.localPath
            val prevFile = prevPath?.let { File(it) }
            try {
                // 参考图：取原视频最后一帧
                val refPath: String? = try {
                    if (prevFile != null && prevFile.exists()) {
                        val r = MediaMetadataRetriever()
                        r.setDataSource(prevFile.absolutePath)
                        val durMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        val bmp = r.getFrameAtTime((durMs - 200).coerceAtLeast(0) * 1000)
                        r.release()
                        if (bmp != null) {
                            val f = File(context.filesDir, "ext_ref_${System.currentTimeMillis()}.jpg")
                            f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
                            f.absolutePath
                        } else null
                    } else null
                } catch (e: Exception) { null }

                asset.extendBasePath = prevPath
                val jobId = Gateway.submitVideo(
                    prompt = followUp.ifBlank { "Extend the video" },
                    aspectRatio = asset.aspect,
                    durationSeconds = 8,
                    quality = if (asset.quality == "720p") "720p" else "480p",
                    referencePath = refPath,
                )
                asset.jobId = jobId
                Repo.persist()
                val files = Gateway.waitAndDownload(jobId, isVideo = true, outDir = dir) { p ->
                    asset.progress = p
                }
                val ext = files.firstOrNull()
                if (ext == null) throw RuntimeException("没有返回延长片段")
                if (prevFile != null && prevFile.exists()) {
                    val out = File(dir, "ext_${System.currentTimeMillis()}.mp4")
                    // 关键修复：不同次生成的视频分辨率可能不同，必须统一缩放后重新编码，
                    // 直接 MediaMuxer 拼接会导致第 8 秒之后无法解码（花屏/黑屏）
                    val ok = VideoCompose.concat(context, listOf(prevFile, ext), out)
                    if (ok) {
                        asset.versions.add(out.absolutePath)
                        asset.localPath = out.absolutePath
                        val total = Mp4Concat.durationSeconds(out)
                        asset.duration = total.toString()
                        asset.status = GenStatus.COMPLETED
                        asset.progress = 100f
                        asset.jobId = null
                        asset.extendBasePath = null
                        ext.delete()
                        Repo.persist()
                        toast = "已延长为 ${total} 秒（共 ${asset.versions.size} 个版本）"
                        addGenerationNotification(project, AssetType.VIDEO,
                            followUp.ifBlank { "延长片段" })
                        Repo.persist()
                        refreshCredits()
                        refreshServerJobs()
                        return@launch
                    }
                }
                // 拼接失败则退化为追加片段
                asset.versions.add(ext.absolutePath)
                asset.localPath = ext.absolutePath
                asset.duration = Mp4Concat.durationSeconds(ext).toString()
                asset.status = GenStatus.COMPLETED
                asset.progress = 100f
                asset.jobId = null
                asset.extendBasePath = null
                Repo.persist()
                toast = "已生成延长片段（拼接失败，已作为新版本保存）"
                addGenerationNotification(project, AssetType.VIDEO, followUp.ifBlank { "延长片段" })
                Repo.persist()
                refreshServerJobs()
            } catch (e: Exception) {
                asset.status = if (asset.localPath != null) GenStatus.COMPLETED else GenStatus.FAILED
                asset.jobId = null
                asset.extendBasePath = null
                toast = "延长失败：${friendlyError(e)}"
                Repo.persist()
            }
        }
    }

    // 应用启动/回到前台时，恢复所有带 jobId 的进行中任务（进程被杀也不丢）
    fun resumeAsset(a: FlowAsset) {
        val jobId = a.jobId ?: return
        val proj = Repo.projectOf(a.id) ?: return
        a.status = GenStatus.GENERATING
        a.progress = 0f
        Repo.persist()
        scope.launch {
            val dir = File(context.filesDir, "generated")
            try {
                val files = Gateway.waitAndDownload(jobId, a.type == AssetType.VIDEO, dir) { p -> a.progress = p }
                val append = if (a.type == AssetType.VIDEO && a.versions.isNotEmpty()) a else null
                applyGenerationOutputs(listOf(a), files, append)
            } catch (e: Exception) {
                a.status = GenStatus.FAILED
                a.jobId = null
                a.lastError = e.toString().take(300)
                logGenError("resumeAsset", e)
                toast = "恢复失败：${e.message?.take(80)}"
                Repo.persist()
            }
        }
    }

    fun resumePendingJobs() {
        Repo.pendingJobGroups().forEach { (jobId, group) ->
            scope.launch {
                val dir = File(context.filesDir, "generated")
                try {
                    val isVideo = group.first().type == AssetType.VIDEO
                    val files = Gateway.waitAndDownload(jobId, isVideo, dir) { p -> group.forEach { it.progress = p } }
                    val target = group.firstOrNull()
                    val basePath = target?.extendBasePath
                    if (target != null && isVideo && basePath != null && files.isNotEmpty()) {
                        val base = File(basePath)
                        val out = File(dir, "ext_${System.currentTimeMillis()}.mp4")
                        val ok = base.exists() && VideoCompose.concat(context, listOf(base, files[0]), out)
                        if (ok) {
                            target.versions.add(out.absolutePath)
                            target.localPath = out.absolutePath
                            target.duration = Mp4Concat.durationSeconds(out).toString()
                            target.status = GenStatus.COMPLETED
                            target.progress = 100f
                            target.jobId = null
                            target.extendBasePath = null
                            files[0].delete()
                            Repo.persist()
                            refreshCredits()
                            refreshServerJobs()
                            toast = "已恢复并完成延长（${Mp4Concat.durationSeconds(out)} 秒）"
                            return@launch
                        }
                    }
                    val appendTarget = group.firstOrNull { it.versions.isNotEmpty() && it.status == GenStatus.GENERATING && group.size == 1 }
                    applyGenerationOutputs(group, files, appendTarget.takeIf { it != null && it.type == AssetType.VIDEO })
                } catch (e: Exception) {
                    group.forEach { it.status = GenStatus.FAILED; it.lastError = e.toString().take(300) }
                    logGenError("resumePendingJobs", e)
                    toast = "生成失败：${e.message?.take(80)}"
                    Repo.persist()
                }
            }
        }
    }

    if (!AuthStore.isLoggedIn) {
        Box(Modifier.fillMaxSize()) {
        LoginScreen(
            loading = authLoading,
            error = authError,
            onLogin = { e, p -> runAuth { Gateway.login(e, p) } },
            onRegister = { e, p -> runAuth { Gateway.register(e, p) } },
            onGuest = { runAuth { Gateway.deviceLogin(android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "guest-device") } },
            onOpenTerms = { legalKind = "terms" },
            onOpenPrivacy = { legalKind = "privacy" },
            onForgot = { authError = "请联系 support@example.com 重置密码（待接入邮件服务）" },
        )
        if (deleteAccountConfirm) {
            ConfirmDialog(
                title = "注销账号",
                message = "注销后账号与全部历史数据不可恢复，确定继续吗？",
                confirmLabel = "注销",
                onConfirm = {
                    deleteAccountConfirm = false
                    scope.launch {
                        val ok = Gateway.deleteAccount()
                        if (ok) {
                            AuthStore.logout()
                            Repo.credits = 0
                            toast = "账号已注销"
                        } else toast = "注销失败，请稍后重试"
                    }
                },
                onCancel = { deleteAccountConfirm = false },
            )
        }
        if (legalKind != null) {
            LegalScreen(
                title = if (legalKind == "terms") "服务条款" else "隐私政策",
                body = if (legalKind == "terms") TERMS_TEXT else PRIVACY_TEXT,
                onBack = { legalKind = null },
            )
        }
        }
        return
    }

    LaunchedEffect(Unit) {
        refreshServerJobs()
        refreshCredits()
    }
    LaunchedEffect(Unit) { resumePendingJobs() }

    val avatarVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val f = avatarFileRef.value
        if (res.resultCode == Activity.RESULT_OK && f != null && f.exists()) {
            avatarProgress = 0
            avatarStage = 2
            scope.launch {
                for (p in 5..100 step 5) {
                    avatarProgress = p
                    delay(130)
                }
                try {
                    val jpg = File(context.filesDir, "avatar_image.jpg")
                    val r = MediaMetadataRetriever()
                    r.setDataSource(f.absolutePath)
                    val bmp = r.getFrameAtTime(1_000_000)
                    r.release()
                    if (bmp != null) {
                        jpg.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
                        Repo.avatarImagePath = jpg.absolutePath
                    }
                    Repo.avatarVideoPath = f.absolutePath
                    Repo.persist()
                } catch (e: Exception) {
                    toast = "头像生成失败：${e.message?.take(50)}"
                }
                avatarStage = 3
            }
        } else {
            avatarStage = 1
        }
    }

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) { true }
    }

    fun feeRulesText(): String {
        val rule360 = (3..10).joinToString("\n") { "• ${it}秒：${it} 点数" } + "\n• 15秒：20 点数"
        val rule720 = (3..10).joinToString("\n") { "• ${it}秒：${3 * it} 点数" } + "\n• 15秒：60 点数"
        return "了解生成费用\n" +
            "以下是 Flow 中的生成费用与点数（Credits）规则：\n" +
            "1. 图像生成费用\n" +
            "• 所有图像模型（🍌 Nano Banana 2.1、Nano Banana Pro、Nano Banana 2 Lite）：免费（0 点数）。\n" +
            "• 2K 图像超采样/放大：免费（0 点数）。\n" +
            "2. 视频生成费用（Omni 1.2 Flash）\n" +
            "视频生成费用取决于视频时长与输出分辨率：\n" +
            "• 360p 标准画质：\n$rule360\n" +
            "• 720p 高清画质：\n$rule720\n" +
            "3. 当前账户点数状态\n" +
            "• 当前剩余点数：${Repo.credits} 点数\n" +
            "• 刷新时间：2026-12-06 15:29 UTC"
    }

    suspend fun doAgentReply() {
        chatTyping = true
        val lastUser = chat.lastOrNull { it.first == "user" }?.second ?: ""
        val isFeeQuestion = lastUser.contains("费用") || lastUser.contains("点数") || lastUser.contains("Credits", true)
        delay(if (isFeeQuestion) 1500 else 800)
        val reply: String = try {
            if (isFeeQuestion) feeRulesText() else {
                val history = chat.filter { it.second != "__ERROR__" }
                    .map { (role, text) -> (if (role == "user") "user" else "assistant") to text }
                    .takeLast(12)
                val r = Gateway.agentChat(history)
                if (r.isBlank()) throw RuntimeException("空回复")
                r
            }
        } catch (e: Exception) {
            // 兜底：智能体不可用时仍给出可读回复
            if (isFeeQuestion) feeRulesText()
            else "编辑图片\n请提供或选择您想要编辑的图片，并告诉我具体的修改需求（例如：更改背景、调整色彩、添加或删除特定元素等）。"
        }
        chatTyping = false
        chat = chat + ("agent" to reply)
        chatProjectId?.let { pid ->
            Repo.findProject(pid)?.let { p -> p.agentPending = true; Repo.persist() }
        }
    }

    fun startAvatarCapture() {
        try {
            val f = File(context.filesDir, "avatar_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
            avatarFileRef.value = f
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 15)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            avatarVideoLauncher.launch(intent)
        } catch (e: Exception) {
            toast = "无法打开相机：${e.message?.take(40)}"
            avatarStage = 1
        }
    }

    fun startMedia(kind: String) {
        rightsPend = kind
        when (kind) {
            "capture-photo", "capture-video" -> {
                val isVideo = kind == "capture-video"
                val f = File(context.filesDir, "camera_${System.currentTimeMillis()}." + if (isVideo) "mp4" else "jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                cameraUri = uri
                val intent = if (isVideo) Intent(MediaStore.ACTION_VIDEO_CAPTURE) else Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                cameraLauncher.launch(intent)
            }
            "video-edit", "upload-video" -> pickVideoLauncher.launch("video/*")
            else -> pickImageLauncher.launch("image/*")
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) { delay(1800); toast = null }
    }


    Box(Modifier.fillMaxSize().background(Bg)) {
        when (val s = screen) {
            is Screen.Home -> HomeScreen(
                onOpenProject = { screen = Screen.ProjectScreen(it) },
                onNewProject = {
                    val p = Repo.createProject()
                    screen = Screen.ProjectScreen(p.id)
                },
                onTemplate = { kind ->
                    if (kind == "avatar") { if (Repo.avatarImagePath != null) avatarStage = 3 else avatarOpen = true }
                    else if (kind == "animate-photo") { animatePickLauncher.launch("image/*") }
                    else { rightsPend = kind; sheet = "rights" }
                },
                onNotifications = { screen = Screen.Notifications },
                onAccount = { screen = Screen.Account },
            )

            is Screen.ProjectScreen -> {
                val project = Repo.findProject(s.id)
                if (project == null) {
                    screen = Screen.Home
                } else {
                    ProjectScreen(
                        project = project,
                        filter = filter,
                        onFilter = { filter = it },
                        onBack = { screen = Screen.Home },
                        onOpenAsset = { screen = Screen.AssetScreen(it) },
                        onCreate = { composer = ComposerUi(mode = Mode.AGENT) },
                        onMenu = { menu = "project" },
                        onSearch = { searchOpen = true },
                        onUpload = { sheet = "upload" },
                        onRetry = { id ->
                            val a = Repo.findAsset(id)
                            val p = Repo.projectOf(id)
                            if (a != null && p != null) {
                                if (!a.jobId.isNullOrBlank()) resumeAsset(a)
                                else { Repo.retry(a); launchGeneration(p, listOf(a), null) }
                            }
                        },
                        gridSize = gridSize,
                        selected = selectedAssets,
                        onToggleSelect = { id ->
                            selectedAssets = if (selectedAssets.contains(id)) selectedAssets - id else selectedAssets + id
                        },
                        onClearSelection = { selectedAssets = emptySet() },
                        onGridCycle = { gridSize = if (gridSize >= 4) 2 else gridSize + 1 },
                        onAddToSet = { showCreateSet = true },
                        onTrash = { moveConfirm = true },
                        onRename = { newTitle ->
                            Repo.findProject((screen as? Screen.ProjectScreen)?.id)?.let {
                                it.title = newTitle
                                Repo.persist()
                            }
                        },
                        renameRequest = renameRequested,
                        onRenameConsumed = { renameRequested = false },
                        sets = Repo.sets.filter { it.projectId == project.id && (filter == "all" || filter == "sets") },
                        onOpenSet = { id -> screen = Screen.SetScreen(id) },
                        agentPending = project.agentPending,
                        onResumeAgent = { composer = ComposerUi(mode = Mode.AGENT) },
                    )
                }
            }

            is Screen.SetScreen -> {
                val st = Repo.findSet(s.setId)
                if (st == null) screen = Screen.Home else
                SetDetailScreen(
                    set = st,
                    onBack = { screen = Screen.ProjectScreen(st.projectId) },
                    onOpenAsset = { id -> screen = Screen.AssetScreen(id) },
                    onRename = { newName -> st.name = newName; Repo.persist() },
                    onDelete = { Repo.sets.remove(st); Repo.persist(); toast = "集合已删除"; screen = Screen.ProjectScreen(st.projectId) },
                )
            }
            is Screen.AssetScreen -> {
                val asset = Repo.findAsset(s.assetId)
                if (asset == null) {
                    screen = Screen.Home
                } else {
                    AssetScreen(
                        asset = asset,
                        onBack = {
                            val p = Repo.projectOf(asset.id)
                            screen = if (p != null) Screen.ProjectScreen(p.id) else Screen.Home
                        },
                        onMenu = { menu = "asset" },
                        onExtend = { extendAsset = asset },
                        onAnimate = { composer = ComposerUi(mode = Mode.VIDEO, refAsset = asset, animate = true) },
                        onEdit = { editMode = if (asset.type == AssetType.VIDEO) Mode.VIDEO else Mode.IMAGE; editAsset = asset },
                        onCancel = {
                            val jobId = asset.jobId
                            scope.launch {
                                val ok = if (jobId != null) Gateway.cancelJob(jobId) else false
                                if (ok) {
                                    asset.status = GenStatus.FAILED
                                    asset.jobId = null
                                    toast = "已取消生成"
                                    Repo.persist()
                                    refreshCredits()
                                } else {
                                    toast = "取消失败：任务可能已完成或不可取消"
                                    refreshCredits()
                                }
                            }
                        },
                        onRetry = {
                            val proj = Repo.projectOf(asset.id)
                            if (proj != null) {
                                if (!asset.jobId.isNullOrBlank()) resumeAsset(asset)
                                else { Repo.retry(asset); launchGeneration(proj, listOf(asset), null) }
                            }
                        },
                        onDuplicatePrompt = {
                            menu = null
                            composer = ComposerUi(
                                mode = if (asset.type == AssetType.VIDEO) Mode.VIDEO else Mode.IMAGE,
                                text = asset.prompt,
                            )
                        },
                    )
                }
            }

            Screen.Notifications -> NotificationsScreen(
                serverJobs = serverJobs,
                serverNotifs = serverNotifs,
                onRefresh = { refreshServerJobs() },
                onBack = { screen = Screen.Home },
                onOpen = {
                val target = Repo.projects.firstNotNullOfOrNull { p ->
                    p.assets.firstOrNull { it.status == GenStatus.COMPLETED }
                }
                screen = if (target != null) Screen.AssetScreen(target.id) else Screen.Home
            })
            Screen.Account -> AccountScreen(
                onBack = { screen = Screen.Home },
                onLogout = { AuthStore.logout(); screen = Screen.Home },
                onOpenTerms = { legalKind = "terms" },
                onOpenPrivacy = { legalKind = "privacy" },
                onDeleteAccount = { deleteAccountConfirm = true },
                onOpenActivity = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://support.google.com/googleone?p=g1_ai_credit_menu")))
                    } catch (e: Exception) {
                        toast = "无法打开链接"
                    }
                },
            )
        }

        composer?.let { ui ->
            ComposerScreen(
                ui = ui,
                chat = chat,
                typing = chatTyping,
                onHistory = { agentHistory = true },
                onSettings = { agentSettings = true },
                onAttach = { attachOpen = true },
                onStop = { chatJob?.cancel(); chatJob = null; chatTyping = false },
                onRetryChat = {
                    chat = chat.filterNot { it.second == "__ERROR__" }
                    scope.launch {
                        val ok = isOnline() && Gateway.probe()
                        if (ok) doAgentReply() else chat = chat + ("agent" to "__ERROR__")
                    }
                },
                onChange = { composer = it },
                onClose = { composer = null },
                onSend = { sent ->
                    if (sent.mode == Mode.AGENT) {
                        (screen as? Screen.ProjectScreen)?.let { ps ->
                            chatProjectId = ps.id
                            Repo.findProject(ps.id)?.let { p -> p.agentPending = false; Repo.persist() }
                        }
                        if (sent.text.isNotBlank() || sent.attach != null) {
                            val attachNote = sent.attach?.let { a -> "\n[已附加：${a.title}]" } ?: ""
                            chat = chat + ("user" to (sent.text + attachNote))
                            composer = sent.copy(text = "")
                            chatJob = scope.launch {
                                val ok = isOnline() && Gateway.probe()
                                if (ok) doAgentReply() else chat = chat + ("agent" to "__ERROR__")
                            }
                        }
                    } else {
                        // 从 AssetScreen / 参考素材打开 composer 时，必须回到素材所属项目（此前错误地落到第一个项目）
                        val project = (screen as? Screen.ProjectScreen)?.let { Repo.findProject(it.id) }
                            ?: (screen as? Screen.AssetScreen)?.let { scr -> Repo.findAsset(scr.assetId)?.let { Repo.projectOf(it.id) } }
                            ?: sent.refAsset?.let { Repo.projectOf(it.id) }
                            ?: sent.attach?.let { Repo.projectOf(it.id) }
                            ?: Repo.projects.firstOrNull() ?: Repo.createProject()
                        val models = if (sent.mode == Mode.IMAGE) Repo.imageModels else Repo.videoModels
                        val model = models.firstOrNull { it.id == sent.modelId } ?: models.first()
                        val cost = if (sent.mode == Mode.VIDEO) Repo.videoCost(sent.res, sent.duration) * sent.count else 0
                        if (!enoughCredits(cost)) {
                            // 点数不足：不创建素材，留在 composer
                        } else {
                            val created = Repo.submit(project, sent.mode, sent.text.trim(), model, sent.aspect, sent.count, sent.res, sent.duration)
                            screen = Screen.ProjectScreen(project.id)
                            composer = null
                            launchGeneration(project, created, sent.attach ?: sent.refAsset)
                        }
                    }
                },
            )
        }

        if (searchOpen) {
            SearchScreen(
                project = Repo.findProject((screen as? Screen.ProjectScreen)?.id),
                onBack = { searchOpen = false },
                onOpenAsset = { id -> searchOpen = false; screen = Screen.AssetScreen(id) },
            )
        }
        if (avatarOpen) AvatarScreen(onClose = { avatarOpen = false }, onStart = {
            avatarOpen = false
            avatarStage = 1
        })
        extendAsset?.let { a ->
            ExtendScreen(asset = a, onBack = { extendAsset = null }, onSend = { followUp ->
                val proj = Repo.projectOf(a.id) ?: Repo.projects.firstOrNull()
                if (proj != null) {
                    a.status = GenStatus.GENERATING
                    a.progress = 0f
                    extendAsset = null
                    screen = Screen.ProjectScreen(proj.id)
                    launchExtend(proj, a, followUp)
                } else extendAsset = null
            })
        }
        shareAsset?.let { a ->
            ShareSheet(onDismiss = { shareAsset = null }, prompt = a.prompt)
        }
        if (menu == "project") {
            val project = Repo.findProject((screen as? Screen.ProjectScreen)?.id)
            ProjectMenuSheet(
                onDismiss = { menu = null },
                onRename = { menu = null; renameRequested = true },
                onGrid = { gridSize = if (gridSize >= 4) 2 else gridSize + 1; menu = null },
                gridLabel = when (gridSize) { 2 -> "网格：大"; 3 -> "网格：中"; else -> "网格：小" },
                onTrash = { menu = null; trashOpen = true },
                onDelete = { confirmDelete = project },
            )
        }
        if (menu == "asset") {
            AssetMenuSheet(
                isVideo = Repo.findAsset((screen as? Screen.AssetScreen)?.assetId)?.type == AssetType.VIDEO,
                onDismiss = { menu = null },
                onAction = { label ->
                    val a = Repo.findAsset((screen as? Screen.AssetScreen)?.assetId)
                    when {
                        label == "分享" && a != null -> shareAsset = a
                        label == "重复使用提示" && a != null -> {
                            menu = null
                            composer = ComposerUi(
                                mode = if (a.type == AssetType.VIDEO) Mode.VIDEO else Mode.IMAGE,
                                text = a.prompt,
                            )
                        }
                        label == "收藏" && a != null -> {
                            Repo.toggleFavorite(a)
                            toast = if (a.fav) "已加入收藏夹" else "已移出收藏夹"
                        }
                        else -> toast = label
                    }
                },
            )
        }
        if (sheet == "upload") {
            UploadSheet(onDismiss = { sheet = null }, onPick = { label ->
                rightsPend = when (label) {
                    "拍摄" -> "capture-photo"
                    "图片" -> "upload-image"
                    else -> "upload-video"
                }
                sheet = "rights"
            })
        }
        if (sheet == "rights") {
            RightsDialog(onCancel = { sheet = null; rightsPend = null }, onAgree = {
                val pend = rightsPend
                sheet = null
                if (pend == "avatar") { rightsPend = null; toast = "需要相机权限（假数据）" }
                else if (pend != null) startMedia(pend)
                else rightsPend = null
            })
        }
        if (agentHistory) {
            AgentHistoryScreen(
                onBack = { agentHistory = false },
                onNewChat = { agentHistory = false; chat = listOf(); composer = ComposerUi(mode = Mode.AGENT) },
                onOpenConversation = {
                    agentHistory = false
                    chat = listOf(
                        "user" to "编辑图片",
                        "agent" to "编辑图片\n请提供或选择您想要编辑的图片，并告诉我具体的修改需求（例如：更改背景、调整色彩、添加或删除特定元素等）。",
                    )
                    composer = ComposerUi(mode = Mode.AGENT)
                    toast = "已打开历史会话"
                },
            )
        }
        if (agentSettings) AgentSettingsScreen(onBack = { agentSettings = false })
        confirmDelete?.let { p ->
            ConfirmDialog(
                title = "删除项目",
                message = "删除“${p.title}”？此操作不可撤销。",
                confirmLabel = "删除",
                onConfirm = {
                    Repo.projects.remove(p)
                    Repo.persist()
                    confirmDelete = null
                    menu = null
                    screen = Screen.Home
                },
                onCancel = { confirmDelete = null },
            )
        }
        if (avatarStage == 1) {
            AvatarConsentScreen(onBack = { avatarStage = 0 }, onAgree = { avatarStage = 4 })
        }
        if (avatarStage == 4) {
            AvatarTipsScreen(onBack = { avatarStage = 1 }, onNext = { startAvatarCapture() })
        }
        if (avatarStage == 2) {
            AvatarProcessingScreen(progress = avatarProgress, onCancel = { avatarStage = 0 })
        }
        if (avatarStage == 3) {
            val avatarImage = Repo.avatarImagePath
            AvatarReadyScreen(
                onClose = { avatarStage = 0 },
                onUse = {
                    avatarStage = 0
                    val ref = FlowAsset(
                        id = "avatar_ref", type = AssetType.IMAGE, title = "我的虚拟形象",
                        model = "本地素材", res = "原始", prompt = "virtual avatar",
                        status0 = GenStatus.COMPLETED, progress0 = 100f, localPath = avatarImage,
                    )
                    composer = ComposerUi(mode = Mode.VIDEO, refAsset = ref)
                },
                onRecreate = { avatarStage = 1 },
            )
        }
        editAsset?.let { src ->
            EditImageScreen(
                asset = src,
                mode = editMode,
                onClose = { editAsset = null },
                onSubmit = { prompt, count, aspect ->
                    val proj = Repo.projectOf(src.id) ?: Repo.projects.firstOrNull()
                    if (proj != null) {
                        val target = Repo.findAsset(src.id) ?: src
                        target.status = GenStatus.GENERATING
                        target.progress = 0f
                        editAsset = null
                        screen = Screen.ProjectScreen(proj.id)
                        launchGeneration(
                            proj, listOf(target), src,
                            appendTo = target,
                            promptOverride = prompt,
                            aspectOverride = aspect,
                            countOverride = count,
                            durationOverride = if (editMode == Mode.VIDEO) 8 else null,
                        )
                    } else editAsset = null
                },
            )
        }
        if (showCreateSet) {
            CreateCollectionSheet(
                selectedCount = selectedAssets.size,
                onDismiss = { showCreateSet = false },
                onSubmit = { name ->
                    val projId = (screen as? Screen.ProjectScreen)?.id ?: Repo.projects.firstOrNull()?.id ?: ""
                    Repo.createSet(name, projId, selectedAssets.toList())
                    showCreateSet = false
                    selectedAssets = emptySet()
                    toast = "集合已创建：$name"
                },
            )
        }
        if (moveConfirm) {
            val proj = Repo.findProject((screen as? Screen.ProjectScreen)?.id)
            ConfirmDialog(
                title = "要移至回收站吗？",
                message = "${selectedAssets.size} 项内容将移至回收站，并且可以通过“更多”菜单恢复。",
                confirmLabel = "移至回收站",
                onConfirm = {
                    if (proj != null) Repo.moveToTrash(proj, selectedAssets)
                    selectedAssets = emptySet()
                    moveConfirm = false
                    toast = "已移至回收站"
                },
                onCancel = { moveConfirm = false },
            )
        }
        if (trashOpen) {
            val proj = Repo.findProject((screen as? Screen.ProjectScreen)?.id)
            if (proj != null) {
                TrashScreen(
                    project = proj,
                    onBack = { trashOpen = false },
                    onRestoreAll = { Repo.restoreAll(proj); toast = "已全部恢复" },
                    onClearAll = { Repo.clearTrash(proj); toast = "回收站已清空" },
                )
            } else trashOpen = false
        }
        if (attachOpen) {
            AttachSheet(
                onDismiss = { attachOpen = false },
                onCamera = { toast = "拍摄（系统相机）" },
                onImage = { pickImageLauncher.launch("image/*") },
                onLibrary = { resourceOpen = true },
            )
        }
        if (resourceOpen) {
            ResourceLibraryScreen(
                projects = Repo.projects,
                onClose = { resourceOpen = false },
                onPick = { a ->
                    composer = composer?.copy(attach = a)
                    resourceOpen = false
                    toast = "已附加：${a.title}"
                },
                onUpload = { pickImageLauncher.launch("image/*") },
            )
        }
        if (deleteAccountConfirm) {
            ConfirmDialog(
                title = "注销账号",
                message = "注销后账号与全部历史数据不可恢复，确定继续吗？",
                confirmLabel = "注销",
                onConfirm = {
                    deleteAccountConfirm = false
                    scope.launch {
                        val ok = Gateway.deleteAccount()
                        if (ok) {
                            AuthStore.logout()
                            Repo.credits = 0
                            toast = "账号已注销"
                        } else toast = "注销失败，请稍后重试"
                    }
                },
                onCancel = { deleteAccountConfirm = false },
            )
        }
        if (legalKind != null) {
            val isTerms = legalKind == "terms"
            LegalScreen(
                title = if (isTerms) "服务条款" else "隐私政策",
                body = if (isTerms) TERMS_TEXT else PRIVACY_TEXT,
                onBack = { legalKind = null },
            )
        }
        FlowToast(toast)
    }
}

/* ---------- 通用组件 ---------- */

@Composable
fun RoundIconButton(icon: ImageVector, onClick: () -> Unit, bg: Color = Surface, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun TopBar(
    title: String? = null,
    brand: String? = null,
    onBack: (() -> Unit)? = null,
    titleSize: androidx.compose.ui.unit.TextUnit = 19.sp,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            RoundIconButton(Icons.Outlined.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (brand != null) {
                Text(brand, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            }
            if (title != null) {
                Text(title, color = TextPrimary, fontSize = titleSize, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, bg: Color = Surface, icon: ImageVector? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
fun ResChip(res: String) {
    Box(Modifier.border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(res, color = Color(0xFFE8EAED), fontSize = 10.sp)
    }
}

@Composable
fun SimpleSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier.size(50.dp, 30.dp).clip(RoundedCornerShape(50))
            .background(if (on) Color(0xFFA8C7FA) else Color(0xFF3A3B3E))
            .clickable { onChange(!on) },
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(3.dp).size(24.dp).clip(CircleShape)
                .background(if (on) Color(0xFF0B1D3A) else Color(0xFF9AA0A6)),
        )
    }
}

/* ---------- 首页 ---------- */

private data class Template(val img: Int, val label: String, val kind: String)

private val templates = listOf(
    Template(R.drawable.tpl1, "编辑视频", "video-edit"),
    Template(R.drawable.tpl2, "编辑照片", "photo-edit"),
    Template(R.drawable.tpl3, "使用虚拟形象", "avatar"),
    Template(R.drawable.tpl4, "为照片添加动画效果", "animate-photo"),
    Template(R.drawable.tpl5, "拍摄视频", "capture-video"),
    Template(R.drawable.tpl6, "拍照", "capture-photo"),
)

@Composable
fun HomeScreen(
    onOpenProject: (String) -> Unit,
    onNewProject: () -> Unit,
    onTemplate: (String) -> Unit,
    onNotifications: () -> Unit,
    onAccount: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(brand = "Google Flow", trailing = {
            RoundIconButton(Icons.Outlined.Notifications, onNotifications, bg = Color.Transparent)
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Accent).clickable { onAccount() },
                contentAlignment = Alignment.Center,
            ) { Text("B", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        })

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            items(templates) { t ->
                Column(Modifier.width(112.dp).clickable { onTemplate(t.kind) }) {
                    Image(
                        painterResource(t.img), contentDescription = null,
                        modifier = Modifier.size(112.dp, 160.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Text(t.label, color = TextPrimary, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("项目", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            PillButton("新建", onNewProject, icon = Icons.Outlined.Add)
        }

        if (Repo.projects.isEmpty()) {
            Text("未找到任何项目。请先创建一个。", color = TextSecondary, fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(Repo.projects) { p ->
                ProjectRow(p) { onOpenProject(p.id) }
            }
        }
    }
}

@Composable
private fun ProjectRow(p: FlowProject, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val first = p.assets.firstOrNull()
        val img = when {
            first == null -> null
            first.type == AssetType.IMAGE -> R.drawable.teapot
            else -> R.drawable.video_teapot
        }
        if (img != null) {
            Image(painterResource(img), null,
                Modifier.size(92.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(14.dp)).background(Surface))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.title, color = TextPrimary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val videos = p.assets.count { it.type == AssetType.VIDEO }
                val images = p.assets.count { it.type == AssetType.IMAGE }
                if (videos > 0) {
                    Icon(Icons.Outlined.Videocam, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Text(" $videos", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.width(12.dp))
                }
                if (images > 0) {
                    Icon(Icons.Outlined.Image, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Text(" $images", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
        Icon(Icons.Outlined.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

/* ---------- 项目页 ---------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectScreen(
    project: FlowProject,
    filter: String,
    onFilter: (String) -> Unit,
    onBack: () -> Unit,
    onOpenAsset: (String) -> Unit,
    onCreate: () -> Unit,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onUpload: () -> Unit,
    onRetry: (String) -> Unit,
    gridSize: Int,
    selected: Set<String>,
    onToggleSelect: (String) -> Unit,
    onClearSelection: () -> Unit,
    onGridCycle: () -> Unit,
    onAddToSet: () -> Unit,
    onTrash: () -> Unit,
    onRename: (String) -> Unit,
    renameRequest: Boolean,
    onRenameConsumed: () -> Unit,
    sets: List<FlowSet> = emptyList(),
    onOpenSet: (String) -> Unit = {},
    agentPending: Boolean = false,
    onResumeAgent: () -> Unit = {},
) {
    var renaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(project.title) }
    val renameFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(renameRequest) {
        if (renameRequest) {
            renaming = true
            renameText = project.title
            onRenameConsumed()
        }
    }
    LaunchedEffect(renaming) {
        if (renaming) { try { renameFocus.requestFocus() } catch (e: Exception) {} }
    }
    val filtered = project.assets.filter {
        filter == "all" ||
            (filter == "fav" && it.fav) ||
            (filter == "image" && it.type == AssetType.IMAGE) ||
            (filter == "video" && it.type == AssetType.VIDEO)
    }
    val inSelection = selected.isNotEmpty()

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            when {
                renaming -> {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton(Icons.Outlined.Close, { renaming = false })
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = renameText, onValueChange = { renameText = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 22.sp),
                            cursorBrush = SolidColor(Color.White),
                            singleLine = true,
                            modifier = Modifier.weight(1f).focusRequester(renameFocus),
                        )
                        RoundIconButton(Icons.Outlined.Check, {
                            onRename(renameText.trim().ifBlank { project.title })
                            renaming = false
                        })
                    }
                }
                inSelection -> {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton(Icons.Outlined.Close, onClearSelection)
                        Spacer(Modifier.width(12.dp))
                        Text("已选择 ${selected.size} 项", color = TextPrimary, fontSize = 19.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
                else -> {
                    TopBar(title = project.title, onBack = onBack, titleSize = 24.sp, trailing = {
                        RoundIconButton(Icons.Outlined.MoreVert, onMenu)
                    })
                }
            }

            if (!inSelection && project.assets.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("全部", null, filter == "all") { onFilter("all") }
                    FilterChip("收藏夹", Icons.Outlined.Star, filter == "fav") { onFilter("fav") }
                    FilterChip("图片", Icons.Outlined.Image, filter == "image") { onFilter("image") }
                    FilterChip("视频", Icons.Outlined.Videocam, filter == "video") { onFilter("video") }
                    if (sets.isNotEmpty()) FilterChip("集合", Icons.Outlined.Star, filter == "sets") { onFilter("sets") }
                }
            }

            if (filter == "sets" && sets.isNotEmpty()) {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = GridCells.Fixed(gridSize),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(sets.size) { i ->
                        val st = sets[i]
                        val members = st.assetIds.mapNotNull { Repo.findAsset(it) }.take(4)
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(0.5625f).clip(RoundedCornerShape(20.dp))
                                .background(Surface3)
                                .clickable { onOpenSet(st.id) },
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                Row(Modifier.fillMaxWidth().weight(1f)) {
                                    members.getOrNull(0)?.localPath?.let { LocalAssetImage(it, Modifier.weight(1f).fillMaxHeight()) }
                                    members.getOrNull(1)?.localPath?.let { LocalAssetImage(it, Modifier.weight(1f).fillMaxHeight()) }
                                }
                                Row(Modifier.fillMaxWidth().weight(1f)) {
                                    members.getOrNull(2)?.localPath?.let { LocalAssetImage(it, Modifier.weight(1f).fillMaxHeight()) }
                                    members.getOrNull(3)?.localPath?.let { LocalAssetImage(it, Modifier.weight(1f).fillMaxHeight()) }
                                }
                            }
                            Column(
                                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                    .background(Color(0x99000000)).padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(st.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${st.assetIds.size} 项", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("暂无资源。\n请创建一个或从媒体库上传。", color = TextSecondary, fontSize = 16.sp,
                        textAlign = TextAlign.Center, lineHeight = 24.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridSize),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered) { a ->
                        AssetCard(
                            a = a,
                            selected = selected.contains(a.id),
                            selectionMode = inSelection,
                            onClick = { if (inSelection) onToggleSelect(a.id) else onOpenAsset(a.id) },
                            onLongClick = { onToggleSelect(a.id) },
                            onRetry = { onRetry(a.id) },
                        )
                    }
                }
            }
        }

        if (inSelection) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    val actions = listOf(
                        Triple("分享", Icons.Outlined.Share, {}),
                        Triple("收藏", Icons.Outlined.Star, {}),
                        Triple("下载", Icons.Outlined.Download, {}),
                        Triple("发送", Icons.Outlined.Send, {}),
                        Triple("回收站", Icons.Outlined.Delete, { onTrash() }),
                    )
                    actions.forEach { (label, icon, act) ->
                        Column(Modifier.width(120.dp).clickable { act() }.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, null, tint = if (label == "回收站") Color(0xFFFF8A80) else Color.White,
                                modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(label, color = if (label == "回收站") Color(0xFFFF8A80) else Color.White, fontSize = 13.sp)
                        }
                    }
                }
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp)
                        .clip(RoundedCornerShape(50)).background(Surface).clickable { onAddToSet() },
                    contentAlignment = Alignment.Center,
                ) { Text("添加到新集合", color = TextPrimary, fontSize = 16.sp) }
            }
        } else {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(Icons.Outlined.Search, onSearch, bg = Color(0xEB2C2D30), size = 56)
                Row(
                    Modifier.height(56.dp).clip(RoundedCornerShape(50)).background(Color(0xEB2C2D30))
                        .clickable { if (agentPending) onResumeAgent() else onCreate() }.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (agentPending) "轮到您了" else "创建", color = Color.White, fontSize = 16.sp)
                }
                RoundIconButton(Icons.Outlined.UploadFile, onUpload, bg = Color(0xEB2C2D30), size = 56)
            }
        }
    }
}

@Composable
fun SetDetailScreen(
    set: FlowSet,
    onBack: () -> Unit,
    onOpenAsset: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
            Text(set.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f))
            RoundIconButton(Icons.Outlined.MoreVert, { menuOpen = true })
        }
        val members = set.assetIds.mapNotNull { Repo.findAsset(it) }
        if (members.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("此集合暂无内容", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(members.size) { i ->
                    val a = members[i]
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(0.5625f).clip(RoundedCornerShape(20.dp))
                            .background(Surface3).clickable { onOpenAsset(a.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        val lp = a.localPath
                        if (lp != null) LocalAssetImage(lp, Modifier.fillMaxSize()) else Image2(a, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
    if (menuOpen) {
        Overlay({ menuOpen = false }) {
            var renameMode by remember { mutableStateOf(false) }
            var nameText by remember { mutableStateOf(set.name) }
            Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp).clip(RoundedCornerShape(28.dp)).background(Surface)) {
                if (renameMode) {
                    BasicTextField(
                        value = nameText, onValueChange = { nameText = it },
                        textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White), singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                    Box(Modifier.fillMaxWidth().clickable { onRename(nameText.trim().ifBlank { set.name }); menuOpen = false }
                        .padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text("保存", color = Color.White, fontSize = 16.sp)
                    }
                } else {
                    Box(Modifier.fillMaxWidth().clickable { renameMode = true }.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center) { Text("重命名", color = TextPrimary, fontSize = 16.sp) }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                    Box(Modifier.fillMaxWidth().clickable { menuOpen = false }.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center) { Text("网格：小", color = TextPrimary, fontSize = 16.sp) }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                    Box(Modifier.fillMaxWidth().clickable { onDelete() }.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center) { Text("删除集合", color = Color(0xFFFF8A80), fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(50))
            .background(if (selected) Color.White else Surface)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) Color.Black else Color.White,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = if (selected) Color.Black else Color.White, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}

private fun fmtDur(raw: String?): String {
    if (raw.isNullOrBlank()) return "0:06"
    if (raw.contains(":")) return raw
    val sec = raw.toIntOrNull() ?: return "0:06"
    return "%d:%02d".format(sec / 60, sec % 60)
}

@Composable
private fun LocalVideoThumb(path: String, modifier: Modifier = Modifier) {
    val bmp = remember(path) {
        try {
            val r = android.media.MediaMetadataRetriever()
            r.setDataSource(path)
            r.getFrameAtTime(100_000)
        } catch (e: Exception) {
            null
        }
    }
    if (bmp != null) {
        Image(bmp.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(Surface3))
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AssetCard(
    a: FlowAsset,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.5625f).clip(RoundedCornerShape(20.dp)).background(Surface3)
            .then(if (selected) Modifier.border(3.dp, Color.White, RoundedCornerShape(20.dp)) else Modifier)
            .combinedClickable(
                enabled = true,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        if (a.status == GenStatus.COMPLETED && a.versions.size > 1) {
            Text(
                "${a.versions.size} 项",
                color = Color(0xFFE8EAED), fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0x99000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (a.status == GenStatus.GENERATING) {
            Text("${a.progress.toInt()}%", color = Color(0xFFE8EAED), fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            Text("正在生成...", color = Color(0xFFE8EAED), fontSize = 15.sp,
                modifier = Modifier.align(Alignment.Center))
        } else if (a.status == GenStatus.FAILED) {
            Icon(
                Icons.Outlined.Warning, "生成失败",
                tint = Color(0xFF9AA0A6), modifier = Modifier.align(Alignment.Center).size(30.dp),
            )
        } else {
            val lp = a.localPath
            if (lp != null && a.type == AssetType.IMAGE) {
                LocalAssetImage(lp, Modifier.fillMaxSize())
            } else if (lp != null && a.type == AssetType.VIDEO) {
                LocalVideoThumb(lp, Modifier.fillMaxSize())
            } else {
                Image(
                    painterResource(if (a.type == AssetType.IMAGE) R.drawable.teapot else R.drawable.video_teapot),
                    null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                )
            }
            if (a.type == AssetType.VIDEO) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).clip(CircleShape)
                        .background(Color(0x73000000)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

/* ---------- 创建面板 ---------- */

@Composable
fun ComposerScreen(
    ui: ComposerUi,
    chat: List<Pair<String, String>>,
    typing: Boolean,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onAttach: () -> Unit,
    onStop: () -> Unit = {},
    onRetryChat: () -> Unit = {},
    onChange: (ComposerUi) -> Unit,
    onClose: () -> Unit,
    onSend: (ComposerUi) -> Unit,
) {
    val models = if (ui.mode == Mode.IMAGE) Repo.imageModels else Repo.videoModels
    val model = models.firstOrNull { it.id == ui.modelId } ?: models.first()
    val placeholder = if (ui.mode == Mode.AGENT) "您想制作什么内容?" else "您想制作什么内容?"
    val canSend = ui.text.isNotBlank()

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.Close, onClose)
            Spacer(Modifier.weight(1f))
            if (ui.mode == Mode.AGENT) RoundIconButton(Icons.Outlined.History, onHistory)
        }

        if (ui.mode == Mode.AGENT) {
            if (chat.isNotEmpty()) {
                AgentChat(chat, typing, onRetryChat)
            } else if (ui.text.isBlank()) {
                var sugIdx by remember { mutableStateOf(0) }
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(5000)
                        sugIdx = (sugIdx + 1) % 3
                    }
                }
                val suggestionGroups = listOf(
                    listOf("✏️" to "编辑图片", "🎬" to "制作分镜画面", "💸" to "了解生成费用"),
                    listOf("🧭" to "应该怎么开始？", "🎨" to "制作视觉情绪板", "✏️" to "编辑图片"),
                    listOf(
                        "🖼" to "描述图片或视频，我来为你生成",
                        "🎭" to "构思角色、场景或世界观并赋予生命",
                        "🎬" to "逐场景创作故事并制作分镜",
                        "🗂" to "整理和管理你的项目收藏夹",
                        "💡" to "了解 Flow 的功能与使用技巧",
                    ),
                )
                val suggestions = suggestionGroups[sugIdx]
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    suggestions.forEach { (emoji, text) ->
                        Row(
                            Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(20.dp)).background(Surface)
                                .clickable {
                                    // 原版：点建议卡立即发送并触发回复
                                    onChange(ui.copy(text = text))
                                    onSend(ui.copy(text = text))
                                }
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(14.dp))
                            Text(text, color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            ModeTab(Icons.Outlined.Image, "图片", ui.mode == Mode.IMAGE) { onChange(ui.copy(mode = Mode.IMAGE)) }
            Spacer(Modifier.width(6.dp))
            ModeTab(Icons.Outlined.Videocam, "视频", ui.mode == Mode.VIDEO) { onChange(ui.copy(mode = Mode.VIDEO)) }
            Spacer(Modifier.width(6.dp))
            ModeTab(Icons.Outlined.AutoAwesome, "智能体", ui.mode == Mode.AGENT) { onChange(ui.copy(mode = Mode.AGENT)) }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)
                .clip(RoundedCornerShape(28.dp)).background(Surface2)
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (ui.attach != null) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Surface3)) {
                        val lp = ui.attach.localPath
                        if (lp != null) LocalAssetImage(lp, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(ui.attach.title, color = Color(0xFFE8EAED), fontSize = 13.sp,
                        modifier = Modifier.weight(1f), maxLines = 1)
                    Icon(Icons.Outlined.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp)
                        .clickable { onChange(ui.copy(attach = null)) })
                }
            }
            BasicTextField(
                value = ui.text,
                onValueChange = { onChange(ui.copy(text = it)) },
                textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                decorationBox = { inner ->
                    Box {
                        if (ui.text.isEmpty()) Text(placeholder, color = TextSecondary, fontSize = 17.sp)
                        inner()
                    }
                },
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Outlined.Add, onAttach, bg = Color.Transparent, size = 40)
                Spacer(Modifier.weight(1f))
                if (ui.mode == Mode.AGENT) {
                    RoundIconButton(Icons.Outlined.Settings, onSettings, bg = Color.Transparent, size = 40)
                } else {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
                            .clickable { onChange(ui.copy(settingsOpen = !ui.settingsOpen)) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (ui.animate) "添加动画效果" else model.name, color = Color(0xFFE8EAED), fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("${ui.count} 份", color = Color(0xFFE8EAED), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (typing && ui.mode == Mode.AGENT) {
                    Box(
                        Modifier.height(44.dp).clip(RoundedCornerShape(50)).background(Surface)
                            .clickable { onStop() }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("停止", color = TextPrimary, fontSize = 14.sp) }
                } else {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(if (canSend) Color.White else Surface3)
                            .clickable(enabled = canSend) { onSend(ui) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Send, null, tint = if (canSend) Color.Black else TextSecondary,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (ui.settingsOpen && ui.mode != Mode.AGENT) {
            val cost = if (ui.mode == Mode.VIDEO) Repo.videoCost(ui.res, ui.duration) * ui.count
                       else Repo.imageCost()
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)
                    .clip(RoundedCornerShape(24.dp)).background(Surface2).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("本次生成将消耗 $cost 个 AI 点数", color = TextPrimary, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                SegRow(
                    if (ui.mode == Mode.IMAGE) Repo.aspects
                    else if (ui.animate) listOf("16:9", "9:16")
                    else Repo.videoAspects,
                    ui.aspect,
                ) { onChange(ui.copy(aspect = it)) }
                if (ui.mode == Mode.VIDEO && model.resolutions != null) {
                    SegRow(if (ui.animate) listOf("360p", "720p") else model.resolutions, ui.res) { onChange(ui.copy(res = it)) }
                }
                SegRow(Repo.counts.map { "$it 份" }, "${ui.count} 份") {
                    onChange(ui.copy(count = it.first().digitToInt()))
                }
                if (ui.mode == Mode.VIDEO && model.durations != null && !ui.animate) {
                    SegRow(model.durations.map { "$it 秒" }, "${ui.duration} 秒") {
                        onChange(ui.copy(duration = it.split(" ")[0].toInt()))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)
                        .clickable { onChange(ui.copy(modelListOpen = !ui.modelListOpen)) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${model.icon} ${model.name}", color = TextPrimary, fontSize = 15.sp,
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.KeyboardArrowDown, null, tint = TextSecondary)
                }
                if (ui.modelListOpen) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)) {
                        models.forEach { m ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (m.id == model.id) Surface3 else Color.Transparent)
                                    .clickable { onChange(ui.copy(modelId = m.id, modelListOpen = false)) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${m.icon} ${m.name}", color = TextPrimary, fontSize = 15.sp,
                                    modifier = Modifier.weight(1f))
                                Text(if (m.cost == 0) "0 个点数" else "${m.cost} 个点数/次",
                                    color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModeTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(Surface).clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, color = Color.White, fontSize = 15.sp)
        }
    } else {
        Box(
            Modifier.size(46.dp).clip(CircleShape).clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
fun SegRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(4.dp)) {
        options.forEach { opt ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (opt == selected) Surface3 else Color.Transparent)
                    .clickable { onSelect(opt) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(opt, color = if (opt == selected) Color.White else TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

/* ---------- 素材详情 ---------- */

@Composable
fun AssetScreen(
    asset: FlowAsset,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onExtend: () -> Unit,
    onAnimate: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit = {},
    onDuplicatePrompt: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Outlined.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(asset.title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(asset.model, color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    ResChip(asset.res)
                }
            }
            RoundIconButton(Icons.Outlined.MoreVert, onMenu)
        }

        val versions = asset.versions
        var vIdx by remember(asset.id) { mutableStateOf((versions.size - 1).coerceAtLeast(0)) }
        val currentPath = versions.getOrNull(vIdx) ?: asset.localPath
        if (asset.type == AssetType.VIDEO) {
            val videoPath = versions.getOrNull(vIdx) ?: asset.localPath
            var dragAcc by remember(asset.id) { mutableStateOf(0f) }
            Box(
                Modifier.fillMaxWidth().weight(1f).pointerInput(versions.size, vIdx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount -> dragAcc += amount },
                        onDragEnd = {
                            if (versions.size > 1) {
                                if (dragAcc < -80f) vIdx = (vIdx + 1) % versions.size
                                else if (dragAcc > 80f) vIdx = (vIdx - 1 + versions.size) % versions.size
                            }
                            dragAcc = 0f
                        },
                    )
                },
            ) {
                androidx.compose.runtime.key(videoPath) {
                    VideoAssetView(asset, Modifier.fillMaxSize(), path = videoPath)
                }
                if (versions.size > 1) {
                    // 原版：右侧竖直分段指示条（点击/滑动切版本）
                    Column(
                        Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
                            .clip(RoundedCornerShape(50)).background(Color(0xCC2C2D30))
                            .padding(horizontal = 5.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(versions.size) { i ->
                            Box(
                                Modifier.size(24.dp).clickable { vIdx = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.width(5.dp).height(if (i == vIdx) 18.dp else 5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (i == vIdx) Color.White else Color(0x66FFFFFF)),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            var dragAcc2 by remember(asset.id) { mutableStateOf(0f) }
            Box(
                Modifier.fillMaxWidth().weight(1f).pointerInput(versions.size, vIdx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount -> dragAcc2 += amount },
                        onDragEnd = {
                            if (versions.size > 1) {
                                if (dragAcc2 < -80f) vIdx = (vIdx + 1) % versions.size
                                else if (dragAcc2 > 80f) vIdx = (vIdx - 1 + versions.size) % versions.size
                            }
                            dragAcc2 = 0f
                        },
                    )
                },
                contentAlignment = Alignment.Center,
            ) {
                if (currentPath != null) {
                    LocalAssetImage(currentPath, Modifier.fillMaxWidth())
                } else {
                    Image(painterResource(R.drawable.teapot), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                }
                if (versions.size > 1) {
                    // 原版：右侧竖直分段指示条（点击/滑动切版本）
                    Column(
                        Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
                            .clip(RoundedCornerShape(50)).background(Color(0xCC2C2D30))
                            .padding(horizontal = 5.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(versions.size) { i ->
                            Box(
                                Modifier.size(24.dp).clickable { vIdx = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.width(5.dp).height(if (i == vIdx) 18.dp else 5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (i == vIdx) Color.White else Color(0x66FFFFFF)),
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (asset.status == GenStatus.FAILED) {
                DetailAction("重试", Icons.Outlined.Refresh, Modifier.weight(1f), onRetry)
            } else if (asset.status == GenStatus.GENERATING) {
                DetailAction("取消生成", Icons.Outlined.Close, Modifier.weight(1f), onCancel)
                DetailAction("重复使用提示", Icons.Outlined.Refresh, Modifier.weight(1f), onDuplicatePrompt)
            } else {
            DetailAction("修改", Icons.Outlined.Edit, Modifier.weight(1f)) { currentPath?.let { asset.localPath = it }; onEdit() }
            if (asset.type == AssetType.VIDEO) {
                DetailAction("延长", Icons.Outlined.FastForward, Modifier.weight(1f)) {
                    // 对「当前正在看的版本」做延长（原版行为），而不是永远取最新版本
                    currentPath?.let { asset.localPath = it }
                    onExtend()
                }
            } else {
                DetailAction("添加动画效果", Icons.Outlined.AutoAwesome, Modifier.weight(1f), onAnimate)
            }
            }
        }
    }
}

@Composable
private fun DetailAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Surface)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

/* ---------- 通知 / 账号 ---------- */

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpen: () -> Unit,
    serverJobs: List<Gateway.JobInfo> = emptyList(),
    serverNotifs: List<Gateway.ServerNotification> = emptyList(),
    onRefresh: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        LaunchedEffect(Unit) { onRefresh() }
        TopBar(title = "通知", onBack = onBack, trailing = {
            Text("剩余点数：${Repo.credits}", color = Color(0xFFE8EAED), fontSize = 14.sp)
        })
        if (serverNotifs.isEmpty() && Repo.notifications.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("暂无通知", color = TextSecondary, fontSize = 15.sp)
            }
            return
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 服务端通知（跨设备兜底：仅当本机没有本地通知时展示，避免与原版单列表重复）
            val showServer = Repo.notifications.isEmpty()
            if (showServer) items(serverNotifs.size) { i ->
                val n = serverNotifs[i]
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface)
                        .clickable { onOpen() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Surface3), contentAlignment = Alignment.Center) {
                        Text("✅", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(n.title, color = TextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(n.body, color = Color(0xFFDADCE0), fontSize = 13.sp, lineHeight = 19.sp, maxLines = 3)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("刚刚", color = TextSecondary, fontSize = 11.sp)
                }
            }
            // 本地通知（含项目名与相对时间）
            items(Repo.notifications) { (text, time) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface)
                        .clickable { onOpen() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val thumb = remember {
                        Repo.projects.asSequence().flatMap { it.assets.asSequence() }
                            .firstOrNull { !it.localPath.isNullOrBlank() }?.localPath
                    }
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(Surface3)) {
                        if (thumb != null) LocalAssetImage(thumb, Modifier.fillMaxSize())
                        else Text("🔔", fontSize = 20.sp, modifier = Modifier.align(Alignment.Center))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(time, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onOpenActivity: () -> Unit,
    onLogout: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
) {
    var watermark by remember { mutableStateOf(false) }
    var improve by remember { mutableStateOf(true) }
    var pushEnabled by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(title = "账号", onBack = onBack)
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                    Text("B", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    val accountName = (AuthStore.email?.substringBefore("@") ?: "游客账号")
                    Text(accountName, color = TextPrimary, fontSize = 17.sp)
                    Text(AuthStore.email ?: "本机游客模式", color = TextSecondary, fontSize = 13.sp)
                    Box(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).background(Surface3)
                        .padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("PRO", color = Color(0xFFE8EAED), fontSize = 11.sp)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).padding(16.dp)) {
                Text("${Repo.credits} 个 Google Flow 点数", color = TextPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50)).clickable { onOpenActivity() },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("查看活动", color = TextPrimary, fontSize = 15.sp)
                }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)) {
                AccountRow("创建虚拟形象", Icons.Outlined.Person) {}
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                AccountRowSwitch("可见水印", watermark) { watermark = it }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                AccountRowSwitch("帮助改进 Flow", improve) { improve = it }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                AccountRowSwitch("推送通知", pushEnabled) { pushEnabled = it }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)) {
                AccountRow("服务条款", Icons.Outlined.Description, onOpenTerms)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                AccountRow("隐私政策", Icons.Outlined.PrivacyTip, onOpenPrivacy)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                AccountRow("退出账号", Icons.Outlined.ExitToApp, onLogout)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                Row(Modifier.fillMaxWidth().clickable { onDeleteAccount() }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DeleteForever, null, tint = Color(0xFFFF8A80), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("注销账号", color = Color(0xFFFF8A80), fontSize = 16.sp)
                }
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AccountRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFFE8EAED), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AccountRowSwitch(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        SimpleSwitch(on, onChange)
    }
}
