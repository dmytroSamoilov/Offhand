package com.dmytrosamoilov.offhand.core.common

private const val BYTES_PER_GB = 1024f * 1024f * 1024f

fun formatGigabytes(bytes: Long): String = DecimalFormatter.oneDecimal(bytes / BYTES_PER_GB)
