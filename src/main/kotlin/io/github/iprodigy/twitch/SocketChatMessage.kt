package io.github.iprodigy.twitch

private const val BOT_FEATURE = "bot"
private const val SUB_FEATURE = "subscriber"
private const val PROTECTED_FEATURE = "protected"
private const val MOD_FEATURE = "moderator"
private const val ADMIN_FEATURE = "admin"

data class SocketChatMessage(
    val data: String, // message
    val nick: String? = null, // username
    val sub: String? = null, // "1" for sub
    val features: List<String>? = null,
    val pronouns: String? = null,
    val timestamp: Long?,
//    val nodes: Map<String, Any>
) {
    fun isBot(): Boolean = features?.contains(BOT_FEATURE) == true
    fun isSub(): Boolean = "1" == sub || "true" == sub || features?.contains(SUB_FEATURE) == true
    fun isProtected(): Boolean = features?.contains(PROTECTED_FEATURE) == true
    fun isMod(): Boolean = features?.contains(MOD_FEATURE) == true
    fun isAdmin(): Boolean = features?.contains(ADMIN_FEATURE) == true
    fun isPrivileged() = features != null && features.any {
        it == BOT_FEATURE || it == SUB_FEATURE || it == PROTECTED_FEATURE || it == MOD_FEATURE || it == ADMIN_FEATURE
    }
}

enum class MessageType {
    AWARE,
    BAN,
    UNBAN,
    MUTE,
    UNMUTE,
    NAMES,
    JOIN,
    QUIT,
    REFRESH,
    MSG,
    BROADCAST,
    UNKNOWN
}

val messageTypesByName = MessageType.values().associateBy { it.name }
