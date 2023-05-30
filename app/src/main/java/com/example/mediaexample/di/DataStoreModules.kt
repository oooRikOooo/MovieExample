package com.example.mediaexample.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.mediaexample.data.FilePreferencesRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "files_storage")

val dataStoreModule = module {
    factory { FilePreferencesRepository(androidContext().dataStore) }

    single {

    }
}