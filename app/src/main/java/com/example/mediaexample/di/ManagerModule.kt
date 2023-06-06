package com.example.mediaexample.di

import com.example.mediaexample.manager.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val managerModule = module {
    factory { DecodeVideoManager() }
    factory { EncodeVideoManager(get()) }
    factory { EncodeAudioManager(get()) }
    factory { MediaMuxerManager(androidContext()) }
}