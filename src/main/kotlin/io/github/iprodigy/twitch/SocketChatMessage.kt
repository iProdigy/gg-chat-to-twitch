package io.github.iprodigy.twitch

data class SocketChatMessage(
    val nick: String, // username
    val data: String, // message
    val sub: String?, // "1" for sub
//    val pronouns: String,
//    val features: List<String>,
//    val timestamp: Long,
//    val nodes: Map<String, Any>
)
