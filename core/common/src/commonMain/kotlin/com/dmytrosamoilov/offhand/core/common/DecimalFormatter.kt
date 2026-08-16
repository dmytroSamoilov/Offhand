package com.dmytrosamoilov.offhand.core.common

object DecimalFormatter {

    fun oneDecimal(value: Float): String {
        val tenths = (value * TENTHS_PER_UNIT + ROUNDING_OFFSET).toInt()
        return "${tenths / TENTHS_PER_UNIT}.${tenths % TENTHS_PER_UNIT}"
    }

    private const val TENTHS_PER_UNIT = 10
    private const val ROUNDING_OFFSET = 0.5f
}
