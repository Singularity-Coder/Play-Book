/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("PermissionUtils")

package com.singularitycoder.playbooks.helpers

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// https://github.com/android/storage-samples/tree/main/ActionOpenDocumentTree

// AppOpsManager.OPSTR_MANAGE_EXTERNAL_STORAGE is a @SystemAPI at the moment
// We should remove the annotation for applications to avoid hardcoded value
const val MANAGE_EXTERNAL_STORAGE_PERMISSION = "android:manage_external_storage"
const val NOT_APPLICABLE = "N/A"
const val MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1
const val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 2

internal fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

internal fun Activity.shouldShowRationaleFor(permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Activity.hasNotificationsPermission(): Boolean {
    return hasPermission(android.Manifest.permission.POST_NOTIFICATIONS)
}

fun getStoragePermissionName(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        MANAGE_EXTERNAL_STORAGE_PERMISSION
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

fun Activity.openPermissionSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        requestStoragePermissionApi30()
    } else {
        showAppSettings()
    }
}

fun Activity.showAppSettings() {
    this.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", this.packageName, null)
        )
    )
}

fun getLegacyStorageStatus(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Environment.isExternalStorageLegacy().toString()
    } else {
        NOT_APPLICABLE
    }
}

fun Activity.getPermissionStatus(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        checkStoragePermissionApi30().toString()
    } else {
        checkStoragePermissionApi19().toString()
    }
}

fun Activity.hasStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        checkStoragePermissionApi30()
    } else {
        checkStoragePermissionApi19()
    }
}

fun Activity.requestStoragePermission() {
    requestStoragePermissionApi30()
}

fun Activity.checkStoragePermissionApi30(): Boolean {
    val appOps = this.getSystemService(AppOpsManager::class.java)
    val mode = appOps.unsafeCheckOpNoThrow(
        MANAGE_EXTERNAL_STORAGE_PERMISSION,
        this.applicationInfo.uid,
        this.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun Activity.requestStoragePermissionApi30() {
    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    this.startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST)
}

fun Activity.checkStoragePermissionApi19(): Boolean {
    val status = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
    return status == PackageManager.PERMISSION_GRANTED
}

fun Activity.requestStoragePermissionApi19() {
    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ActivityCompat.requestPermissions(
        this,
        permissions,
        READ_EXTERNAL_STORAGE_PERMISSION_REQUEST
    )
}
