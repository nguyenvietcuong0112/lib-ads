package com.mobi.libraryads.ads.native_ads.model

enum class AdLoadState {
    NOT_LOADED,   // Chưa load lần nào hoặc đã consume
    LOADING,      // Đang trong quá trình load
    LOADED,       // Đã load thành công, ad sẵn sàng
    ERROR         // Load thất bại
}
