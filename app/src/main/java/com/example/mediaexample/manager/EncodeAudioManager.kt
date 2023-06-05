package com.example.mediaexample.manager

import android.annotation.SuppressLint
import android.media.*
import android.util.Log
import java.io.IOException

class EncodeAudioManager(private val mediaMuxerManager: MediaMuxerManager) {

    var audioMediaCodec: MediaCodec? = null
    private var audioFormat: MediaFormat? = null
    private var isEncoding = false

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    private val SAMPLE_RATE = 48000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val DEFAULT_BUFFER_SIZE = 4096


    fun startAudioEncoding() {

        val codecName = MediaFormat.MIMETYPE_AUDIO_AAC
        audioMediaCodec = MediaCodec.createEncoderByType(codecName)
        audioFormat = MediaFormat.createAudioFormat(codecName, 48000, 1)

        audioFormat?.setInteger(MediaFormat.KEY_BIT_RATE, 32000)
        audioFormat?.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )

        audioMediaCodec?.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioMediaCodec?.start()

        startRecording()

        recordingThread = Thread(AudioEncoderRunnable())

        recordingThread?.start()
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecorder", "Failed to initialize AudioRecord")
            return
        }

        audioRecord?.startRecording()
        isRecording = true
    }

    private fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        try {
            recordingThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    fun stopEncoding() {
        isEncoding = false
    }

    inner class AudioEncoderRunnable : Runnable {
        private val TIMEOUT_US = 10000L

        override fun run() {
            try {

                var inputBufferIndex: Int
                var outputBufferIndex: Int
                val bufferInfo = MediaCodec.BufferInfo()

                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioRecord?.startRecording()

                isEncoding = true

                while (isEncoding) {
                    if (audioMediaCodec == null && audioRecord == null) {
                        break
                    }

                    inputBufferIndex = audioMediaCodec!!.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = audioMediaCodec!!.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()

                        val readSize = audioRecord!!.read(inputBuffer!!, bufferSize)
                        if (readSize > 0) {
                            audioMediaCodec!!.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                readSize,
                                System.currentTimeMillis(),
                                0
                            )
                        }
                    }

                    outputBufferIndex =
                        audioMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    while (outputBufferIndex >= 0) {
                        val outputBuffer = audioMediaCodec!!.getOutputBuffer(outputBufferIndex)
                        val outData = ByteArray(bufferInfo.size)
                        outputBuffer?.get(outData)

                        if (mediaMuxerManager.audioFormat == null)
                            mediaMuxerManager.audioFormat = audioMediaCodec?.outputFormat

                        mediaMuxerManager.processFrame(bufferInfo, outData, MediaMuxerManager.FrameType.AUDIO)

                        audioMediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex =
                            audioMediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    }
                }

                stopRecording()
                audioMediaCodec?.stop()
                audioMediaCodec?.release()
            } catch (e: IOException) {
                Log.e("riko", "Error encoding audio: ${e.message}")
            }
        }
    }


}