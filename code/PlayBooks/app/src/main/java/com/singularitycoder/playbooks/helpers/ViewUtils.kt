package com.singularitycoder.playbooks.helpers

import android.app.Activity
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.graphics.*
import android.graphics.drawable.*
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.text.*
import android.text.style.BackgroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.animation.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.singularitycoder.playbooks.MainActivity
import com.singularitycoder.playbooks.R
import java.lang.reflect.Method
import java.util.*

// underline text programatically
fun TextView.strike() {
    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
}

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

// https://stackoverflow.com/questions/6115715/how-do-i-programmatically-set-the-background-color-gradient-on-a-custom-title-ba
// https://stackoverflow.com/questions/17823451/set-android-shape-color-programmatically
// https://stackoverflow.com/questions/28578701/how-to-create-android-shape-background-programmatically
fun gradientDrawable(): GradientDrawable {
    return GradientDrawable().apply {
        colors = intArrayOf(
            R.color.purple_500,
            R.color.purple_50,
        )
        orientation = GradientDrawable.Orientation.RIGHT_LEFT
        gradientType = GradientDrawable.SWEEP_GRADIENT
        shape = GradientDrawable.RECTANGLE
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


fun AppCompatActivity.showScreen(
    fragment: Fragment,
    tag: String
) {
    supportFragmentManager.beginTransaction()
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        .add(R.id.cl_main_container, fragment, tag)
        .addToBackStack(null)
        .commit()
}

// https://stackoverflow.com/questions/37104960/bottomsheetdialog-with-transparent-background
fun BottomSheetDialogFragment.setTransparentBackground() {
    dialog?.apply {
        // window?.setDimAmount(0.2f) // Set dim amount here
        setOnShowListener {
            val bottomSheet = findViewById<View?>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
    }
}

/** https://stackoverflow.com/questions/48002290/show-entire-bottom-sheet-with-edittext-above-keyboard
 * This is for adjusting the input field properly when keyboard visible */
fun BottomSheetDialogFragment.enableSoftInput() {
    dialog?.window?.setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    )
}

fun TextView.showHideIcon(
    context: Context,
    showTick: Boolean,
    @DrawableRes leftIcon: Int = android.R.drawable.ic_delete,
    @DrawableRes topIcon: Int = android.R.drawable.ic_delete,
    @DrawableRes rightIcon: Int = android.R.drawable.ic_delete,
    @DrawableRes bottomIcon: Int = android.R.drawable.ic_delete,
    @ColorRes leftIconColor: Int = android.R.color.white,
    @ColorRes topIconColor: Int = android.R.color.white,
    @ColorRes rightIconColor: Int = android.R.color.white,
    @ColorRes bottomIconColor: Int = android.R.color.white,
    direction: Int
) {
    val left = 1
    val top = 2
    val right = 3
    val bottom = 4
    val leftRight = 5
    val topBottom = 6

    val leftDrawable = ContextCompat.getDrawable(context, leftIcon)?.changeColor(context = context, color = leftIconColor)
    val topDrawable = ContextCompat.getDrawable(context, topIcon)?.changeColor(context = context, color = topIconColor)
    val rightDrawable = ContextCompat.getDrawable(context, rightIcon)?.changeColor(context = context, color = rightIconColor)
    val bottomDrawable = ContextCompat.getDrawable(context, bottomIcon)?.changeColor(context = context, color = bottomIconColor)

    if (showTick) {
        when (direction) {
            left -> this.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, null, null, null)
            top -> this.setCompoundDrawablesWithIntrinsicBounds(null, topDrawable, null, null)
            right -> this.setCompoundDrawablesWithIntrinsicBounds(null, null, rightDrawable, null)
            bottom -> this.setCompoundDrawablesWithIntrinsicBounds(null, null, null, bottomDrawable)
            leftRight -> this.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, null, rightDrawable, null)
            topBottom -> this.setCompoundDrawablesWithIntrinsicBounds(null, topDrawable, null, bottomDrawable)
        }
    } else this.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
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

/** Use this if the list is dynamic n to set a fixed width irrespective of the width of the item */
// Create custom list item to get correct width
// Refer for a fix - https://stackoverflow.com/questions/14200724/listpopupwindow-not-obeying-wrap-content-width-spec
// defStyleAttr = com.google.android.material.R.attr.listPopupWindowStyle
fun Context.showListPopupMenu(
    anchorView: View,
    adapter: ArrayAdapter<String>,
    onItemClick: (position: Int) -> Unit
) {
//    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, todayOptions)
    ListPopupWindow(
        /* context = */ this,
        /* attrs = */ null,
        /* defStyleAttr = */ com.google.android.material.R.attr.listPopupWindowStyle
    ).apply {
        this.anchorView = anchorView
        setAdapter(adapter)
//        setContentWidth(ListPopupWindow.WRAP_CONTENT)
//        setContentWidth(measureContentWidth(adapter))
        width = ListPopupWindow.MATCH_PARENT
        setOnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            view?.setHapticFeedback()
            onItemClick.invoke(position)
            this.dismiss()
        }
        show()
    }
}

