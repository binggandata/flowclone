package com.example.flowclone

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AssetType { IMAGE, VIDEO }
enum class GenStatus { GENERATING, COMPLETED, FAILED }

class FlowAsset(
    val id: String,
    val type: AssetType,
    val title: String,
    val model: String,
    val res: String,
    val prompt: String,
    status0: GenStatus,
    progress0: Float,
    var duration: String? = null,
    var localPath: String? = null,
    val aspect: String = "9:16",
    val quality: String = "720p",
) {
    var status by mutableStateOf(status0)
    var progress by mutableStateOf(progress0)
    var fav by mutableStateOf(false)
    val versions = mutableStateListOf<String>()
    var jobId: String? = null
    var lastError: String? = null
    // 延长任务：记录被延长的原视频路径，便于进程被杀后恢复时重新拼接
    var extendBasePath: String? = null
}

class FlowProject(id: String, title: String) {
    val id: String = id
    var title by mutableStateOf(title)
    val assets = mutableStateListOf<FlowAsset>()
    val trashed = mutableStateListOf<FlowAsset>()
    var agentPending by mutableStateOf(false)
}

data class FlowModel(
    val id: String,
    val name: String,
    val icon: String,
    val cost: Int,
    val resolutions: List<String>? = null,
    val durations: List<Int>? = null,
)

enum class Mode { AGENT, IMAGE, VIDEO }

class FlowSet(
    val id: String,
    var name: String,
    val projectId: String,
    assetIds: List<String> = emptyList(),
) {
    val assetIds = mutableStateListOf<String>().apply { addAll(assetIds) }
}

object Repo {
    var credits by mutableStateOf(1050)
    var agentAlways by mutableStateOf(true)
    var avatarVideoPath by mutableStateOf<String?>(null)
    var avatarImagePath by mutableStateOf<String?>(null)
    val sets = mutableStateListOf<FlowSet>()
    val projects = mutableStateListOf<FlowProject>()
    val notifications = mutableStateListOf<Pair<String, String>>()
    private var seq = 100

    val imageModels = listOf(
        FlowModel("grok-imagine-image-1.5", "Grok Imagine Image 1.5", "🖼", 0),
    )
    val videoModels = listOf(
        FlowModel("grok-imagine-video-1.5", "Grok Imagine Video 1.5", "🎬", 10,
            listOf("480p", "720p"), listOf(6, 8, 10, 12)),
    )
    val aspects = listOf("16:9", "4:3", "1:1", "3:4", "9:16")
    val videoAspects = listOf("16:9", "9:16")
    val counts = listOf(1, 2, 3, 4)

    private var prefs: SharedPreferences? = null
    private var ready = false

    fun init(context: Context) {
        if (ready) return
        ready = true
        prefs = context.getSharedPreferences("flow_repo", Context.MODE_PRIVATE)
        val raw = prefs?.getString("data", null)
        if (raw != null && loadJson(raw)) return
        seed()
        persist()
    }

    fun seed() {
        projects.clear()
        notifications.clear()
        credits = 1050
        val p = FlowProject("p1", nowTitle())
        p.assets.add(FlowAsset("a1", AssetType.IMAGE, "Teapot on table", "Nano Banana 2.1", "768p",
            "a photorealistic teal ceramic teapot on an ivory table", GenStatus.COMPLETED, 100f))
        p.assets.add(FlowAsset("a2", AssetType.VIDEO, "Steam rising from teapot", "Veo 3.1 - Lite", "720p",
            "gentle camera push in, steam rising from the teapot", GenStatus.COMPLETED, 100f, "0:06"))
        projects.add(p)
        notifications.add("成功！ 您的视频“gentle camera push in, steam rising from the teapot”已生成完毕。点按即可查看。" to "4 分钟")
    }

    fun persist() {
        val p = prefs ?: return
        p.edit().putString("data", toJson().toString()).apply()
    }

