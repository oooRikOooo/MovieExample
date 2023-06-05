package com.example.mediaexample.manager

import android.annotation.SuppressLint
import android.media.*
import android.util.Log
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer


class EncodeVideoManager(private val mediaMuxerManager: MediaMuxerManager) {

    var mediaCodec: MediaCodec? = null
    var encoderSurface: Surface? = null
    var outPutByteBuffer: ByteBuffer? = null

    fun setupMediaCodec() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val width = 1920
        val height = 1080
        val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        val videoBitrate = 50_000_000
        val videoFrameRate = 30
        val iFrameInterval = 1

        val format = MediaFormat.createVideoFormat("video/avc", width, height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)

        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        encoderSurface = mediaCodec?.createInputSurface()

        mediaCodec?.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                outPutByteBuffer = mediaCodec?.getOutputBuffer(index)
                val outData = ByteArray(info.size)
                outPutByteBuffer?.get(outData)

                if (mediaMuxerManager.videoFormat == null)
                    mediaMuxerManager.videoFormat = mediaCodec?.outputFormat

                mediaMuxerManager.processFrame(info, outData, MediaMuxerManager.FrameType.VIDEO)

                mediaCodec?.releaseOutputBuffer(index, false)
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Log.d(TAG, "ERROR: ${e.message}")
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.d(TAG, "format changed: $format")
            }

        })

        mediaCodec?.start()

    }

    fun stopVideoEncoding() {
        mediaCodec?.stop()
        mediaCodec?.release()
        encoderSurface?.release()
    }

    companion object {
        private val TAG = this::class.java.simpleName
    }
}