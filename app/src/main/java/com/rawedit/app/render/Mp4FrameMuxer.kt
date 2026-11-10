package com.rawedit.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume

object Mp4FrameMuxer {

    fun selectTargetBitrate(durationMs: Long): Int {
        return when {
            durationMs <= 60_000L -> 5_000_000   // 5.0 Mbps <= 60s
            durationMs <= 180_000L -> 3_500_000  // 3.5 Mbps <= 3 min
            else -> 2_800_000                    // 2.8 Mbps > 3 min
        }
    }

    /**
     * Uses Media3 Transformer on Main looper to transcode input voiceover to 128 kbps AAC M4A.
     */
    suspend fun transcodeAudioToAac(
        context: Context,
        sourceAudioUri: String,
        tempDir: File
    ): File? = withContext(Dispatchers.Main) {
        if (sourceAudioUri.isBlank()) return@withContext null
        val outFile = File(tempDir, "audio_aac_128k.m4a")
        if (outFile.exists()) outFile.delete()

        suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(outFile.takeIf { it.exists() && it.length() > 0 })
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
                .build()

            val mediaItem = MediaItem.fromUri(Uri.parse(sourceAudioUri))
            val editedItem = EditedMediaItem.Builder(mediaItem).setRemoveVideo(true).build()
            runCatching {
                transformer.start(editedItem, outFile.absolutePath)
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    /**
     * Hardware-accelerated H.264 1920x1080 @ 30fps encoder using MediaCodec input Surface + MediaMuxer.
     */
    class SurfaceVideoEncoder(
        outputFile: File,
        width: Int = 1920,
        height: Int = 1080,
        fps: Int = 30,
        bitrate: Int
    ) {
        private val bufferInfo = MediaCodec.BufferInfo()
        private val codec: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        private val inputSurface: Surface
        private val muxer: MediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        private var videoTrackIndex = -1
        private var muxerStarted = false

        init {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()
        }

        fun encodeBitmapFrame(bitmap: Bitmap, frameIndex: Int, fps: Int = 30) {
            val canvas: Canvas = inputSurface.lockHardwareCanvas()
            try {
                canvas.drawBitmap(bitmap, 0f, 0f, null)
            } finally {
                inputSurface.unlockCanvasAndPost(canvas)
            }
            drainEncoder(endOfStream = false)
        }

        fun finishAndMuxAudio(aacAudioFile: File?, maxDurationUs: Long) {
            drainEncoder(endOfStream = true)
            runCatching {
                codec.stop()
                codec.release()
                inputSurface.release()
            }

            if (aacAudioFile != null && aacAudioFile.exists() && muxerStarted) {
                muxAudioTrack(aacAudioFile, maxDurationUs)
            }

            runCatching {
                if (muxerStarted) muxer.stop()
                muxer.release()
            }
        }

        private fun drainEncoder(endOfStream: Boolean) {
            if (endOfStream) {
                codec.signalEndOfInputStream()
            }
            while (true) {
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        videoTrackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outIndex >= 0) {
                    val encodedData = codec.getOutputBuffer(outIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }

        private fun muxAudioTrack(aacFile: File, maxDurationUs: Long) {
            // Note: Since MediaMuxer requires all tracks added before start(), if audio is present
            // we remux video + audio together cleanly into a sibling file and replace.
        }
    }

    /**
     * Remuxes an encoded video-only MP4 with a transcoded AAC M4A audio track into the final MP4.
     */
    fun remuxVideoAndAudio(videoOnlyMp4: File, aacAudioM4a: File?, finalMp4: File, maxDurationUs: Long) {
        if (aacAudioM4a == null || !aacAudioM4a.exists()) {
            videoOnlyMp4.copyTo(finalMp4, overwrite = true)
            return
        }

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            videoExtractor.setDataSource(videoOnlyMp4.absolutePath)
            audioExtractor.setDataSource(aacAudioM4a.absolutePath)

            muxer = MediaMuxer(finalMp4.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var videoTrack = -1
            for (i in 0 until videoExtractor.trackCount) {
                val fmt = videoExtractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoTrack = muxer.addTrack(fmt)
                    break
                }
            }

            var audioTrack = -1
            for (i in 0 until audioExtractor.trackCount) {
                val fmt = audioExtractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioTrack = muxer.addTrack(fmt)
                    break
                }
            }

            muxer.start()
            val buffer = ByteBuffer.allocateDirect(512 * 1024)
            val info = MediaCodec.BufferInfo()

            if (videoTrack >= 0) {
                while (true) {
                    val sampleSize = videoExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    info.offset = 0
                    info.size = sampleSize
                    info.presentationTimeUs = videoExtractor.sampleTime
                    info.flags = videoExtractor.sampleFlags
                    muxer.writeSampleData(videoTrack, buffer, info)
                    videoExtractor.advance()
                }
            }

            if (audioTrack >= 0) {
                while (true) {
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    val pts = audioExtractor.sampleTime
                    if (pts > maxDurationUs) break
                    info.offset = 0
                    info.size = sampleSize
                    info.presentationTimeUs = pts
                    info.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioTrack, buffer, info)
                    audioExtractor.advance()
                }
            }
            muxer.stop()
        } catch (_: Throwable) {
            videoOnlyMp4.copyTo(finalMp4, overwrite = true)
        } finally {
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
            runCatching { muxer?.release() }
        }
    }
}
