/*
 * Copyright 2026 Nikol
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package direct.direct_core

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.time.Duration

// --- Intent Handler Extensions (Shortcuts) ---

/**
 * Registers a **sequential (serial)** handler for the specific intent type [I].
 *
 * Incoming intents are processed one by one. If a new intent arrives while the
 * previous one is being processed, it waits in the queue.
 *
 * @param block The suspending function to handle the intent.
 */
inline fun <reified I : DirectIntent> IntentBuilder<in I, *, *>.on(
    noinline block: suspend (I) -> Unit
) = setup<I> { serial(block) }

/**
 * Registers a **conditional** sequential handler for the specific intent type [I].
 *
 * If [condition] is `false`, intents of type [I] will be completely ignored (filtered out).
 * If `true`, they will be processed sequentially.
 *
 * Useful for feature toggles or checking configuration before handling events.
 *
 * @param condition If false, the handler effectively does nothing.
 * @param block The suspending function to handle the intent.
 */
inline fun <reified I : DirectIntent> IntentBuilder<in I, *, *>.on(
    condition: Boolean,
    noinline block: suspend (I) -> Unit
) = setup<I> {
    filter { condition }
    serial(block)
}

/**
 * Registers a handler that processes only the **latest** intent of type [I].
 *
 * If a new intent arrives while the previous one is still running, the previous
 * operation is **cancelled** immediately.
 *
 * Best for: Search queries, Tab switching, realtime sliders.
 *
 * @param block The suspending function to handle the intent.
 */
inline fun <reified I : DirectIntent> IntentBuilder<in I, *, *>.onLatest(
    noinline block: suspend (I) -> Unit
) = setup<I> { latest(block) }

/**
 * Registers a **concurrent (parallel)** handler for the specific intent type [I].
 *
 * Every incoming intent launches a new coroutine immediately. Multiple intents
 * can be processed at the same time. The order of completion is not guaranteed.
 *
 * Best for: Analytics events, independent logs, fire-and-forget actions.
 *
 * @param block The suspending function to handle the intent.
 */
inline fun <reified I : DirectIntent> IntentBuilder<in I, *, *>.onParallel(
    noinline block: suspend (I) -> Unit
) = setup<I> { concurrent(block) }

/**
 * Registers a handler that **drops** new intents while busy.
 *
 * If an intent is currently being processed, any new intents of type [I]
 * arriving during that time are ignored.
 *
 * Best for: Submit buttons (preventing double-click), heavy refresh operations.
 *
 * @param block The suspending function to handle the intent.
 */
inline fun <reified I : DirectIntent> IntentBuilder<in I, *, *>.onSingle(
    noinline block: suspend (I) -> Unit
) = setup<I> { dropping(block) }


// --- External Flow Listeners ---

/**
 * Subscribes to an external [source] Flow and processes emissions **sequentially**.
 *
 * @param source The external flow to listen to (e.g., Repository updates, Timer).
 * @param block The action to perform for each emitted value.
 */
inline fun <reified T> IntentBuilder<*, *, *>.listen(
    source: Flow<T>,
    noinline block: suspend (T) -> Unit
) = listen(source) { serial(block) }

/**
 * Subscribes to an external [source] Flow and processes only the **latest** emission.
 * Previous processing is cancelled when a new value is emitted.
 *
 * @param source The external flow to listen to.
 * @param block The action to perform.
 */
inline fun <reified T> IntentBuilder<*, *, *>.listenLatest(
    source: Flow<T>,
    noinline block: suspend (T) -> Unit
) = listen(source) { latest(block) }

/**
 * Subscribes to an external [source] Flow and processes emissions **concurrently**.
 *
 * @param source The external flow to listen to.
 * @param block The action to perform.
 */
inline fun <reified T> IntentBuilder<*, *, *>.listenParallel(
    source: Flow<T>,
    noinline block: suspend (T) -> Unit
) = listen(source) { concurrent(block) }

/**
 * Subscribes to an external [source] Flow and **drops** emissions while busy.
 *
 * @param source The external flow to listen to.
 * @param block The action to perform.
 */
inline fun <reified T> IntentBuilder<*, *, *>.listenSingle(
    source: Flow<T>,
    noinline block: suspend (T) -> Unit
) = listen(source) { dropping(block) }


// --- Handler Builder Transformations ---

/**
 * Filters out emissions that are followed by newer emissions within the given [ms] timeout.
 * Useful for handling rapid input (e.g., text typing).
 */
@OptIn(FlowPreview::class)
fun HandlerBuilder<*>.debounce(ms: Long) = transform { it.debounce(ms) }

/**
 * Filters out emissions that are followed by newer emissions within the given [duration].
 */
@OptIn(FlowPreview::class)
fun HandlerBuilder<*>.debounce(duration: Duration) = transform { it.debounce(duration) }

/**
 * Suppresses consecutive duplicate emissions.
 * The handler will only be triggered if the new value is different from the previous one.
 * (Relies on [equals]).
 */
fun HandlerBuilder<*>.distinct() = transform { flow -> flow.distinctUntilChanged() }

/**
 * Filters the stream, allowing only items that satisfy the given [predicate].
 *
 * @param predicate A suspending function that returns `true` to keep the item, or `false` to drop it.
 */
inline fun <reified I> HandlerBuilder<I>.filter(
    crossinline predicate: suspend (I) -> Boolean
) = transform { flow -> flow.filter(predicate) }