    private fun toJson(): JSONObject {
        val root = JSONObject()
        root.put("credits", credits)
        root.put("agentAlways", agentAlways)
        avatarVideoPath?.let { root.put("avatarVideo", it) }
        avatarImagePath?.let { root.put("avatarImage", it) }
        val setArr = JSONArray()
        sets.forEach { st ->
            val sj = JSONObject()
            sj.put("id", st.id); sj.put("name", st.name); sj.put("projectId", st.projectId)
            val ids = JSONArray(); st.assetIds.forEach { ids.put(it) }; sj.put("assetIds", ids)
            setArr.put(sj)
        }
        root.put("sets", setArr)
        root.put("seq", seq)
        val arr = JSONArray()
        for (proj in projects) {
            val pj = JSONObject()
            pj.put("id", proj.id)
            pj.put("title", proj.title)
            pj.put("agentPending", proj.agentPending)
            val asArr = JSONArray()
            for (a in proj.assets) {
                val aj = JSONObject()
                aj.put("id", a.id)
                aj.put("type", a.type.name)
                aj.put("title", a.title)
                aj.put("model", a.model)
                aj.put("res", a.res)
                aj.put("prompt", a.prompt)
                aj.put("status", a.status.name)
                aj.put("progress", a.progress.toDouble())
                if (a.duration != null) aj.put("duration", a.duration)
                if (a.localPath != null) aj.put("localPath", a.localPath)
                aj.put("aspect", a.aspect)
                aj.put("quality", a.quality)
                aj.put("fav", a.fav)
                a.jobId?.let { aj.put("jobId", it) }
                a.extendBasePath?.let { aj.put("extendBasePath", it) }
                val vArr = JSONArray()
                a.versions.forEach { vArr.put(it) }
                aj.put("versions", vArr)
                asArr.put(aj)
            }
            pj.put("assets", asArr)
            val trArr = JSONArray()
            for (a in proj.trashed) {
                val aj = JSONObject()
                aj.put("id", a.id); aj.put("type", a.type.name); aj.put("title", a.title)
                aj.put("model", a.model); aj.put("res", a.res); aj.put("prompt", a.prompt)
                aj.put("status", a.status.name); aj.put("progress", a.progress.toDouble())
                if (a.duration != null) aj.put("duration", a.duration)
                if (a.localPath != null) aj.put("localPath", a.localPath)
                aj.put("aspect", a.aspect); aj.put("quality", a.quality)
                aj.put("fav", a.fav)
                trArr.put(aj)
            }
            pj.put("trashed", trArr)
            arr.put(pj)
        }
        root.put("projects", arr)
        val nArr = JSONArray()
        for (n in notifications) {
            val nj = JSONObject()
            nj.put("text", n.first)
            nj.put("time", n.second)
            nArr.put(nj)
        }
        root.put("notifications", nArr)
        return root
    }

