package com.example.mediaexample

import android.app.Application
import com.example.mediaexample.di.dataStoreModule
import com.example.mediaexample.di.managerModule
import com.example.mediaexample.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()

            androidContext(this@MainApplication)

            modules(dataStoreModule, viewModelModule, managerModule)
        }
    }
}