fun Context.showListPopupMenu2(
    anchorView: View?,
    menuList: List<String?>,
    onItemClick: (position: Int) -> Unit
) {
    val adapter = ArrayAdapter(
        /* context = */ this,
        /* resource = */ android.R.layout.simple_list_item_1,
        /* objects = */ menuList
    )
    ListPopupWindow(
        /* context = */ this,
        /* attrs = */ null,
        /* defStyleAttr = */ /* R.attr.listPopupWindowStyle */ 0,
        /* defStyleRes = */ R.style.ListPopupWindowTheme
    ).apply {
        this.anchorView = anchorView
        setAdapter(adapter)
        setOnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            view?.setHapticFeedback()
            onItemClick.invoke(position)
            this.dismiss()
        }
        show()
    }
}

// https://stackoverflow.com/questions/9247792/how-to-make-animation-for-popup-window-in-android/12436853
private fun Context.showPopupWindow(
    @LayoutRes layout: Int,
    @AnimRes layoutAnimation: Int
): PopupWindow? = try {
    val inflater = this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val contentView = inflater.inflate(layout, null).apply {
        animation = AnimationUtils.loadAnimation(this@showPopupWindow, layoutAnimation)
    }
    PopupWindow(
        /* contentView = */ contentView,
        /* width = */ LinearLayout.LayoutParams.WRAP_CONTENT,
        /* height = */ LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        isFocusable = true
        showAtLocation(
            /* parent = */ contentView,
            /* gravity = */ Gravity.TOP,
            /* x = */ 0,
            /* y = */ 0
        )
        update(
            /* x = */ 0,
            /* y = */ 0,
            /* width = */ LinearLayout.LayoutParams.WRAP_CONTENT,
            /* height = */ deviceHeight() / 3
        )
        animationStyle = R.style.PopupWindowAnimation
        isOutsideTouchable = true
        isFocusable = true
    }
} catch (_: Exception) {
    null
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

// https://stackoverflow.com/questions/28578701/how-to-create-android-shape-background-programmatically
fun Context.createCustomView(backgroundColor: Int, borderColor: Int): View {
    val shape = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = floatArrayOf(8f, 8f, 8f, 8f, 0f, 0f, 0f, 0f)
        setColor(backgroundColor)
        setStroke(3, borderColor)
    }
    return View(this).apply {
        background = shape
    }
}

// https://stackoverflow.com/questions/5776684/how-can-i-convert-a-view-to-a-drawable
fun createGradientDrawable(width: Int, height: Int): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(Color.TRANSPARENT)
        setSize(width, height)
    }
}

fun Context.layoutAnimationController(@AnimRes animationRes: Int): LayoutAnimationController {
    return AnimationUtils.loadLayoutAnimation(this, animationRes)
}

// To reanimate list if necessary
fun RecyclerView.runLayoutAnimation(@AnimRes animationRes: Int) {
    layoutAnimation = context.layoutAnimationController(animationRes)
    scheduleLayoutAnimation()
}

inline fun <reified T> List<T>.toArrayList(): ArrayList<T> = ArrayList(this)

fun Context.resourceUri(@AnyRes resourceId: Int): Uri = Uri.Builder()
    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
    .authority(packageName)
    .path(resourceId.toString())
    .build()

fun Context.getResourceUri(@AnyRes resourceId: Int): Uri {
    return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$resourceId")
}

