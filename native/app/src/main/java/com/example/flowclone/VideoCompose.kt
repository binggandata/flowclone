package com.example.flowclone

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 用 Media3 Transformer 把多个视频拼接并统一重新编码。
 * 关键点：网关不同次生成的视频分辨率可能不同（例如 416x752 与 400x736），
 * 直接用 MediaMuxer 拼接会让第二段无法解码（播放到第 8 秒后花屏/黑屏）。
 * 这里先统一缩放到第一段的分辨率，再整体转码拼接。
 */
object VideoCompose {
    data class Size(val width: Int, val height: Int)

    fun videoSize(file: File): Size? = try {
        val ex = MediaExtractor()
        ex.setDataSource(file.absolutePath)
        var result: Size? = null
        for (i in 0 until ex.trackCount) {
            val fmt = ex.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) {
                val w = fmt.getInteger(MediaFormat.KEY_WIDTH)
                val h = fmt.getInteger(MediaFormat.KEY_HEIGHT)
                result = Size(w, h)
                break
            }
        }
        ex.release()
        result
    } catch (e: Exception) { null }

    /** 返回是否成功；成功后 out 为重新编码的、可整段播放的视频 */
    suspend fun concat(context: Context, inputs: List<File>, out: File): Boolean {
        if (inputs.size < 2) return false
        val target = videoSize(inputs[0]) ?: return false
        return suspendCoroutine { cont ->
            Handler(Looper.getMainLooper()).post {
                try {
                    val items = inputs.map { f ->
                        val effects = Effects(
                            emptyList(), // audio processors 默认
                            listOf(Presentation.createForWidthAndHeight(
                                target.width, target.height,
                                Presentation.LAYOUT_SCALE_TO_FIT,
                            )),
                        )
                        EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(f)))
                            .setEffects(effects)
                            .build()
                    }
                    val composition = Composition.Builder(
                        listOf(EditedMediaItemSequence(items)),
                    ).build()
                    var once = false
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (!once) { once = true; cont.resume(out.exists() && out.length() > 0) }
                            }
                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                if (!once) { once = true; out.delete(); cont.resume(false) }
                            }
                        })
                        .build()
                    // 防止被 GC
                    holder = transformer
                    transformer.start(composition, out.absolutePath)
                } catch (e: Exception) {
                    out.delete()
                    cont.resume(false)
                }
            }
        }
    }

    @Volatile
    private var holder: Transformer? = null
}
