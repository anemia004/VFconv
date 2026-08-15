package com.vfconv.app

sealed class ConversionState {
    object Idle : ConversionState()
    object Running : ConversionState()
    object Success : ConversionState()
    data class Error(val message: String?) : ConversionState()
}