fun Context.clearNotification(notificationId: Int) {
    (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(packageName, notificationId)
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

// https://stackoverflow.com/questions/2004344/how-do-i-handle-imeoptions-done-button-click
inline fun EditText.onImeDoneClick(crossinline callback: () -> Unit) {
    setOnEditorActionListener { _, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE ||
            (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
        ) {
            callback.invoke()
            return@setOnEditorActionListener true
        }
        return@setOnEditorActionListener false
    }
}

// https://stackoverflow.com/questions/7200535/how-to-convert-views-to-bitmaps
// https://www.youtube.com/watch?v=laySURtxUTk
/** If layout inflated already */
fun View.toBitmapWith(defaultColor: Int): Bitmap? = try {
    val bitmap = Bitmap.createBitmap(
        /* width = */ this.width,
        /* height = */ this.height,
        /* config = */ Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap).apply {
        drawColor(defaultColor)
    }
    this.draw(canvas)
    bitmap
} catch (e: Exception) {
    println("Error: $e")
    null
}

// https://stackoverflow.com/questions/7200535/how-to-convert-views-to-bitmaps
// https://www.youtube.com/watch?v=laySURtxUTk
/** When layout not inflated yet. Measure the view first before extracting the bitmap.
 * Else the width and height will be 0. Which means u cant do view.width & view.height.
 * If unsure of width and height specify MeasureSpec.UNSPECIFIED */
fun View.toBitmapOf(width: Int, height: Int): Bitmap? = try {
    this.measure(
        /* widthMeasureSpec = */ View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        /* heightMeasureSpec = */ View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )
    val bitmap = Bitmap.createBitmap(
        /* width = */ this.measuredWidth,
        /* height = */ this.measuredHeight,
        /* config = */ Bitmap.Config.ARGB_8888 // Each pixel is set to 4 bytes of memory in this config
    )
    this.layout(
        /* l = */ 0,
        /* t = */ 0,
        /* r = */ this.measuredWidth,
        /* b = */ this.measuredHeight
    )
    this.draw(Canvas(bitmap /* The canvas is drawn on the bitmap */)) // We are basically drawing the view on the Canvas
    bitmap
} catch (e: Exception) {
    println("Error: $e")
    null
}

fun View.isKeyboardHidden(): Boolean {
    val rect = Rect() // rect will be populated with the coordinates of your view that area still visible.
    this.getWindowVisibleDisplayFrame(rect)
    val heightDiff: Int = this.rootView.height - (rect.bottom - rect.top)
    // if heightDiff more than 500 pixels, its probably a keyboard...
    println("deviceHeight(): ${deviceHeight()}") // 3036
    println("rect.height(): ${rect.height()}") // 2952, 1860
    println("root.rootView.height: ${this.rootView.height}") // 3120
    return this.rootView.height - rect.height() < 300
}

// https://github.com/LineageOS/android_packages_apps_Jelly
// https://c1ctech.com/android-highlight-a-word-in-texttospeech/
// https://medium.com/androiddevelopers/spantastic-text-styling-with-spans-17b0c16b4568
// setSpan(ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
// setSpan(QuoteSpan(itemBinding.root.context.color(R.color.purple_500)), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
// setSpan(RelativeSizeSpan(1.5f), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
// FIXME not highlighting the right word all the time as compared to direct span assignment
fun TextView?.highlightText(
    query: String,
    result: String,
    spanList: List<ParcelableSpan> = listOf(StyleSpan(Typeface.BOLD), BackgroundColorSpan(Color.YELLOW))
): TextView? {
    if (query.isBlank() || result.isBlank()) return this
    val spannable = SpannableStringBuilder(result)
    var queryTextPos = result.toLowCase().indexOf(string = query)
    while (queryTextPos >= 0) {
        spanList.forEach { span: ParcelableSpan ->
            spannable.setSpan(
                /* what = */ span,
                /* start = */ queryTextPos,
                /* end = */ queryTextPos + query.length,
                /* flags = */ Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        queryTextPos = result.toLowCase().indexOf(string = query, startIndex = queryTextPos + query.length)
    }
    this?.text = spannable
    return this
}

// https://github.com/LineageOS/android_packages_apps_Jelly
fun isColorLight(color: Int): Boolean {
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(red, green, blue, hsl)
    return hsl[2] > 0.5f
}

// https://github.com/LineageOS/android_packages_apps_Jelly
//fun getColor(bitmap: Bitmap?, incognito: Boolean): Int {
//    val palette = Palette.from(bitmap!!).generate()
//    val fallback = Color.TRANSPARENT
//    return if (incognito) palette.getMutedColor(fallback) else palette.getVibrantColor(fallback)
//}

// https://github.com/LineageOS/android_packages_apps_Jelly
fun getShortcutIcon(bitmap: Bitmap, themeColor: Int): Bitmap {
    val out = Bitmap.createBitmap(
        bitmap.width, bitmap.width,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(out)
    val paint = Paint()
    val rect = Rect(0, 0, bitmap.width, bitmap.width)
    val radius = bitmap.width / 2.toFloat()
    paint.isAntiAlias = true
    paint.color = themeColor
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(radius, radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)
    return Bitmap.createScaledBitmap(out, 192, 192, true)
}

// https://github.com/LineageOS/android_packages_apps_Jelly
fun getPositionInTime(timeMilliSec: Long): Int {
    val diff = System.currentTimeMillis() - timeMilliSec
    val hour = 1000 * 60 * 60.toLong()
    val day = hour * 24
    val week = day * 7
    val month = day * 30
    return if (hour > diff) 0 else {
        when {
            day > diff -> 1
            week > diff -> 2
            month > diff -> 3
            else -> 4
        }
    }
}

/**
 * Shows the software keyboard.
 *
 * @param view The currently focused [View], which would receive soft keyboard input.
 */
// https://github.com/LineageOS/android_packages_apps_Jelly
@RequiresApi(Build.VERSION_CODES.M)
fun showKeyboard(view: View) {
    val imm = view.context.getSystemService(InputMethodManager::class.java)
    imm.showSoftInput(view, 0)
}

/**
 * Hides the keyboard.
 *
 * @param view The [View] that is currently accepting input.
 */
// https://github.com/LineageOS/android_packages_apps_Jelly
@RequiresApi(Build.VERSION_CODES.M)
fun hideKeyboard(view: View) {
    val imm = view.context.getSystemService(InputMethodManager::class.java)
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

/**
 * Sets the specified image button to the given state, while modifying or
 * "graying-out" the icon as well
 *
 * @param enabled The state of the menu item
 * @param button  The menu item to modify
 */
// https://github.com/LineageOS/android_packages_apps_Jelly
fun ImageButton.setImageButtonEnabled(isEnabled: Boolean) {
    this.isEnabled = isEnabled
    alpha = if (isEnabled) 1.0f else 0.4f
}

// https://github.com/LineageOS/android_packages_apps_Jelly
fun getDimenAttr(context: Context, @StyleRes style: Int, @AttrRes dimen: Int): Float {
    val args = intArrayOf(dimen)
    val array = context.obtainStyledAttributes(style, args)
    val result = array.getDimension(0, 0f)
    array.recycle()
    return result
}

// https://github.com/LineageOS/android_packages_apps_Jelly
//fun Activity.setNavigationBarColor(@ColorRes color: Int) {
//    window.navigationBarColor = this.color(color)
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        if (isColorLight(this.color(color))) {
//            window.insetsController?.setSystemBarsAppearance(
//                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
//                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
//            )
//        } else {
//            window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
//        }
//    } else {
//        var flags = window.decorView.systemUiVisibility
//        flags = if (isColorLight(this.color(color))) {
//            flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
//        } else {
//            flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
//        }
//        window.decorView.systemUiVisibility = flags
//    }
//}

// https://github.com/LineageOS/android_packages_apps_Jelly
//fun Activity.setStatusBarColor(@ColorRes color: Int) {
//    window.statusBarColor = this.color(color)
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        if (isColorLight(this.color(color))) {
//            window.insetsController?.setSystemBarsAppearance(
//                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
//                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
//            )
//        } else {
//            window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
//        }
//    } else {
//        var flags = window.decorView.systemUiVisibility
//        flags = if (isColorLight(this.color(color))) {
//            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//        } else {
//            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
//        }
//        window.decorView.systemUiVisibility = flags
//    }
//}

// https://github.com/LineageOS/android_packages_apps_Jelly
fun Activity.resetSystemUIColor(@ColorRes color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.let {
            it.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            it.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        }
    } else {
        var flags = window.decorView.systemUiVisibility
        flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        window.decorView.systemUiVisibility = flags
    }
    window.statusBarColor = this.color(color)
    window.navigationBarColor = this.color(color)
}

fun Context.colorDrawable(@ColorRes color: Int) {
    ColorDrawable(this.color(color))
}

// https://github.com/LineageOS/android_packages_apps_Jelly
fun Context.getDrawableTransition(drawableList: Array<Drawable>): Drawable {
    return TransitionDrawable(drawableList).apply {
        isCrossFadeEnabled = true
        startTransition(200)
    }
}

// https://stackoverflow.com/questions/32777597/handle-a-view-visibility-change-without-overriding-the-view
fun View.setOnVisibilityChangedListener(action: (isVisible: Boolean) -> Unit) {
    this.viewTreeObserver.addOnGlobalLayoutListener {
        val newVis: Int = this.visibility
        if (this.tag as Int? != newVis) {
            this.tag = this.visibility
            // visibility has changed
            action(this.isVisible)
        }
    }
}

// https://stackoverflow.com/questions/12128331/how-to-change-fontfamily-of-textview-in-android
fun TextView.setTypeface(context: Context, @FontRes typefaceRes: Int) {
    val typeface = ResourcesCompat.getFont(context, typefaceRes)
    setTypeface(typeface)
}

enum class SlideDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

enum class SlideType {
    SHOW,
    HIDE
}

// https://stackoverflow.com/questions/19765938/show-and-hide-a-view-with-a-slide-up-down-animation
fun View.slideAnimation(direction: SlideDirection, type: SlideType, duration: Long = 250) {
    val fromX: Float
    val toX: Float
    val fromY: Float
    val toY: Float
    val array = IntArray(2)
    getLocationInWindow(array)
    if ((type == SlideType.HIDE && (direction == SlideDirection.RIGHT || direction == SlideDirection.DOWN)) ||
        (type == SlideType.SHOW && (direction == SlideDirection.LEFT || direction == SlideDirection.UP))
    ) {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val deviceWidth = displayMetrics.widthPixels
        val deviceHeight = displayMetrics.heightPixels
        array[0] = deviceWidth
        array[1] = deviceHeight
    }
    when (direction) {
        SlideDirection.UP -> {
            fromX = 0f
            toX = 0f
            fromY = if (type == SlideType.HIDE) 0f else (array[1] + height).toFloat()
            toY = if (type == SlideType.HIDE) -1f * (array[1] + height) else 0f
        }

        SlideDirection.DOWN -> {
            fromX = 0f
            toX = 0f
            fromY = if (type == SlideType.HIDE) 0f else -1f * (array[1] + height)
            toY = if (type == SlideType.HIDE) 1f * (array[1] + height) else 0f
        }

        SlideDirection.LEFT -> {
            fromX = if (type == SlideType.HIDE) 0f else 1f * (array[0] + width)
            toX = if (type == SlideType.HIDE) -1f * (array[0] + width) else 0f
            fromY = 0f
            toY = 0f
        }

        SlideDirection.RIGHT -> {
            fromX = if (type == SlideType.HIDE) 0f else -1f * (array[0] + width)
            toX = if (type == SlideType.HIDE) 1f * (array[0] + width) else 0f
            fromY = 0f
            toY = 0f
        }
    }
    val animate = TranslateAnimation(
        fromX,
        toX,
        fromY,
        toY
    )
    animate.duration = duration
    animate.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {

        }

        override fun onAnimationEnd(animation: Animation?) {
            if (type == SlideType.HIDE) {
                visibility = View.INVISIBLE
            }
        }

        override fun onAnimationStart(animation: Animation?) {
            visibility = View.VISIBLE
        }

    })
    startAnimation(animate)
}

// https://stackoverflow.com/questions/56746361/how-to-create-a-more-natural-cardview-alpha-animation
fun Context.setTransitionDrawable(view: View?) {
    /** You can provide ColorDrawable instead of Drawable as well */
    val color = arrayOf<Drawable?>(
        drawable(R.drawable.ani_gradient_1),
        drawable(R.drawable.ani_gradient_2),
    )
    val transition = TransitionDrawable(color)
    view?.background = transition
    transition.startTransition(1000)
}

fun Context.getDrawableIcon(@DrawableRes drawableResId: Int): Drawable? {
    val icon: Drawable? = VectorDrawableCompat.create(this.resources, drawableResId, null)
    DrawableCompat.setTint(Objects.requireNonNull<Drawable?>(icon), ContextCompat.getColor(this, R.color.purple_500))
    return icon
}

// https://stackoverflow.com/questions/22192291/how-to-change-the-status-bar-color-in-android
fun Activity.setStatusBarColor(@ColorRes color: Int) {
    window.statusBarColor = ContextCompat.getColor(this, color)
}

// https://stackoverflow.com/questions/27839105/android-lollipop-change-navigation-bar-color
fun Activity.setNavigationBarColor(@ColorRes color: Int) {
    window.navigationBarColor = ContextCompat.getColor(this, color)
}

fun Menu.setMarginBtwMenuIconAndText(context: Context, iconMarginDp: Int) {
    this.forEach { item: MenuItem ->
        val iconMarginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconMarginDp.toFloat(), context.resources.displayMetrics).toInt()
        if (null != item.icon) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0)
            } else {
                item.icon = object : InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0) {
                    override fun getIntrinsicWidth(): Int = intrinsicHeight + iconMarginPx + iconMarginPx
                }
            }
        }
    }
}

// https://www.programmersought.com/article/39074216761/
fun Menu.invokeSetMenuIconMethod() {
    if (this.javaClass.simpleName.equals("MenuBuilder", ignoreCase = true)) {
        try {
            val method: Method = this.javaClass.getDeclaredMethod("setOptionalIconsVisible", java.lang.Boolean.TYPE)
            method.isAccessible = true
            method.invoke(this, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
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
    delayAfterClick: Long = 300.milliSeconds(),
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