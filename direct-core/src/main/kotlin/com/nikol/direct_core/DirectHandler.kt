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

package com.nikol.direct_core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A functional interface representing a fully configured event handler.
 *
 * It encapsulates the logic for processing a specific stream of events within
 * a provided [CoroutineScope]. Instances of this interface are created by the [HandlerBuilder].
 */
fun interface RegisteredHandler {
    /**
     * Launches the configured collection process within the given [scope].
     */
    suspend fun run(scope: CoroutineScope)
}

/**
 * A DSL Builder for configuring how a stream of events should be processed.
 *
 * It allows defining execution strategies (e.g., [serial], [latest], [concurrent], [dropping]),
 * applying flow transformations (e.g., debounce), and handling errors.
 *
 * @param T The type of data in the flow (usually an Intent or external data).
 */
@DirectDsl
class HandlerBuilder<T> {
    private var transforms: (Flow<T>) -> Flow<T> = { it }
    private var handler: (suspend (T) -> Unit)? = null
    private var catchBlock: ((e: Exception) -> Unit) = { throw it }

    // Default strategy is Serial (Consistently)
    private var collector: suspend (Flow<T>, suspend (T) -> Unit, CoroutineScope) -> Unit =
        { flow, action, _ -> flow.collect { action(it) } }

    /**
     * Processes events **sequentially** (serially).
     *
     * The next event will not start processing until the current one completes.
     * This guarantees that the order of execution matches the order of emission.
     *
     * @param block The suspending function to execute for each event.
     */
    fun serial(block: suspend (T) -> Unit) = setHandler(block)

    /**
     * Processes only the **latest** event.
     *
     * If a new event arrives while the previous one is still being processed,
     * the previous processing is **cancelled** immediately.
     *
     * Useful for search queries, switch toggles, or any scenario where only the
     * most recent input matters.
     *
     * @param block The suspending function to execute.
     */
    fun latest(block: suspend (T) -> Unit) = setHandler(block) { flow, action, scope ->
        scope.launch {
            flow.collectLatest { action(it) }
        }
    }

    /**
     * Processes events **concurrently** (in parallel).
     *
     * A new coroutine is launched for every incoming event. The handler does not wait
     * for the previous event to finish ("Fire-and-forget").
     *
     * **Note:** The order of completion is not guaranteed.
     *
     * @param block The suspending function to execute.
     */
    fun concurrent(block: suspend (T) -> Unit) = setHandler(block) { flow, action, scope ->
        flow.collect { item ->
            scope.launch { action(item) }
        }
    }

    /**
     * Processes events using a **dropping** strategy.
     *
     * If the handler is busy processing an event, any new events arriving during
     * that time are **ignored** (dropped).
     *
     * Useful for preventing double-clicks on buttons or preventing spamming of heavy operations.
     *
     * @param block The suspending function to execute.
     */
    fun dropping(block: suspend (T) -> Unit) = setHandler(block) { flow, action, scope ->
        var currentJob: Job? = null
        flow.collect { intent ->
            if (currentJob?.isActive != true) {
                currentJob = scope.launch {
                    action(intent)
                }
            }
        }
    }

    /**
     * Defines a custom error handler for exceptions thrown during processing
     * or flow transformations.
     *
     * @param block The lambda to handle the exception.
     */
    fun catch(block: (e: Exception) -> Unit) {
        catchBlock = block
    }

    /**
     * Applies a low-level transformation to the upstream flow.
     * Use this to add operators like `debounce`, `filter`, `distinctUntilChanged`, etc.
     *
     * @param op A function that transforms the input [Flow] into a new [Flow].
     */
    fun transform(op: (Flow<T>) -> Flow<T>) {
        val old = transforms
        transforms = { op(old(it)) }
    }

    /**
     * Builds the final [RegisteredHandler] with all configured strategies,
     * transforms, and error handling logic.
     *
     * @param upstream The source flow of events.
     * @return A ready-to-run handler.
     */
    fun build(upstream: Flow<T>): RegisteredHandler {
        val finalHandler = handler ?: error("Handler action not set. Please call serial(), latest(), concurrent(), or dropping().")
        val finalFlow = transforms(upstream).catch { catchBlock(it as Exception) }

        return RegisteredHandler { scope ->
            val safeAction: suspend (T) -> Unit = { item ->
                try {
                    finalHandler(item)
                } catch (e: Exception) {
                    catchBlock(e)
                }
            }
            collector(finalFlow, safeAction, scope)
        }
    }

    private fun setHandler(
        block: suspend (T) -> Unit,
        collect: (suspend (Flow<T>, suspend (T) -> Unit, CoroutineScope) -> Unit)? = null
    ) {
        handler = block
        collect?.let { collector = it }
    }
}