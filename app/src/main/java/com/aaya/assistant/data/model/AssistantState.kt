package com.aaya.assistant.data.model

data class ApiDiagnostics(
    val isConnected: Boolean = false,
    val totalRequestsSent: Int = 0,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val lastLatencyMs: Long = 0L,
    val lastErrorMessage: String? = null,
    val lastResponseSummary: String? = null,
    val keySource: String = "Configured" // "Google AI Studio" or "Bearer Token"
)

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class AssistantState(
    val voiceState: VoiceState = VoiceState.IDLE,
    val streamingTranscript: String = "",
    val finalResponseText: String = "",
    val executedActionDescription: String = "",
    val confidenceScore: Float = 0.0f
)
