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

package com.nikol.direct_core.middleware

import com.nikol.direct_core.DirectEffect
import com.nikol.direct_core.DirectIntent
import com.nikol.direct_core.DirectState

/**
 * Interface for Store Middleware (Interceptors).
 *
 * Middleware provides a mechanism to hook into the lifecycle of the Store.
 * It allows observing and intercepting [DirectIntent]s, [DirectState] changes, and [DirectEffect]s
 * without modifying the core business logic.
 *
 * Common use cases include:
 * - Logging (e.g., to Logcat or file).
 * - Analytics and Crashlytics reporting (tracking user actions).
 * - Debugging and monitoring tools.
 *
 * @param I The type of Intent.
 * @param S The type of State.
 * @param E The type of Effect.
 */
interface DirectMiddleware<I : DirectIntent, S : DirectState, E : DirectEffect> {

    /**
     * Called immediately when an [intent] is dispatched, before it is processed by any handler.
     *
     * @param intent The intent being dispatched.
     */
    fun onIntent(intent: I) {}

    /**
     * Called after the state has been successfully updated.
     * This is usually triggered only if the new state is distinct from the old state.
     *
     * @param oldState The previous state.
     * @param newState The new current state.
     */
    fun onStateChanged(oldState: S, newState: S) {}

    /**
     * Called immediately before a side [effect] is emitted to the collectors.
     *
     * @param effect The effect being emitted.
     */
    fun onEffect(effect: E) {}
}