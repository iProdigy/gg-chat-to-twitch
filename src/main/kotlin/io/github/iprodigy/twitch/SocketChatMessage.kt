package io.github.iprodigy.twitch

data class SocketChatMessage(
    val nick: String, // username
    val data: String, // message
    val sub: String? = null, // "1" for sub
    val features: List<String>? = null,
//    val pronouns: String,
//    val timestamp: Long,
//    val nodes: Map<String, Any>
)
