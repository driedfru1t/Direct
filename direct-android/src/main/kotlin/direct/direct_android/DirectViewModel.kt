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

package direct.direct_android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import direct.direct_core.DirectEffect
import direct.direct_core.DirectIntent
import direct.direct_core.DirectState
import direct.direct_core.DirectStore
import direct.direct_core.IntentBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Base Android ViewModel implementation for the Direct MVI architecture.
 *
 * This class bridges the platform-independent [DirectStore] with the Android Lifecycle.
 * It automatically manages the [viewModelScope] for coroutines and provides a
 * lifecycle-aware state stream.
 *
 * @param INTENT The type of user actions/events.
 * @param STATE The type of UI state.
 * @param EFFECT The type of one-off side effects.
 */
abstract class DirectViewModel<INTENT : DirectIntent, STATE : DirectState, EFFECT : DirectEffect> :
    ViewModel() {

    // Internal Store instance (Composition/Delegation pattern)
    // Delegates logic to the platform-agnostic DirectStore, binding it to the Android viewModelScope.
    private val store = object : DirectStore<INTENT, STATE, EFFECT>(viewModelScope) {
        override fun createInitialState() = this@DirectViewModel.createInitialState()
        override fun handleIntents() = this@DirectViewModel.handleIntents()
    }

    // --- Public API ---

    /**
     * Exposes the current UI state as a [StateFlow].
     * This flow is lifecycle-aware and always holds the latest state value.
     */
    val uiState: StateFlow<STATE> get() = store.uiState

    /**
     * Exposes the stream of one-off side effects (e.g., Navigation, Toasts).
     * Events in this flow are buffered and delivered exactly once.
     */
    val effect: Flow<EFFECT> get() = store.effect

    /**
     * Dispatches an [intent] to the store for processing.
     * This is the main entry point for UI events (clicks, input, etc.).
     */
    fun setIntent(intent: INTENT) = store.dispatch(intent)

    // --- Abstract Methods ---

    /**
     * Creates the initial state of the screen.
     * Called lazily when the state is first observed.
     */
    protected abstract fun createInitialState(): STATE

    /**
     * Defines the business logic and event handlers.
     * Override this method and use the [intents] DSL block to setup behavior.
     */
    protected abstract fun handleIntents()

    // --- DSL Helpers ---

    /**
     * The main DSL block for configuring the ViewModel's behavior.
     * Inside this block, you can register handlers for intents (using `on`, `onLatest`, etc.)
     * and install middlewares.
     */
    protected fun intents(block: IntentBuilder<INTENT, STATE, EFFECT>.() -> Unit) =
        store.runDsl(block)

    /**
     * Updates the UI state.
     * Thread-safe and supports partial updates via the [reducer] lambda.
     */
    protected fun setState(reducer: STATE.() -> STATE) = store.dispatchState(reducer)

    /**
     * Sends a one-off side effect to the UI.
     */
    protected fun setEffect(builder: () -> EFFECT) = store.dispatchEffect(builder)
}