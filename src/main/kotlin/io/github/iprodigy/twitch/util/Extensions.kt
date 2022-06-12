package io.github.iprodigy.twitch.util

import com.netflix.hystrix.HystrixCommand
import io.github.iprodigy.twitch.Bot
import java.util.*
import kotlin.collections.ArrayDeque

// Hystrix
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

// Collections
fun <T : Any, C : MutableCollection<T>> Queue<T>.drain(supplier: () -> C): C {
    val collection = supplier()
    require(collection != this) { "Cannot drain collection to itself!" }

    while (true) {
        val e = this.poll() ?: break
        collection += e
    }

    return collection
}

fun <T : Any> Queue<T>.drain(): Collection<T> = this.drain { ArrayDeque() }

fun <T> Iterable<T>.ensureSet(): Set<T> = if (this is Set) this else this.toSet()
