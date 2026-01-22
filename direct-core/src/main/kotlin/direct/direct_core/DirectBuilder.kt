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

import direct.direct_core.middleware.DirectMiddleware
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * DSL marker used to restrict the scope of receivers in the Direct DSL.
 * Prevents mixing scopes from nested blocks.
 */
@DslMarker
annotation class DirectDsl

/**
 * The main DSL entry point for configuring the Store's behavior.
 *
 * This class is the receiver of the `intents { ... }` block. It allows you to:
 * 1. Register handlers for specific Intent types via [setup].
 * 2. Subscribe to external data sources via [listen].
 * 3. Install middlewares (plugins) via [install].
 *
 * @param INTENT The base type of intents handled by this builder.
 * @param STATE The state type (required for middleware typing).
 * @param EFFECT The effect type (required for middleware typing).
 * @property upstream The source flow of all incoming intents.
 */
@DirectDsl
class IntentBuilder<INTENT : DirectIntent, STATE : DirectState, EFFECT : DirectEffect>(
    val upstream: Flow<INTENT>
) {
    /** Internal list of configured handlers. */
    val handlers = mutableListOf<RegisteredHandler>()

    /** Internal list of registered middlewares. */
    val middlewares = mutableListOf<DirectMiddleware<INTENT, STATE, EFFECT>>()

    /**
     * Registers a [DirectMiddleware] for this Store.
     *
     * Middleware allows you to intercept Intents, State changes, and Effects
     * for logging, analytics, or crash reporting purposes.
     *
     * @param middleware The middleware instance to install.
     */
    fun install(middleware: DirectMiddleware<INTENT, STATE, EFFECT>) {
        middlewares.add(middleware)
    }

    /**
     * Configures a handler for a specific [INTENT] type [I].
     *
     * Automatically filters the global intent flow to include only instances of [I].
     * Inside the [configure] block, you can define the execution strategy
     * (e.g., serial, latest, concurrent) and the business logic.
     *
     * @param configure The configuration block for the handler.
     */
    inline fun <reified I : INTENT> setup(
        configure: HandlerBuilder<I>.() -> Unit
    ) {
        val builder = HandlerBuilder<I>().apply(configure)
        val filteredFlow = upstream.filterIsInstance<I>()
        handlers.add(builder.build(filteredFlow))
    }

    /**
     * Subscribes to an external [source] Flow (not an Intent).
     *
     * Use this to react to repository updates, system broadcasts, timers,
     * or any other data stream that lives outside the UI layer but should affect the Store.
     *
     * @param source The external flow to listen to.
     * @param configure The configuration block for the handler.
     */
    inline fun <T> listen(
        source: Flow<T>,
        configure: HandlerBuilder<T>.() -> Unit
    ) {
        val builder = HandlerBuilder<T>().apply(configure)
        handlers.add(builder.build(source))
    }
}