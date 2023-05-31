package com.example.mediaexample.manager

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import kotlinx.coroutines.delay


class VideoManager {

    suspend fun renderVideoFrames(filePath: String, surface: Surface) {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)

        val trackIndex = selectTrack(extractor)
        if (trackIndex < 0) {
            throw RuntimeException("No video track found in the MP4 file")
        }

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)

        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, surface, null, 0)
        codec.start()


        val info = MediaCodec.BufferInfo()

        var isEOS = false
        while (!Thread.interrupted()) {
            if (!isEOS) {
                val inputIndex = codec.dequeueInputBuffer(0)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                    if (sampleSize >= 0 && extractor.advance()) {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            sampleSize,
                            presentationTimeUs,
                            MediaCodec.BUFFER_FLAG_KEY_FRAME
                        )
                    } else {
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        isEOS = true
                    }
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(info, 0)
            if (outputIndex >= 0) {
                codec.releaseOutputBuffer(outputIndex, true)
                delay(30)
            }
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break
            }
        }

        codec.stop()
        codec.release()
        extractor.release()
    }

    private fun selectTrack(extractor: MediaExtractor): Int {
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime!!.startsWith("video/")) {
                return i
            }
        }
        return -1
    }


    companion object {
        private const val MIME_TYPE = "video/mp4"
    }
}