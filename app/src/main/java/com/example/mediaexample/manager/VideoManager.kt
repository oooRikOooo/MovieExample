package com.example.mediaexample.manager

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.FileOutputStream

class VideoManager {
    private lateinit var extractor: MediaExtractor
    private lateinit var decoder: MediaCodec
    private lateinit var bufferInfo: MediaCodec.BufferInfo

    fun decodeMP4(filePath: String, outputFilePath: String) {
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(filePath)

            val trackIndex = selectTrack(extractor)
            if (trackIndex < 0) {
                throw RuntimeException("No video track found in the MP4 file")
            }

            extractor.selectTrack(trackIndex)
            val format: MediaFormat = extractor.getTrackFormat(trackIndex)

            decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            Log.d("riko", "mimeType MediaCodec: ${format.getString(MediaFormat.KEY_MIME)}")
            decoder.configure(format, null, null, 0)
            decoder.start()


            bufferInfo = MediaCodec.BufferInfo()

            val outputStream = FileOutputStream(outputFilePath)

            var isEOS = false
            while (!isEOS) {
                val inputIndex = decoder.dequeueInputBuffer(0)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        isEOS = true
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        Log.d("riko", "sampleTime: ${extractor.sampleTime}")
                        Log.d("riko", "sampleSize: ${extractor.sampleSize}")
                        extractor.advance()
                    }
                }

                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 0)
                if (outputIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)!!
                    val buffer = ByteArray(bufferInfo.size)
                    outputBuffer.get(buffer)

                    outputStream.write(buffer)
                    decoder.releaseOutputBuffer(outputIndex, false)
                }

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isEOS = true
                }
            }

            decoder.stop()
            decoder.release()
            extractor.release()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun selectTrack(extractor: MediaExtractor): Int {
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            Log.d("riko", "mimeType: $mime")
            if (mime?.startsWith("video/") == true) {
                return i
            }
        }
        return -1
    }
}