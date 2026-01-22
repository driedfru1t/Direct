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

package com.nikol.direct_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikol.direct_android.DirectViewModel
import com.nikol.direct_core.DirectEffect
import com.nikol.direct_core.DirectState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Collects the UI state from the ViewModel in a lifecycle-aware manner.
 *
 * This extension wraps [collectAsStateWithLifecycle], ensuring that the underlying flow collection
 * is active **only** when the lifecycle is in the [Lifecycle.State.STARTED] state or higher.
 *
 * Benefits:
 * - Automatically pauses data collection when the app goes to the background, saving battery and resources.
 * - Automatically resumes when the app returns to the foreground.
 * - Always provides the initial or latest emitted value.
 *
 * @return The current value of [DirectState].
 */
@Composable
fun <S : DirectState> DirectViewModel<*, S, *>.collectState(): S {
    val state by uiState.collectAsStateWithLifecycle()
    return state
}

/**
 * Safely collects and handles one-off side effects (Side Effects).
 *
 * Use this for actions that should happen exactly once, such as:
 * - Navigation events
 * - Showing Toasts or Snackbars
 * - Triggering animations
 *
 * Guarantees:
 * 1. **Lifecycle Safety:** Effects are processed only when the UI is visible ([Lifecycle.State.STARTED]).
 *    If an effect is emitted while the app is in the background, it is buffered and delivered when the app resumes.
 * 2. **Main Thread Execution:** The [block] is executed on [Dispatchers.Main.immediate], ensuring instant UI updates (crucial for Navigation).
 * 3. **No Event Loss:** Thanks to the internal buffering in DirectStore, events are not lost during configuration changes (rotation).
 *
 * Usage example:
 * ```
 * viewModel.CollectEffect { effect ->
 *     when(effect) {
 *         is MyEffect.ShowError -> Toast.makeText(context, effect.msg, Toast.LENGTH_SHORT).show()
 *         is MyEffect.NavigateHome -> navController.navigate("home")
 *     }
 * }
 * ```
 *
 * @param block The suspending lambda to handle each emitted effect.
 */
@Composable
fun <E : DirectEffect> DirectViewModel<*, *, E>.CollectEffect(
    block: suspend (E) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Using Main.immediate is important for Navigation to avoid visual glitches/delays
            withContext(Dispatchers.Main.immediate) {
                this@CollectEffect.effect.collect { effect ->
                    block(effect)
                }
            }
        }
    }
}