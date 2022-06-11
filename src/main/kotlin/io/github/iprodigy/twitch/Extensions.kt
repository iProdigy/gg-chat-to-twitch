package io.github.iprodigy.twitch

import com.netflix.hystrix.HystrixCommand

fun <T> HystrixCommand<T>.executeOrNull(
    exceptionHandler: ((Exception) -> Unit)? = {
        Bot.log.warn("Hystrix command execution failed!", it)
    }
): T? = try {
    this.execute()
} catch (e: Exception) {
    exceptionHandler?.invoke(e)
    null
}
