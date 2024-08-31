package com.singularitycoder.playbooks.helpers

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.PopupMenu
import androidx.annotation.AnimRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.singularitycoder.playbooks.MainActivity
import com.singularitycoder.playbooks.R

// https://stackoverflow.com/questions/2004344/how-do-i-handle-imeoptions-done-button-click
fun EditText.onImeClick(
    imeAction: Int = EditorInfo.IME_ACTION_DONE,
    callback: () -> Unit
) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == imeAction) {
            callback.invoke()
            return@setOnEditorActionListener true
        }
        false
    }
}

fun MainActivity.showScreen(
    fragment: Fragment,
    tag: String,
    isAdd: Boolean = false,
    isAddToBackStack: Boolean = true,
    @AnimRes enterAnim: Int = R.anim.slide_to_left,
    @AnimRes exitAnim: Int = R.anim.slide_to_right,
    @AnimRes popEnterAnim: Int = R.anim.slide_to_left,
    @AnimRes popExitAnim: Int = R.anim.slide_to_right,
) {
    if (isAdd) {
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
            .add(R.id.fragment_container_view, fragment, tag)
        if (isAddToBackStack) transaction.addToBackStack(null)
        transaction.commit()
    } else {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, fragment, tag)
            .commit()
    }
}

fun Drawable.changeColor(
    context: Context,
    @ColorRes color: Int
): Drawable {
    val unwrappedDrawable = this
    val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable)
    DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(context, color))
    return this
}

fun Context.color(@ColorRes colorRes: Int) = ContextCompat.getColor(this, colorRes)

fun Context.drawable(@DrawableRes drawableRes: Int): Drawable? = ContextCompat.getDrawable(this, drawableRes)

fun Context.showAlertDialog(
    title: String = "",
    message: String,
    positiveBtnText: String,
    negativeBtnText: String = "",
    neutralBtnText: String = "",
    icon: Drawable? = null,
    @ColorRes positiveBtnColor: Int? = null,
    @ColorRes negativeBtnColor: Int? = null,
    @ColorRes neutralBtnColor: Int? = null,
    positiveAction: () -> Unit = {},
    negativeAction: () -> Unit = {},
    neutralAction: () -> Unit = {},
) {
    MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog).apply {
        setCancelable(false)
        if (title.isNotBlank()) setTitle(title)
        setMessage(message)
        background = drawable(R.drawable.alert_dialog_bg)
        if (icon != null) setIcon(icon)
        setPositiveButton(positiveBtnText) { dialog, int ->
            positiveAction.invoke()
        }
        if (negativeBtnText.isNotBlank()) {
            setNegativeButton(negativeBtnText) { dialog, int ->
                negativeAction.invoke()
            }
        }
        if (neutralBtnText.isNotBlank()) {
            setNeutralButton(neutralBtnText) { dialog, int ->
                neutralAction.invoke()
            }
        }
        val dialog = create()
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).apply {
            setHapticFeedback()
            isAllCaps = false
            setPadding(0, 0, 16.dpToPx().toInt(), 0)
            if (positiveBtnColor != null) setTextColor(this@showAlertDialog.color(positiveBtnColor))
        }
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).apply {
            setHapticFeedback()
            isAllCaps = false
            if (negativeBtnColor != null) setTextColor(this@showAlertDialog.color(negativeBtnColor))
        }
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).apply {
            setHapticFeedback()
            isAllCaps = false
            setPadding(16.dpToPx().toInt(), 0, 0, 0)
            if (neutralBtnColor != null) setTextColor(this@showAlertDialog.color(neutralBtnColor))
        }
    }
}

fun Context.showPopupMenu(
    view: View?,
    title: String? = null,
    menuList: List<String?>,
    onItemClick: (position: Int) -> Unit
) {
    val popupMenu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        PopupMenu(
            /* context = */ this,
            /* anchor = */ view,
            /* gravity = */ 0,
            /* popupStyleAttr = */ 0,
            /* popupStyleRes = */ R.style.PopupMenuTheme
        )
    } else {
        PopupMenu(
            /* context = */ this,
            /* anchor = */ view
        )
    }
    popupMenu.apply {
        if (title != null) {
            menu.add(Menu.NONE, -1, 0, title).apply {
                isEnabled = false
            }
        }
        menuList.forEach {
            menu.add(it)
        }
        setOnMenuItemClickListener { it: MenuItem? ->
            view?.setHapticFeedback()
            onItemClick.invoke(menuList.indexOf(it?.title))
            false
        }
        show()
    }
}

