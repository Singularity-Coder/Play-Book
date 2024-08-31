package com.singularitycoder.playbooks.helpers

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// https://stackoverflow.com/questions/3160447/how-to-show-up-the-settings-for-text-to-speech-in-my-app
fun Activity.showTtsSettings() {
    val intent = Intent().apply {
        action = "com.android.settings.TTS_SETTINGS"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
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

fun deviceWidth() = Resources.getSystem().displayMetrics.widthPixels

fun deviceHeight() = Resources.getSystem().displayMetrics.heightPixels

fun Context.showToast(
    message: String,
    duration: Int = Toast.LENGTH_LONG,
) = Toast.makeText(this, message, duration).show()

fun Number.dpToPx(): Float = this.toFloat() * Resources.getSystem().displayMetrics.density

// Credit: Philip Lackner
fun <T> AppCompatActivity.collectLatestLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest(collect)
        }
    }
}

fun Context?.clipboard(): ClipboardManager? =
    this?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

/** Request focus before hiding keyboard - editText.requestFocus() */
fun EditText?.hideKeyboard(isClearFocus: Boolean = true) {
    this?.requestFocus()
    if (this?.hasFocus() == true) {
        val imm = this.context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(this.windowToken, 0)
    }
    if (isClearFocus) this?.clearFocus()
}

fun Context.showWebPage(url: String) {
    CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
}

// https://medium.com/@saishaddai/how-to-know-when-youre-using-dark-mode-programmatically-9be83fded4b0
fun Context.isDarkModeOn(): Boolean {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}
