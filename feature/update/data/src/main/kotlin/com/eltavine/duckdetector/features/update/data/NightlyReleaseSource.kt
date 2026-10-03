/*
 * Copyright 2026 Duck Apps Contributor
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

package com.eltavine.duckdetector.features.update.data

/** Update metadata, changelog comparisons and APK validation must describe the same fork. */
internal object NightlyReleaseSource {
    const val REPOSITORY = "zhongjitianqianguai/Duck-Detector-Refactoring"
    const val WEB_REPOSITORY = "https://github.com/$REPOSITORY"
    const val API_REPOSITORY = "https://api.github.com/repos/$REPOSITORY"
    const val DOWNLOAD_PATH_PREFIX = "/$REPOSITORY/releases/download/nightly/"
    const val MANIFEST_URL = "https://github.com${DOWNLOAD_PATH_PREFIX}update.json"
    val BUILD_BRANCHES = setOf("main", "master", "nightly")
}
