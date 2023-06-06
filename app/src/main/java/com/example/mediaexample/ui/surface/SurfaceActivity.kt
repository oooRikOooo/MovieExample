package com.example.mediaexample.ui.surface

import android.os.Bundle
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mediaexample.databinding.ActivitySurfaceBinding
import com.example.mediaexample.manager.DecodeVideoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SurfaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurfaceBinding

    private val manager: DecodeVideoManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySurfaceBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val bundle = intent.extras
        val pickedFileAbsolutePath = bundle?.getString("filePath")

        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                lifecycleScope.launch(Dispatchers.IO) {
                    manager.renderVideoFrames(pickedFileAbsolutePath ?: "", holder.surface)
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {}

        })


    }

    override fun onDestroy() {
        super.onDestroy()
        manager.codec.stop()
        manager.codec.release()
        manager.extractor.release()
    }
}