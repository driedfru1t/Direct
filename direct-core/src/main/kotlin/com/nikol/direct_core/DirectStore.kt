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

import com.nikol.direct_core.middleware.DirectMiddleware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The core engine of the Direct MVI architecture.
 *
 * This class manages the Unidirectional Data Flow (UDF) by handling [INTENT]s,
 * updating the [STATE], and emitting side [EFFECT]s.
 *
 *
 * @param INTENT Marker interface for user actions or events.
 * @param STATE Marker interface for the UI state (Single Source of Truth).
 * @param EFFECT Marker interface for one-off events (e.g., Navigation, Toast).
 * @param scope The [CoroutineScope] in which all store operations, flows, and handlers run.
 */
abstract class DirectStore<INTENT : DirectIntent, STATE : DirectState, EFFECT : DirectEffect>(
    protected val scope: CoroutineScope
) {
    private val isInitialized = AtomicBoolean(false)
    private val activeMiddlewares = mutableListOf<DirectMiddleware<INTENT, STATE, EFFECT>>()

    // --- State Management ---

    private val _uiState: MutableStateFlow<STATE> by lazy {
        MutableStateFlow(createInitialState())
    }

    /**
     * A hot flow representing the current state of the screen or feature.
     * It always has a value and retains the last emitted state for new collectors.
     * Thread-safe and lifecycle-aware (when collected properly).
     */
    val uiState: StateFlow<STATE> by lazy {
        ensureInitialized()
        _uiState.asStateFlow()
    }

    // --- Effect Management ---

    private val _effect = Channel<EFFECT>(Channel.BUFFERED)

    /**
     * A flow of one-off side effects (e.g., navigation events, snackbars, errors).
     * Events are buffered and delivered exactly once to the collector.
     */
    val effect: Flow<EFFECT> = _effect.receiveAsFlow()

    // --- Intent Management ---

    private val _intent = MutableSharedFlow<INTENT>()

    /**
     * The internal bus for Intents.
     * Used by the DSL builder to subscribe handlers to specific intent types.
     */
    val intentFlow: SharedFlow<INTENT> by lazy {
        ensureInitialized()
        _intent.asSharedFlow()
    }

    // --- Abstract Methods ---

    /**
     * Creates the initial state of the store.
     * This method is called lazily when [uiState] is accessed for the first time.
     */
    protected abstract fun createInitialState(): STATE

    /**
     * Defines the logic for handling intents.
     * Use the `intents { ... }` DSL block here to register handlers.
     */
    protected abstract fun handleIntents()

    // --- Public API ---

    /**
     * The entry point for all external actions.
     * Sends an [intent] to the store for processing.
     * This method also notifies all registered middlewares.
     *
     * @param intent The user action or system event.
     */
    fun dispatch(intent: INTENT) {
        ensureInitialized()
        activeMiddlewares.forEach { it.onIntent(intent) }
        scope.launch { _intent.emit(intent) }
    }

    // --- Bridge / DSL Methods ---

    /**
     * Configures intent handlers and registers middlewares using the DSL.
     * Usually called internally by the platform wrapper (e.g., Android ViewModel).
     */
    fun runDsl(block: IntentBuilder<INTENT, STATE, EFFECT>.() -> Unit) {
        val builder = IntentBuilder<INTENT, STATE, EFFECT>(intentFlow).apply(block)
        activeMiddlewares.addAll(builder.middlewares)
        builder.handlers.forEach { handler ->
            scope.launch { handler.run(scope) }
        }
    }

    /**
     * Atomically updates the current state using the provided [reducer].
     * This method notifies middlewares about state changes.
     *
     * @param reducer A lambda that takes the current state and returns the new state.
     */
    fun dispatchState(reducer: STATE.() -> STATE) {
        _uiState.update { old ->
            val new = old.reducer()
            if (old != new) {
                activeMiddlewares.forEach { it.onStateChanged(old, new) }
            }
            new
        }
    }

    /**
     * Emits a side effect to the [effect] flow.
     * This method notifies middlewares about the effect.
     *
     * @param builder A lambda returning the effect to be emitted.
     */
    fun dispatchEffect(builder: () -> EFFECT) {
        val effectInstance = builder()
        activeMiddlewares.forEach { it.onEffect(effectInstance) }
        scope.launch { _effect.send(effectInstance) }
    }

    // --- Internal Helpers ---

    private fun ensureInitialized() {
        if (isInitialized.compareAndSet(false, true)) {
            handleIntents()
        }
    }
}