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

package com.nikol.direct_android.middleware

import android.util.Log
import com.nikol.direct_core.DirectEffect
import com.nikol.direct_core.DirectIntent
import com.nikol.direct_core.DirectState
import com.nikol.direct_core.middleware.DirectMiddleware

/**
 * A ready-to-use Middleware implementation that logs all Store events to the Android Logcat.
 *
 * It provides visual tracking of the Unidirectional Data Flow by logging:
 * - Incoming [DirectIntent]s (marked with ➡️)
 * - [DirectState] transitions (marked with 🔄)
 * - Emitted [DirectEffect]s (marked with ⚡)
 *
 * This middleware is highly recommended during development and debugging to trace
 * user actions and system responses.
 *
 * @param tag The tag to be used in [Log.d]. Usually, the name of the ViewModel (e.g., "HomeVM") is used.
 */
class LogMiddleware<I : DirectIntent, S : DirectState, E : DirectEffect>(
    private val tag: String = "Direct"
) : DirectMiddleware<I, S, E> {

    override fun onIntent(intent: I) {
        Log.d(tag, "➡️ Intent: $intent")
    }

    override fun onStateChanged(oldState: S, newState: S) {
        Log.d(tag, "🔄 State: $oldState -> $newState")
    }

    override fun onEffect(effect: E) {
        Log.d(tag, "⚡ Effect: $effect")
    }
}