// TODO set bottom n top margins - popupStyleAttr or popupStyleRes for both PopupMenu n ListPopupWindow. Default attributes like R.attr.listPopupWindowStyle seem to have their own background which is overriding mine
// popupStyleAttr = R.attr.popupMenuStyle
// popupStyleAttr = com.google.android.material.R.attr.popupMenuStyle
fun Context.showPopupMenuWithIcons(
    view: View?,
    title: String? = null,
    customColorItemText: String = "",
    @ColorRes customColor: Int = 0,
    menuList: List<Pair<String, Int>>,
    iconWidth: Int = -1,
    iconHeight: Int = -1,
    defaultSpaceBtwIconTitle: String = "    ",
    isColoredIcon: Boolean = true,
    colorsList: List<Int> = emptyList(),
    onItemClick: (menuItem: MenuItem?) -> Unit
) {
    val popupMenu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        PopupMenu(
            /* context = */ this,
            /* anchor = */ view,
            /* gravity = */ 0,
            /* popupStyleAttr = */ 0,
            /* popupStyleRes = */ R.style.PopupMenuTheme
        )
    } else {
        PopupMenu(
            /* context = */ this,
            /* anchor = */ view
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        popupMenu.menu.setGroupDividerEnabled(true)
    }
    if (title != null) {
        popupMenu.menu.add(Menu.NONE, -1, 0, title).apply {
            isEnabled = false
        }
    }
    val groupId = if (menuList.last().first.contains(other = "delete", ignoreCase = true)) {
        menuList.lastIndex
    } else 0
    menuList.forEachIndexed { index, pair ->
        val icon = if (colorsList.isNotEmpty()) {
            drawable(pair.second)?.changeColor(this, colorsList[index])
        } else {
            if (pair.first == customColorItemText) {
                drawable(pair.second)?.changeColor(this, customColor)
            } else {
                drawable(pair.second)?.apply {
                    if (isColoredIcon) changeColor(this@showPopupMenuWithIcons, R.color.purple_500)
                }
            }
        }
        val insetDrawable = InsetDrawable(
            /* drawable = */ icon,
            /* insetLeft = */ 0,
            /* insetTop = */ 0,
            /* insetRight = */ 0,
            /* insetBottom = */ 0
        )
        popupMenu.menu.add(
            /* groupId */ /* if (index == groupId) groupId else 0 */ 0,
            /* itemId */ 1,
            /* order */ 1,
            /* title */ menuIconWithText(
                icon = insetDrawable,
                title = pair.first,
                iconWidth = iconWidth,
                iconHeight = iconHeight,
                defaultSpace = defaultSpaceBtwIconTitle
            )
        )
//        popupMenu.menu.get(index).actionView?.setMargins(start = 0, top = 0, end = 0, bottom = 8.dpToPx().toInt())
//        findViewById<ViewGroup>(popupMenu.menu.get(index).itemId).get(index)
    }
    popupMenu.setOnMenuItemClickListener { it: MenuItem? ->
        view?.setHapticFeedback()
        onItemClick.invoke(it)
        false
    }
    popupMenu.show()
}

fun Context.showSingleSelectionPopupMenu(
    view: View?,
    title: String? = null,
    selectedOption: String? = null,
    @ColorRes enabledColor: Int = R.color.purple_500,
    @ColorRes disabledColor: Int = android.R.color.transparent,
    menuList: List<Pair<String, Int>>,
    onItemClick: (menuItem: MenuItem?) -> Unit
) {
    val popupMenu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        PopupMenu(
            /* context = */ this,
            /* anchor = */ view,
            /* gravity = */ 0,
            /* popupStyleAttr = */ 0,
            /* popupStyleRes = */ R.style.PopupMenuTheme
        )
    } else {
        PopupMenu(
            /* context = */ this,
            /* anchor = */ view
        )
    }
    popupMenu.menu.add(Menu.NONE, -1, 0, title).apply {
        isEnabled = false
    }
    menuList.forEach { pair: Pair<String, Int> ->
        popupMenu.menu.add(
            0, 1, 1, menuIconWithText(
                icon = this.drawable(pair.second)?.changeColor(
                    context = this,
                    color = if (selectedOption == pair.first) enabledColor else disabledColor
                ),
                title = pair.first
            )
        )
    }
    popupMenu.setOnMenuItemClickListener { menuItem: MenuItem? ->
        view?.setHapticFeedback()
        onItemClick.invoke(menuItem)
        false
    }
    popupMenu.show()
}

