package com.mobi.libraryads.ads.native_ads.model

enum class LoadStrategy {
    SEQUENTIAL,   // Load idHigh trước, nếu fail thì load id
    PARALLEL      // Load cả idHigh và id song song, ưu tiên high
}
