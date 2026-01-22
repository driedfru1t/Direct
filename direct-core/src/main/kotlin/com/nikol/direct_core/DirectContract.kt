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

/**
 * Marker interface for an Intent (Action).
 *
 * Represents a user interaction (e.g., ButtonClick, TextChanged) or a system event
 * that triggers business logic within the Store.
 * Intents are the inputs of the system.
 */
interface DirectIntent

/**
 * Marker interface for the UI State.
 *
 * Represents the immutable data required to render the screen at a specific point in time.
 * In Direct architecture, the State acts as the Single Source of Truth for the UI.
 */
interface DirectState

/**
 * Marker interface for a Side Effect.
 *
 * Represents a one-off event that should be handled by the UI exactly once,
 * such as Navigation, showing a Toast/Snackbar, or triggering an Animation.
 * Unlike State, Effects are not retained (they are "hot").
 */
interface DirectEffect