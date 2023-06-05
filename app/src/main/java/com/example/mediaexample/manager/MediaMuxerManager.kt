package com.example.mediaexample.manager

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

class MediaMuxerManager(private val context: Context) {

    private var mediaMuxer: MediaMuxer? = null

    private var isMediaMuxerStarted = false

    private var videoTrackIndex: Int = 0
    private var audioTrackIndex: Int = 0

    private lateinit var lastAudioData: ByteArray
    private lateinit var lastVideoData: ByteArray

    private var currentAudioPresentationTimeUs = 0L
    private var currentVideoPresentationTimeUs = 0L

    private var defaultVideoTime = 0L
    private var defaultAudioTime = 0L

    private var isFirstVideoFrameReceived = false
    private var isFirstAudioFrameReceived = false

    var videoFormat: MediaFormat? = null
    var audioFormat: MediaFormat? = null

    fun processFrame(info: BufferInfo, outData: ByteArray, frameType: FrameType) {
        when (frameType) {
            FrameType.AUDIO -> {
                isFirstAudioFrameReceived = true
                processAudioBytes(info, outData)
            }
            FrameType.VIDEO -> {
                isFirstVideoFrameReceived = true
                processVideoBytes(info, outData)
            }
        }

        if (mediaMuxer == null &&
            isFirstVideoFrameReceived &&
            isFirstAudioFrameReceived &&
            audioFormat != null &&
            videoFormat != null
        ) {
            setupMediaMuxer()
        }
    }

    private fun processVideoBytes(info: BufferInfo, outData: ByteArray) {
        if (mediaMuxer == null && !isMediaMuxerStarted) return

        if (defaultVideoTime == 0L) {
            defaultVideoTime = info.presentationTimeUs
        }

        val buffer = ByteBuffer.wrap(outData)
        info.presentationTimeUs = info.presentationTimeUs - defaultVideoTime
        lastVideoData = outData

        if (info.presentationTimeUs > currentVideoPresentationTimeUs) {
            mediaMuxer!!.writeSampleData(videoTrackIndex, buffer, info)
            currentVideoPresentationTimeUs = info.presentationTimeUs
        }
    }

    private fun processAudioBytes(info: BufferInfo, outData: ByteArray) {
        if (mediaMuxer == null && !isMediaMuxerStarted) return

        if (defaultAudioTime == 0L) {
            defaultAudioTime = info.presentationTimeUs
        }

        val buffer = ByteBuffer.wrap(outData)

        lastAudioData = outData
        info.presentationTimeUs = info.presentationTimeUs - defaultAudioTime
        info.flags = 0

        if (info.presentationTimeUs >= 0 && info.presentationTimeUs > currentAudioPresentationTimeUs) {
            mediaMuxer!!.writeSampleData(audioTrackIndex, buffer, info)
            currentAudioPresentationTimeUs = info.presentationTimeUs
        }
    }

    private fun setupMediaMuxer() {

        val dir = File(context.filesDir, "my_dir")

        if (!dir.exists()) dir.mkdir()

        val outputFile = File(dir, "${System.currentTimeMillis()}.mp4")

        mediaMuxer =
            MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        videoTrackIndex = mediaMuxer!!.addTrack(videoFormat!!)
        audioTrackIndex = mediaMuxer!!.addTrack(audioFormat!!)

        mediaMuxer?.start()

        isMediaMuxerStarted = true

    }

    private fun finishRecordToFile() {
        val bufferVideo = ByteBuffer.wrap(lastVideoData)
        val infoVideo = BufferInfo()
        infoVideo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM
        infoVideo.size = 0
        mediaMuxer!!.writeSampleData(videoTrackIndex, bufferVideo, infoVideo)

        val bufferAudio = ByteBuffer.wrap(lastAudioData)
        val infoAudio = BufferInfo()
        infoAudio.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM
        infoAudio.size = 0
        mediaMuxer!!.writeSampleData(audioTrackIndex, bufferAudio, infoAudio)
    }

    fun releaseMediaMuxer() {
        finishRecordToFile()

        audioTrackIndex = 0
        videoTrackIndex = 0

        currentAudioPresentationTimeUs = 0L
        currentVideoPresentationTimeUs = 0L

        defaultVideoTime = 0L

        isMediaMuxerStarted = false
        mediaMuxer?.stop()
        mediaMuxer?.release()
        mediaMuxer = null
    }

    sealed class FrameType {
        object VIDEO : FrameType()
        object AUDIO : FrameType()
    }
}