// https://stackoverflow.com/questions/32969172/how-to-display-menu-item-with-icon-and-text-in-appcompatactivity
// https://developer.android.com/develop/ui/views/text-and-emoji/spans
fun menuIconWithText(
    icon: Drawable?,
    title: String,
    iconWidth: Int = -1,
    iconHeight: Int = -1,
    defaultSpace: String = "    "
): CharSequence {
    icon?.setBounds(
        /* left = */ 0,
        /* top = */ 0,
        /* right = */ if (iconWidth == -1) icon.intrinsicWidth else iconWidth,
        /* bottom = */ if (iconHeight == -1) icon.intrinsicHeight else iconHeight
    )
    icon ?: return title
    val imageSpan = ImageSpan(icon, ImageSpan.ALIGN_BOTTOM)
    return SpannableString("$defaultSpace$title").apply {
        setSpan(
            /* what = */ imageSpan,
            /* startCharPos = */ 0,
            /* endCharPos = */ 1,
            /* flags = */ Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

fun Context.layoutAnimationController(@AnimRes animationRes: Int): LayoutAnimationController {
    return AnimationUtils.loadLayoutAnimation(this, animationRes)
}

fun View.setMargins(
    all: Int? = null,
    start: Int = 0,
    top: Int = 0,
    end: Int = 0,
    bottom: Int = 0
) {
    if (this.layoutParams !is ViewGroup.MarginLayoutParams) return
    val params = this.layoutParams as ViewGroup.MarginLayoutParams
    if (all != null) {
        params.setMargins(all, all, all, all)
    } else {
        params.setMargins(start, top, end, bottom)
    }
    this.requestLayout()
}

// https://stackoverflow.com/questions/27839105/android-lollipop-change-navigation-bar-color
fun Activity.setNavigationBarColor(@ColorRes color: Int) {
    window.navigationBarColor = ContextCompat.getColor(this, color)
}

// https://stackoverflow.com/questions/2228151/how-to-enable-haptic-feedback-on-button-view
fun View.setHapticFeedback() {
    isHapticFeedbackEnabled = true
    performHapticFeedback(
        HapticFeedbackConstants.VIRTUAL_KEY,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING  // Ignore device's setting. Otherwise, you can use FLAG_IGNORE_VIEW_SETTING to ignore view's setting.
    )
}

// https://stackoverflow.com/questions/5608720/android-preventing-double-click-on-a-button
fun View.onSafeClick(
    delayAfterClick: Long = 700.milliSeconds(),
    onSafeClick: (Pair<View?, Boolean>) -> Unit
) {
    val onSafeClickListener = OnSafeClickListener(delayAfterClick, onSafeClick)
    setOnClickListener(onSafeClickListener)
}

fun View.onCustomLongClick(
    onCustomLongClick: (view: View?) -> Unit
) {
    val onCustomLongClickListener = OnCustomLongClickListener(onCustomLongClick)
    setOnLongClickListener(onCustomLongClickListener)
}

class OnSafeClickListener(
    private val delayAfterClick: Long,
    private val onSafeClick: (Pair<View?, Boolean>) -> Unit
) : View.OnClickListener {
    private var lastClickTime = 0L
    private var isClicked = false

    override fun onClick(v: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
        val elapsedRealtime = SystemClock.elapsedRealtime()
        if (elapsedRealtime - lastClickTime < delayAfterClick) return
        lastClickTime = elapsedRealtime
//        v?.startAnimation(AlphaAnimation(1F, 0.8F))
//        v?.setTouchEffect()
        isClicked = !isClicked
        onSafeClick(v to isClicked)
//        v?.setHapticFeedback()
    }
}

class OnCustomLongClickListener(
    private val onCustomClick: (view: View?) -> Unit
) : View.OnLongClickListener {
    override fun onLongClick(v: View?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        onCustomClick.invoke(v)
        return false
    }
}