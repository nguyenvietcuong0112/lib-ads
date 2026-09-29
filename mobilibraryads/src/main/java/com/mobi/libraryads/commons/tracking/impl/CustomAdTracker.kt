package com.mobi.libraryads.commons.tracking.impl

import com.mobi.libraryads.commons.tracking.IAdTracker
import com.mobi.libraryads.commons.tracking.model.AdRevenueData

/**
 * Custom tracker cho phép app tùy biến bắn doanh thu về bất kỳ backend/analytics riêng nào.
 */
class CustomAdTracker(
    override val trackerName: String = "Custom",
    private val onRevenue: (AdRevenueData) -> Unit
) : IAdTracker {
    override fun trackAdRevenue(data: AdRevenueData) {
        onRevenue(data)
    }
}