    private fun loadJson(raw: String): Boolean {
        return try {
            val root = JSONObject(raw)
            credits = root.optInt("credits", 1050)
            agentAlways = root.optBoolean("agentAlways", true)
            if (root.has("avatarVideo")) avatarVideoPath = root.optString("avatarVideo")
            if (root.has("avatarImage")) avatarImagePath = root.optString("avatarImage")
            sets.clear()
            val setArr = root.optJSONArray("sets") ?: JSONArray()
            for (i in 0 until setArr.length()) {
                val item = setArr.opt(i)
                if (item is JSONObject) {
                    val ids = mutableStateListOf<String>()
                    val idArr = item.optJSONArray("assetIds") ?: JSONArray()
                    for (k in 0 until idArr.length()) ids.add(idArr.optString(k))
                    sets.add(FlowSet(item.optString("id"), item.optString("name"), item.optString("projectId"), ids))
                }
            }
            seq = root.optInt("seq", 100)
            projects.clear()
            val arr = root.optJSONArray("projects") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val pj = arr.getJSONObject(i)
                val proj = FlowProject(pj.optString("id"), pj.optString("title"))
                proj.agentPending = pj.optBoolean("agentPending", false)
                val asArr = pj.optJSONArray("assets") ?: JSONArray()
                for (j in 0 until asArr.length()) {
                    val aj = asArr.getJSONObject(j)
                    val type = if (aj.optString("type") == "VIDEO") AssetType.VIDEO else AssetType.IMAGE
                    val rawStatus = aj.optString("status")
                    val hasJob = aj.has("jobId") && aj.optString("jobId").isNotEmpty()
                    val status = when (rawStatus) {
                        "GENERATING" -> if (hasJob) GenStatus.GENERATING else GenStatus.FAILED
                        "FAILED" -> GenStatus.FAILED
                        else -> GenStatus.COMPLETED
                    }
                    val loadedAsset = FlowAsset(
                        aj.optString("id"), type, aj.optString("title"), aj.optString("model"),
                        aj.optString("res"), aj.optString("prompt"), status,
                        aj.optDouble("progress", 100.0).toFloat(),
                        if (aj.has("duration")) aj.optString("duration") else null,
                        if (aj.has("localPath")) aj.optString("localPath") else null,
                        aj.optString("aspect", "9:16"),
                        aj.optString("quality", "720p"),
                    )
                    loadedAsset.fav = aj.optBoolean("fav", false)
                    if (aj.has("jobId")) loadedAsset.jobId = aj.optString("jobId")
                    if (aj.has("extendBasePath")) loadedAsset.extendBasePath = aj.optString("extendBasePath")
                    val vArr2 = aj.optJSONArray("versions") ?: JSONArray()
                    for (k in 0 until vArr2.length()) loadedAsset.versions.add(vArr2.optString(k))
                    if (loadedAsset.versions.isEmpty() && loadedAsset.localPath != null) {
                        loadedAsset.versions.add(loadedAsset.localPath!!)
                    }
                    proj.assets.add(loadedAsset)
                }
                val trArr2 = pj.optJSONArray("trashed") ?: JSONArray()
                for (j in 0 until trArr2.length()) {
                    val aj = trArr2.getJSONObject(j)
                    val type = if (aj.optString("type") == "VIDEO") AssetType.VIDEO else AssetType.IMAGE
                    val status = when (aj.optString("status")) {
                        "GENERATING" -> GenStatus.FAILED
                        "FAILED" -> GenStatus.FAILED
                        else -> GenStatus.COMPLETED
                    }
                    proj.trashed.add(FlowAsset(
                        aj.optString("id"), type, aj.optString("title"), aj.optString("model"),
                        aj.optString("res"), aj.optString("prompt"), status,
                        aj.optDouble("progress", 100.0).toFloat(),
                        if (aj.has("duration")) aj.optString("duration") else null,
                        if (aj.has("localPath")) aj.optString("localPath") else null,
                        aj.optString("aspect", "9:16"), aj.optString("quality", "720p"),
                    ).also { it.fav = aj.optBoolean("fav", false) })
                }
                projects.add(proj)
            }
            notifications.clear()
            val nArr = root.optJSONArray("notifications") ?: JSONArray()
            for (i in 0 until nArr.length()) {
                val nj = nArr.getJSONObject(i)
                notifications.add(nj.optString("text") to nj.optString("time"))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun nowTitle(): String {
        val f = SimpleDateFormat("M月 dd, hh:mm a", Locale.CHINA)
        return f.format(Date())
    }

    fun nextId(): String { seq += 1; return "x$seq" }

    fun createProject(): FlowProject {
        val p = FlowProject(nextId(), nowTitle())
        projects.add(0, p)
        persist()
        return p
    }

    fun findProject(id: String?): FlowProject? = projects.firstOrNull { it.id == id }
    fun findAsset(id: String?): FlowAsset? = projects.firstNotNullOfOrNull { p -> p.assets.firstOrNull { it.id == id } }
    fun projectOf(assetId: String?): FlowProject? = projects.firstOrNull { p -> p.assets.any { it.id == assetId } }

    fun submit(
        project: FlowProject,
        mode: Mode,
        prompt: String,
        model: FlowModel,
        aspect: String,
        count: Int,
        res: String,
        duration: Int,
    ): List<FlowAsset> {
        val cost = model.cost * count
        credits = (credits - cost).coerceAtLeast(0)
        val created = mutableListOf<FlowAsset>()
        repeat(count) {
            val a = FlowAsset(
                id = nextId(),
                type = if (mode == Mode.IMAGE) AssetType.IMAGE else AssetType.VIDEO,
                title = autoTitle(prompt),
                model = model.name,
                res = if (mode == Mode.IMAGE) "768p" else (model.resolutions?.let { res } ?: "720p"),
                prompt = prompt,
                status0 = GenStatus.GENERATING,
                progress0 = 0f,
                duration = if (mode == Mode.VIDEO) "${model.durations?.let { duration } ?: 8}" else null,
                aspect = aspect,
                quality = res,
            )
            project.assets.add(0, a)
            created.add(a)
        }
        persist()
        return created
    }

    fun addLocalAsset(project: FlowProject, type: AssetType, path: String, title: String) {
        val a = FlowAsset(
            id = nextId(), type = type, title = title, model = "本地素材", res = "原始",
            prompt = "", status0 = GenStatus.COMPLETED, progress0 = 100f,
            duration = if (type == AssetType.VIDEO) "0:06" else null, localPath = path,
        )
        project.assets.add(0, a)
        persist()
    }

    fun complete(a: FlowAsset) {
        a.status = GenStatus.COMPLETED
        a.progress = 100f
        notifications.add(0, "成功！ 您的${if (a.type == AssetType.VIDEO) "视频" else "图片"}“${a.prompt}”已生成完毕。点按即可查看。" to "刚刚")
        persist()
    }

    fun autoTitle(prompt: String): String {
        val stop = setOf("a", "an", "the", "of", "on", "in", "with", "and", "to", "for", "at")
        val words = prompt.trim().split(Regex("[\\s,.!?]+")).filter { it.isNotBlank() && it.lowercase() !in stop }
        val pick = words.take(3).joinToString(" ")
        if (pick.isBlank()) return "未命名创作"
        return pick[0].uppercase() + pick.drop(1)
    }

    fun tickGenerating() {
        for (p in projects) {
            for (a in p.assets) {
                if (a.status == GenStatus.GENERATING) {
                    val step = if (a.type == AssetType.IMAGE) 34f else 5f
                    a.progress = (a.progress + step).coerceAtMost(100f)
                    if (a.progress >= 100f) {
                        if (a.type == AssetType.VIDEO && Math.random() < 0.18) {
                            a.status = GenStatus.FAILED
                            persist()
                        } else {
                            complete(a)
                        }
                    }
                }
            }
        }
    }

    fun retry(a: FlowAsset) {
        a.status = GenStatus.GENERATING
        a.progress = 0f
        persist()
    }

    fun moveToTrash(project: FlowProject, ids: Set<String>) {
        val moving = project.assets.filter { ids.contains(it.id) }
        project.assets.removeAll { ids.contains(it.id) }
        project.trashed.addAll(moving)
        persist()
    }

    fun restoreAll(project: FlowProject) {
        project.assets.addAll(0, project.trashed)
        project.trashed.clear()
        persist()
    }

    fun clearTrash(project: FlowProject) {
        project.trashed.clear()
        persist()
    }

    fun pendingJobGroups(): List<Pair<String, List<FlowAsset>>> {
        val map = LinkedHashMap<String, MutableList<FlowAsset>>()
        projects.forEach { p ->
            p.assets.filter { (it.status == GenStatus.GENERATING || it.status == GenStatus.FAILED) && !it.jobId.isNullOrBlank() }
                .forEach { a -> map.getOrPut(a.jobId!!) { mutableListOf() }.add(a) }
        }
        return map.map { it.key to it.value.toList() }
    }

    fun videoCost(quality: String, durationSec: Int): Int =
        if (quality == "720p") 3 * durationSec else durationSec

    fun imageCost(): Int = 0

    fun toggleFavorite(a: FlowAsset) {
        a.fav = !a.fav
        persist()
    }

    fun createSet(name: String, projectId: String, assetIds: List<String>): FlowSet {
        val set = FlowSet("s${System.currentTimeMillis()}", name, projectId, assetIds)
        sets.add(set)
        persist()
        return set
    }

    fun findSet(id: String): FlowSet? = sets.firstOrNull { it.id == id }
}

