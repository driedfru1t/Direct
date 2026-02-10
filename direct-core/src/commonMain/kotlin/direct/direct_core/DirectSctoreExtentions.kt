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

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/**
 * Suspends the coroutine until the store's state satisfies the [predicate] or the [timeoutMillis] is reached.
 *
 * This utility is essential for orchestrating complex asynchronous flows where an action
 * depends on the system reaching a specific state (e.g., waiting for data to load
 * before triggering a secondary analytics event or navigation).
 *
 * It uses [first] under the hood, meaning it will resume immediately once a matching
 * state is emitted.
 *
 * Example usage:
 * ```
 * on<Intent.Submit> {
 *     val isReady = awaitState(timeoutMillis = 2000) { it is State.Ready }
 *     if (isReady) {
 *         // Proceed with submission
 *     }
 * }
 * ```
 *
 * @param S The expected type of [DirectState].
 * @param timeoutMillis The maximum time to wait in milliseconds. Defaults to 5000 (5 seconds).
 * @param predicate A lambda that returns `true` when the desired state is reached.
 * @return `true` if the state was reached within the timeout, `false` if the timeout was reached.
 */
suspend inline fun <reified S : DirectState> DirectStore<*, S, *>.awaitState(
    timeoutMillis: Long = 5_000L,
    crossinline predicate: (S) -> Boolean
): Boolean = try {
    withTimeout(timeoutMillis) {
        state.first { predicate(it) }
        true
    }
} catch (_: TimeoutCancellationException) {
    false
}