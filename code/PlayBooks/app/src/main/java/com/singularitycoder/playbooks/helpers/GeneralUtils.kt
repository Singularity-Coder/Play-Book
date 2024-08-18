package com.singularitycoder.playbooks.helpers

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Point
import android.location.LocationManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Spanned
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

// https://stackoverflow.com/questions/3160447/how-to-show-up-the-settings-for-text-to-speech-in-my-app
fun Activity.showTtsSettings() {
    val intent = Intent().apply {
        action = "com.android.settings.TTS_SETTINGS"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
}

fun Context.isRecordAudioPermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

fun View.showSnackBar(
    message: String,
    anchorView: View? = null,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionBtnText: String = "NA",
    isAnimated: Boolean = true,
    action: () -> Unit = {},
) {
    Snackbar.make(this, message, duration).apply {
        if (isAnimated) this.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
//        this.setBackgroundTint(this.context.color(R.color.black_50))
//        this.setTextColor(this.context.color(R.color.title_color))
        if (null != anchorView) this.anchorView = anchorView
        if ("NA" != actionBtnText) setAction(actionBtnText) { action.invoke() }
        this.show()
    }
}

fun getDeviceSize(): Point = try {
    Point(deviceWidth(), deviceHeight())
} catch (e: Exception) {
    e.printStackTrace()
    Point(0, 0)
}

fun deviceWidth() = Resources.getSystem().displayMetrics.widthPixels

fun deviceHeight() = Resources.getSystem().displayMetrics.heightPixels

fun Context.isCameraPresent(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
}

val mainActivityPermissions = arrayOf(
    Manifest.permission.READ_CONTACTS,
//    Manifest.permission.WRITE_CONTACTS,
//    Manifest.permission.READ_EXTERNAL_STORAGE,
//    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA,
)

fun Context.isLocationPermissionGranted(): Boolean = ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED

fun Context.showPermissionSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", this@showPermissionSettings.packageName, null)
    }
    startActivity(intent)
}

fun Context.showToast(
    message: String,
    duration: Int = Toast.LENGTH_LONG,
) = Toast.makeText(this, message, duration).show()

fun Timer.doEvery(
    duration: Long,
    withInitialDelay: Long = 0.seconds(),
    task: suspend () -> Unit
) = scheduleAtFixedRate(
    object : TimerTask() {
        override fun run() {
            CoroutineScope(Dispatchers.IO).launch { task.invoke() }
        }
    },
    withInitialDelay,
    duration
)

fun doAfter(duration: Long, task: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed(task, duration)
}

fun Number.dpToPx(): Float = this.toFloat() * Resources.getSystem().displayMetrics.density

fun Number.pxToDp(): Float = this.toFloat() / Resources.getSystem().displayMetrics.density

fun Number.spToPx(): Float = this.toFloat() * Resources.getSystem().displayMetrics.scaledDensity

fun Number.pxToSp(): Float = this.toFloat() / Resources.getSystem().displayMetrics.scaledDensity

// https://stackoverflow.com/questions/44109057/get-video-thumbnail-from-uri
@RequiresApi(Build.VERSION_CODES.O_MR1)
fun Context.getVideoThumbnailBitmap(docUri: Uri): Bitmap? {
    return try {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(this, docUri)
        mmr.getScaledFrameAtTime(
            1000, /* Time in Video */
            MediaMetadataRetriever.OPTION_NEXT_SYNC,
            128,
            128
        )
    } catch (e: Exception) {
        null
    }
}

fun Context.makeCall(phoneNum: String) {
    val callIntent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNum, null))
    callIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
    startActivity(callIntent)
}

fun Context.sendSms(phoneNum: String) = try {
    val smsIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("sms:$phoneNum")
        putExtra("sms_body", "")
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
    }
    startActivity(smsIntent)
} catch (e: Exception) {
}

fun Context.sendWhatsAppMessage(whatsAppPhoneNum: String) {
    try {
        // checks if such an app exists or not
        packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
        val uri = Uri.parse("smsto:$whatsAppPhoneNum")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply { setPackage("com.whatsapp") }
        startActivity(Intent.createChooser(intent, "Dummy Title"))
    } catch (e: PackageManager.NameNotFoundException) {
        Toast.makeText(this, "WhatsApp not found. Install from PlayStore.", Toast.LENGTH_SHORT)
            .show()
        try {
            val uri = Uri.parse("market://details?id=com.whatsapp")
            val intent = Intent(
                Intent.ACTION_VIEW,
                uri
            ).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) }
            startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

// Credit: Philip Lackner
fun <T> AppCompatActivity.collectLatestLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest(collect)
        }
    }
}

fun showSettingsAlert(context: Context) {
    AlertDialog.Builder(context).apply {
        setTitle("Enable GPS")
        setMessage("We need GPS location permission for this feature to work!")
        setPositiveButton("Settings") { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        }
        setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        show()
    }
}

// https://stackoverflow.com/questions/10311834/how-to-check-if-location-services-are-enabled#:~:text=%40lenik%2C%20some%20devices%20provide%20a,if%20specific%20providers%20are%20enabled.
// https://developer.android.com/reference/android/provider/Settings.Secure#LOCATION_PROVIDERS_ALLOWED
fun Context.isLocationToggleEnabled(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val isProvidersListEmpty = locationManager.getProviders(true).isEmpty()
        locationManager.isLocationEnabled
    } else {
        val locationProviders: String = Settings.Secure.getString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
        locationProviders.isNotBlank()
    }
}

fun Context?.clipboard(): ClipboardManager? =
    this?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

fun Activity.hideKeyboard() {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    // Find the currently focused view, so we can grab the correct window token from it.
    var view = currentFocus
    // If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

// https://stackoverflow.com/questions/4745988/how-do-i-detect-if-software-keyboard-is-visible-on-android-device-or-not
fun View?.isKeyboardVisible(): Boolean {
    this ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        WindowInsetsCompat
            .toWindowInsetsCompat(rootWindowInsets)
            .isVisible(WindowInsetsCompat.Type.ime())
    } else false
}

/** Request focus before showing keyboard - editText.requestFocus() */
fun EditText?.showKeyboard() {
    this?.requestFocus()
    if (this?.hasFocus() == true) {
        val imm = this.context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

/** Request focus before hiding keyboard - editText.requestFocus() */
fun EditText?.hideKeyboard(isClearFocus: Boolean = true) {
    this?.requestFocus()
    if (this?.hasFocus() == true) {
        val imm = this.context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(this.windowToken, 0)
    }
    if (isClearFocus) this?.clearFocus()
}

fun getHtmlFormattedTime(html: String): Spanned {
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
}

// https://developer.android.com/training/sharing/send
fun Context.shareTextOrImage(
    text: String?,
    title: String? = null,
    uri: Uri? = null
) {
    val share = Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, title) // (Optional) Here you're setting the title of the content
        if (uri != null) data = uri // (Optional) Here you're passing a content URI to an image to be displayed
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        type = if (uri == null) "text/*" else "image/*"
    }, null)
    startActivity(share)
}

// https://stackoverflow.com/questions/33222918/sharing-bitmap-via-android-intent
fun Context.shareImageAndTextViaApps(
    uri: Uri,
    title: String = "",
    subtitle: String = "",
    intentTitle: String? = null
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, subtitle)
    }
    startActivity(Intent.createChooser(intent, intentTitle ?: "Share to..."))
}


fun Context.sendCustomBroadcast(
    action: String?,
    bundle: Bundle = bundleOf()
) {
    val intent = Intent(action).apply {
        putExtras(bundle)
    }
    try {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    } catch (_: Exception) {
    }
}
