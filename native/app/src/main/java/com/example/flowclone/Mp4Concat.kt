package com.example.flowclone

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * 把多个 MP4 顺序拼接成一个 MP4（用于「延长」：原视频 + 续写片段 = 更长视频）。
 * 假设所有输入的视频/音频编码参数一致（同一模型生成的产物）。
 */
object Mp4Concat {
    private const val TIMEOUT_US = 10_000L

    fun concat(inputs: List<File>, out: File): Boolean {
        if (inputs.isEmpty()) return false
        if (inputs.size == 1) return inputs[0].copyTo(out, overwrite = true).exists()
        var muxer: MediaMuxer? = null
        val extractors = mutableListOf<MediaExtractor>()
        try {
            val first = MediaExtractor().apply { setDataSource(inputs[0].absolutePath) }
            extractors.add(first)
            var videoTrack = -1
            var audioTrack = -1
            for (i in 0 until first.trackCount) {
                val fmt = first.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                when {
                    mime.startsWith("video/") && videoTrack < 0 -> videoTrack = i
                    mime.startsWith("audio/") && audioTrack < 0 -> audioTrack = i
                }
            }
            if (videoTrack < 0) return false
            val videoFmt = first.getTrackFormat(videoTrack)
            val audioFmt = if (audioTrack >= 0) first.getTrackFormat(audioTrack) else null
            val videoMime = videoFmt.getString(MediaFormat.KEY_MIME)!!

            muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outVideo = muxer.addTrack(videoFmt)
            val outAudio = if (audioFmt != null) muxer.addTrack(audioFmt) else -1
            muxer.start()

            var timeOffsetUs = 0L
            val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()

            for (fi in inputs.indices) {
                val ext = if (fi == 0) first else MediaExtractor().apply { setDataSource(inputs[fi].absolutePath) }
                if (fi > 0) extractors.add(ext)
                val inDurationUs = durationUs(inputs[fi])
                // 视频轨
                val vt = findTrack(ext, "video/")
                if (vt >= 0 && videoMime.isNotEmpty()) {
                    ext.selectTrack(vt)
                    ext.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    while (true) {
                        buffer.clear()
                        val size = ext.readSampleData(buffer, 0)
                        if (size < 0) break
                        info.offset = 0
                        info.size = size
                        info.presentationTimeUs = ext.sampleTime + timeOffsetUs
                        info.flags = if (ext.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                            MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        muxer.writeSampleData(outVideo, buffer, info)
                        ext.advance()
                    }
                    ext.unselectTrack(vt)
                }
                // 音频轨
                if (outAudio >= 0) {
                    val at = findTrack(ext, "audio/")
                    if (at >= 0) {
                        ext.selectTrack(at)
                        ext.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        while (true) {
                            buffer.clear()
                            val size = ext.readSampleData(buffer, 0)
                            if (size < 0) break
                            info.offset = 0
                            info.size = size
                            info.presentationTimeUs = ext.sampleTime + timeOffsetUs
                            info.flags = if (ext.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                                MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                            muxer.writeSampleData(outAudio, buffer, info)
                            ext.advance()
                        }
                        ext.unselectTrack(at)
                    }
                }
                timeOffsetUs += if (inDurationUs > 0) inDurationUs else 0L
            }
            return out.exists() && out.length() > 0
        } catch (e: Exception) {
            out.delete()
            return false
        } finally {
            try { muxer?.stop() } catch (e: Exception) {}
            try { muxer?.release() } catch (e: Exception) {}
            extractors.forEach { try { it.release() } catch (e: Exception) {} }
        }
    }

    private fun findTrack(ext: MediaExtractor, prefix: String): Int {
        for (i in 0 until ext.trackCount) {
            val mime = ext.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(prefix)) return i
        }
        return -1
    }

    fun durationUs(file: File): Long {
        return try {
            val r = MediaMetadataRetriever()
            r.setDataSource(file.absolutePath)
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            r.release()
            ms * 1000L
        } catch (e: Exception) { 0L }
    }

    fun durationSeconds(file: File): Int = (durationUs(file) / 1_000_000L).toInt()
}
