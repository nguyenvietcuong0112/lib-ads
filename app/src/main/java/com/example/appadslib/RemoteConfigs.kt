package com.example.appadslib

import com.mobi.libraryads.commons.remote.KonfigModel
import com.mobi.libraryads.commons.remote.konfig

object RemoteConfigs : KonfigModel {

    val native_home by konfig("native_home", true)
    val native_all by konfig("native_all", false)

}