package com.example.mediaexample.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mediaexample.databinding.ActivityCameraBinding
import com.example.mediaexample.manager.EncodeAudioManager
import com.example.mediaexample.manager.EncodeVideoManager
import com.example.mediaexample.manager.MediaMuxerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File


class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null

    private var myCameras: ArrayList<CameraService>? = null
    private var mCameraManager: CameraManager? = null

    private val cameraBack = 0

    private val encodeVideoManager: EncodeVideoManager by inject()
    private val encodeAudioManager: EncodeAudioManager by inject()
    private val mediaMuxerManager: MediaMuxerManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setupListeners()

        setupCamera()
    }

    override fun onPause() {
        if (myCameras?.get(cameraBack)?.isOpen == true) {
            myCameras?.get(cameraBack)?.closeCamera()
        }

        stopBackgroundThread();
        super.onPause()

    }

    override fun onResume() {
        super.onResume()

        startBackgroundThread()
    }

    private fun setupCamera() {
        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            myCameras = arrayListOf()

            for (cameraID in mCameraManager!!.cameraIdList) {
                myCameras?.add(CameraService(mCameraManager!!, cameraID))
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace();
        }
    }

    private fun setupListeners() {
        binding.apply {
            imageViewPlay.setOnClickListener {

                lifecycleScope.launch(Dispatchers.IO) {

                }

                encodeAudioManager.startAudioEncoding()

                encodeVideoManager.setupMediaCodec()


                imageViewPlay.visibility = View.GONE
                imageViewStop.visibility = View.VISIBLE

                myCameras?.get(cameraBack)?.openCamera()

//                mediaMuxerManager.setupMediaMuxer(
//                    outputFile,
//                    encodeVideoManager.mediaCodec!!.outputFormat,
//                    encodeAudioManager.audioMediaCodec!!.outputFormat
//                )
            }

            imageViewStop.setOnClickListener {
                imageViewPlay.visibility = View.VISIBLE
                imageViewStop.visibility = View.GONE

                myCameras?.get(cameraBack)?.stopStreaming()

            }
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread?.let {
            it.start()
            mBackgroundHandler = Handler(it.looper)
        }

    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private inner class CameraService(cameraManager: CameraManager, cameraID: String) {
        private val mCameraID: String
        private var mCameraDevice: CameraDevice? = null
        private var mCaptureSession: CameraCaptureSession? = null

        private val mCameraCallback: CameraDevice.StateCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    mCameraDevice = camera
                    Log.d(TAG, "Open camera  with id:" + mCameraDevice!!.id)
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    mCameraDevice!!.close()
                    Log.d(TAG, "disconnect camera  with id:" + mCameraDevice!!.id)
                    mCameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.d(TAG, "error! camera id:" + camera.id + " error:" + error)
                }
            }

        init {
            mCameraManager = cameraManager
            mCameraID = cameraID
        }

        private fun createCameraPreviewSession() {
            val texture: SurfaceTexture? = binding.textureView.surfaceTexture
            texture?.setDefaultBufferSize(1920, 1080)
            val surface = Surface(texture)
            try {
                val builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                builder.addTarget(surface)
                encodeVideoManager.encoderSurface?.let {
                    builder.addTarget(it)
                }
                mCameraDevice!!.createCaptureSession(
                    listOf(surface, encodeVideoManager.encoderSurface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            mCaptureSession = session
                            try {
                                mCaptureSession!!.setRepeatingRequest(
                                    builder.build(),
                                    null,
                                    mBackgroundHandler
                                )
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    }, mBackgroundHandler
                )
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        val isOpen: Boolean
            get() = mCameraDevice != null

        fun openCamera() {
            try {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    mCameraManager?.openCamera(mCameraID, mCameraCallback, mBackgroundHandler)
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        fun closeCamera() {
            if (mCameraDevice != null) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
        }

        fun stopStreaming() {
            if ((mCameraDevice != null) and (encodeVideoManager.mediaCodec != null)) {
                try {
                    mCaptureSession?.stopRepeating()
                    mCaptureSession?.abortCaptures()
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }

                encodeAudioManager.stopEncoding()

                encodeVideoManager.stopVideoEncoding()

                mediaMuxerManager.releaseMediaMuxer()

                closeCamera()
            }

        }
    }

    companion object {
        private val TAG = this::class.java.simpleName
    }

}
