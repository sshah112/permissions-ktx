/*
 * Copyright 2020 Marcel Pinto Biescas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.marcelpinto.permissionktx

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ProcessLifecycleOwner
import dev.marcelpinto.permissionktx.internal.AndroidActivityProvider
import dev.marcelpinto.permissionktx.internal.AndroidPermissionChecker
import dev.marcelpinto.permissionktx.internal.AndroidPermissionObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Used to hold the reference to check and observe application permission status.
 *
 * @see Permission
 * @see PermissionStatus
 * @see PermissionRational
 * @see registerForPermissionResult
 */
class PermissionProvider @OptIn(ExperimentalCoroutinesApi::class) constructor(
    val checker: PermissionChecker,
    val observer: PermissionObserver,
    internal val registry: ActivityResultRegistry?
) : PermissionChecker by checker, PermissionObserver by observer {

    companion object {

        @JvmStatic
        lateinit var instance: PermissionProvider
            private set

        /**
         * When PermissionProvider self-initialization is disabled, PermissionProvider.init(context)
         * must be called to manually initialize the PermissionProvider instance. Depending on
         * implementation if init() is called more than once (multiple times in app OR in app + test),
         * an IllegalStateException is thrown.
         *
         * Provide an easy way to check if PermissionProvider instance is already
         * initialized, before calling PermissionProvider.init(context).
         */
        fun isInitialized() = ::instance.isInitialized

        /**
         * Initialize the PermissionProvider instance and wire the components to check and observe
         * PermissionProvider status changes.
         *
         * Note: by default this is called automatically via PermissionInitializer
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        fun init(context: Context) {
            check(!::instance.isInitialized) {
                "PermissionProvider instance was already initialized, if you are calling this method manually ensure you disabled the PermissionInitializer"
            }
            init(context, null, null, null)
        }

        /**
         * Init method that allows to provide custom parameters for testing purposes.
         */
        @VisibleForTesting
        @OptIn(ExperimentalCoroutinesApi::class)
        fun init(
            context: Context,
            checker: PermissionChecker? = null,
            observer: PermissionObserver? = null,
            registry: ActivityResultRegistry? = null
        ) {
            val permissionChecker =
                checker ?: AndroidPermissionChecker(AndroidActivityProvider(context).also {
                    (context.applicationContext as Application).registerActivityLifecycleCallbacks(
                        it
                    )
                })
            val permissionObserver = observer ?: AndroidPermissionObserver(
                permissionChecker,
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_PERMISSIONS
                ).requestedPermissions.toList()
            ).also {
                // Wire to lifecycle ensuring its done in the main thread
                Handler(context.mainLooper).post {
                    ProcessLifecycleOwner.get().lifecycle.addObserver(it)
                }
            }
            instance = PermissionProvider(permissionChecker, permissionObserver, registry)
        }

        /**
         * Init method for unit testing that simply creates the PermissionProvider instance with
         * the given parameters without wiring or need of Context
         */
        @VisibleForTesting
        @OptIn(ExperimentalCoroutinesApi::class)
        fun init(checker: PermissionChecker, observer: PermissionObserver) {
            instance = PermissionProvider(checker, observer, null)
        }
    }
}