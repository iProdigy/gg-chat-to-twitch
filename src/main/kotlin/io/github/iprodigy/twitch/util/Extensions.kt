package io.github.iprodigy.twitch.util

import com.netflix.hystrix.HystrixCommand
import io.github.iprodigy.twitch.Bot
import java.util.NoSuchElementException
import java.util.Queue

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
fun <T : Any?, C : MutableCollection<T>> Queue<T>.drainTo(supplier: () -> C): C {
    val collection = supplier()
    require(collection != this) { "Cannot drain collection to itself!" }

    while (isNotEmpty()) {
        val e = try {
            remove()
        } catch (ignored: NoSuchElementException) {
            break
        }
        collection += e
    }

    return collection
}

fun <T> Iterable<T>.ensureSet(): Set<T> = if (this is Set) this else this.toSet()
