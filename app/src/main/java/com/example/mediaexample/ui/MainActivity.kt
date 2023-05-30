package com.example.mediaexample.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mediaexample.databinding.ActivityMainBinding
import com.example.mediaexample.manager.VideoManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModel()
    private val videoManager: VideoManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(
                Manifest.permission.READ_MEDIA_VIDEO, READ_MEDIA_VIDEO
            )
        } else {
            checkPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                READ_EXTERNAL_STORAGE
            )
        }

        checkPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            checkPermission(
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                MANAGE_EXTERNAL_STORAGE
            )
        }

        setupListeners()
    }

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        }
    }

    private fun setupListeners() {
        binding.apply {
            button.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "video/*"

                resultLauncherPickVideoFile.launch(Intent.createChooser(intent, "Choose"))
            }

            buttonDecodeVideo.setOnClickListener {

                val pickedFile = File(viewModel.pickedFileUri.value)
                val pickedFileFullUri = Uri.parse(viewModel.pickedFileFullUri.value)

                val fileDescriptor =
                    contentResolver.openFileDescriptor(pickedFileFullUri, "r")

                val outputDecodedVideoFile = File(
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        ?.absolutePath + "/decodedVideo.txt"
                )


                Log.d("riko", outputDecodedVideoFile.absolutePath)

                if (!outputDecodedVideoFile.exists()) {
                    outputDecodedVideoFile.createNewFile()
                }

                Log.d("riko", "encodedPath: ${pickedFileFullUri}")

                videoManager.decodeMP4(
                    pickedFile.absolutePath,
                    outputDecodedVideoFile.absolutePath
                )

                fileDescriptor?.close()
            }
        }
    }

    private fun getRealPathFromURI(uri: Uri?): String {
        if (uri == null) return ""

        var filePath = ""
        if (contentResolver != null) {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor?.let {
                it.moveToFirst()
                val index: Int = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                filePath = it.getString(index)
                it.close()
            }
        }
        return filePath
    }

    private val resultLauncherPickVideoFile =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uriString = getRealPathFromURI(result.data?.data)
                viewModel.savePickedFileUri(uriString, result.data?.data.toString())
                Log.d("riko", "fileUri: ${result.data?.data}")
            }
        }

    companion object {
        private const val READ_MEDIA_VIDEO = 100
        private const val READ_EXTERNAL_STORAGE = 101
        private const val WRITE_EXTERNAL_STORAGE = 102
        private const val MANAGE_EXTERNAL_STORAGE = 103
    }
}