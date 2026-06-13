package com.ptithcm.ptitmeet.live

data class LiveParticipantState(
    val identity: String,
    val displayName: String,
    val hasVideo: Boolean,
    val isMicOn: Boolean,
    val isSpeaking: Boolean,
    val isLocal: Boolean,
)
