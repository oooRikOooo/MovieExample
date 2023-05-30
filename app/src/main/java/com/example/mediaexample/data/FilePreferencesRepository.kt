package com.example.mediaexample.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mediaexample.data.model.PickedFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FilePreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val PICKED_FILE_URI = stringPreferencesKey("picked_file_uri")
        val PICKED_FILE_FULL_URI = stringPreferencesKey("picked_file_full_uri")
    }

    suspend fun savePickedFileUri(pickedFile: PickedFile) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PICKED_FILE_URI] = pickedFile.uri
            preferences[PreferencesKeys.PICKED_FILE_FULL_URI] = pickedFile.fullPathUri
        }
    }

    val getPickedFileUri: Flow<PickedFile> = dataStore.data
        .map { preferences ->
            PickedFile(
                uri = preferences[PreferencesKeys.PICKED_FILE_URI] ?: "",
                fullPathUri = preferences[PreferencesKeys.PICKED_FILE_FULL_URI] ?: ""
            )
        }

}