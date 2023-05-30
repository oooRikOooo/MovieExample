package com.example.mediaexample.di

import com.example.mediaexample.manager.VideoManager
import org.koin.dsl.module

val managerModule = module {
    single { VideoManager() }
}