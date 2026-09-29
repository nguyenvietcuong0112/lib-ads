package com.mobi.libraryads.commons.tracking.model

/**
 * Model chứa dữ liệu doanh thu quảng cáo chuẩn hóa độc lập với bất kỳ SDK nào
 */
data class AdRevenueData(
    val valueMicros: Long,
    val currencyCode: String,
    val adSourceName: String,
    val adUnitId: String = "",
    val adFormat: String = "",
    val precisionType: Int = 0
) {
    /** Doanh thu quy đổi ra đơn vị tiền tệ tiêu chuẩn (USD, EUR, v.v.) */
    val revenue: Double get() = valueMicros / 1_000_000.0
}
