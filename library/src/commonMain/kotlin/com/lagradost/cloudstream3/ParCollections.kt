package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.mvvm.logError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val DEFAULT_CONCURRENCY = 4

@Deprecated("Use amap with concurrency parameter", level = DeprecationLevel.HIDDEN)
@Throws(CancellationException::class)
suspend fun <K, V, R> Map<out K, V>.amap(f: suspend (Map.Entry<K, V>) -> R): List<R> =
    amap(DEFAULT_CONCURRENCY, f)

/**
 * Short for "Asynchronous Map", runs on all values concurrently,
 * this means that if you are not doing networking, you should use a regular map
 */
@Throws(CancellationException::class)
suspend fun <K, V, R> Map<out K, V>.amap(
    concurrency: Int = DEFAULT_CONCURRENCY,
    f: suspend (Map.Entry<K, V>) -> R,
): List<R> = coroutineScope {
    val semaphore = Semaphore(concurrency)
    map { entry ->
        async { semaphore.withPermit { f(entry) } }
    }.awaitAll()
}

/**
 * Short for "Asynchronous Parallel Map", but is not really parallel, only concurrent.
 */
@Deprecated(
    "This blocks with runBlocking, and should not be used inside a suspended context",
    replaceWith = ReplaceWith("amap(f)", "com.lagradost.cloudstream3.amap"),
    level = DeprecationLevel.ERROR
)
@Throws(CancellationException::class)
fun <K, V, R> Map<out K, V>.apmap(f: suspend (Map.Entry<K, V>) -> R): List<R> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

@Deprecated("Use amap with concurrency parameter", level = DeprecationLevel.HIDDEN)
@Throws(CancellationException::class)
suspend fun <A, B> List<A>.amap(f: suspend (A) -> B): List<B> =
    amap(DEFAULT_CONCURRENCY, f)

/**
 * Short for "Asynchronous Map", runs on all values concurrently,
 * this means that if you are not doing networking, you should use a regular map
 */
@Throws(CancellationException::class)
suspend fun <A, B> List<A>.amap(
    concurrency: Int = DEFAULT_CONCURRENCY,
    f: suspend (A) -> B,
): List<B> = coroutineScope {
    val semaphore = Semaphore(concurrency)
    map { item ->
        async { semaphore.withPermit { f(item) } }
    }.awaitAll()
}

/**
 * Short for "Asynchronous Parallel Map", but is not really parallel, only concurrent.
 */
@Deprecated(
    "This blocks with runBlocking, and should not be used inside a suspended context",
    replaceWith = ReplaceWith("amap(f)", "com.lagradost.cloudstream3.amap"),
    level = DeprecationLevel.ERROR
)
@Throws(CancellationException::class)
fun <A, B> List<A>.apmap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

/**
 * Short for "Asynchronous Parallel Map" with an Index, but is not really parallel, only concurrent.
 */
@Deprecated(
    "This blocks with runBlocking, and should not be used inside a suspended context",
    replaceWith = ReplaceWith("amapIndexed(f)", "com.lagradost.cloudstream3.amapIndexed"),
    level = DeprecationLevel.ERROR
)
@Throws(CancellationException::class)
fun <A, B> List<A>.apmapIndexed(f: suspend (index: Int, A) -> B): List<B> = runBlocking {
    mapIndexed { index, a -> async { f(index, a) } }.map { it.await() }
}

@Deprecated("Use amapIndexed with concurrency parameter", level = DeprecationLevel.HIDDEN)
@Throws(CancellationException::class)
suspend fun <A, B> List<A>.amapIndexed(f: suspend (index: Int, A) -> B): List<B> =
    amapIndexed(DEFAULT_CONCURRENCY, f)

/**
 * Short for "Asynchronous Map" with an Index, runs on all values concurrently,
 * this means that if you are not doing networking, you should use a regular mapIndexed
 */
@Throws(CancellationException::class)
suspend fun <A, B> List<A>.amapIndexed(
    concurrency: Int = DEFAULT_CONCURRENCY,
    f: suspend (index: Int, A) -> B,
): List<B> = coroutineScope {
    val semaphore = Semaphore(concurrency)
    mapIndexed { index, item ->
        async { semaphore.withPermit { f(index, item) } }
    }.awaitAll()
}

/**
 * Short for "Argument Asynchronous Map" because it allows for a variadic number of parameters.
 *
 * Runs all different functions at the same time and awaits for all to be finished, then returns
 * a list of all those items or null if they fail. However Unit is often used.
 */
@Deprecated(
    "This blocks with runBlocking, and should not be used inside a suspended context",
    replaceWith = ReplaceWith("runAllAsync(transforms)", "com.lagradost.cloudstream3.runAllAsync"),
    level = DeprecationLevel.ERROR
)
@Throws(CancellationException::class)
fun <R> argamap(
    vararg transforms: suspend () -> R,
) : List<R?> = runBlocking {
    transforms.map {
        async {
            try {
                it.invoke()
            } catch (e: Exception) {
                logError(e)
                null
            }
        }
    }.map { it.await() }
}

@Deprecated("Use runAllAsync with concurrency parameter", level = DeprecationLevel.HIDDEN)
@Throws(CancellationException::class)
suspend fun <R> runAllAsync(
    vararg transforms: suspend () -> R,
): List<R?> = runAllAsync(*transforms, concurrency = DEFAULT_CONCURRENCY)

/**
 * Runs all different functions at the same time and awaits for all to be finished, then returns
 * a list of all those items or null if they fail. However Unit is often used.
 */
@Throws(CancellationException::class)
suspend fun <R> runAllAsync(
    vararg transforms: suspend () -> R,
    concurrency: Int = DEFAULT_CONCURRENCY,
): List<R?> = coroutineScope {
    val semaphore = Semaphore(concurrency)
    transforms.map { fn ->
        async {
            semaphore.withPermit {
                try {
                    fn()
                } catch (e: CancellationException) {
                    throw e
                } catch (ex: Exception) {
                    logError(ex)
                    null
                }
            }
        }
    }.awaitAll()
}
