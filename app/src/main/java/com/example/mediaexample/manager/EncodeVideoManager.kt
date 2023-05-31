package com.example.mediaexample.manager

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer


class EncodeVideoManager {

    var mediaCodec: MediaCodec? = null
    var encoderSurface: Surface? = null
    var outPutByteBuffer: ByteBuffer? = null

    fun setupMediaCodec() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
        } catch (e: Exception) {
            Log.i("riko", e.message.toString())
        }

        val width = 1920
        val height = 1080
        val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        val videoBitrate = 500_000
        val videoFrameRate = 30
        val iFrameInterval = 2

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
                Log.d("riko", "timeInfo: ${info.presentationTimeUs}")
                Log.d("riko", "outData: ${outData.size}")

                mediaCodec?.releaseOutputBuffer(index, false)
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Log.d("riko", "ERROR: ${e.message}")
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.d("riko", "format changed: $format")
            }

        })


    }

    fun stopEncoding() {
        mediaCodec?.stop()
        mediaCodec?.release()
        encoderSurface?.release()
    }
}