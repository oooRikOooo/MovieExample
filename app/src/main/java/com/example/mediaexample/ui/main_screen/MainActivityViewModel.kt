package com.example.mediaexample.ui.main_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediaexample.data.FilePreferencesRepository
import com.example.mediaexample.data.model.PickedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val filePreferencesRepository: FilePreferencesRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            filePreferencesRepository.getPickedFileUri.collect {
                pickedFileUri.value = it.uri
                pickedFileFullUri.value = it.fullPathUri
            }
        }
    }

    val pickedFileUri = MutableStateFlow("")
    val pickedFileFullUri = MutableStateFlow("")


    fun savePickedFileUri(uri: String, fullPathUri: String) {
        viewModelScope.launch {
            filePreferencesRepository.savePickedFileUri(
                pickedFile = PickedFile(uri, fullPathUri)
            )
        }
    }
}