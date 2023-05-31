package com.example.mediaexample.di

import com.example.mediaexample.manager.DecodeVideoManager
import com.example.mediaexample.manager.EncodeVideoManager
import org.koin.dsl.module

val managerModule = module {
    factory { DecodeVideoManager() }
    factory { EncodeVideoManager() }
}