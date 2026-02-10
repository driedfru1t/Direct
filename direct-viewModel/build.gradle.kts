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

@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}


kotlin {
    jvmToolchain(17)
    withSourcesJar()
    androidLibrary {
        namespace = "direct.direct_viewmodel"
        compileSdk = 36
        minSdk = 24
    }

    val xcfName = "direct-viewModelKit"
    listOf(
        iosX64(), iosArm64(), iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                api(projects.directCore)
                implementation(libs.androidx.lifecycle.viewmodel)
            }
        }
    }
}

apply(from = "../publish-local.gradle.kts")