package com.github.pgreze.reactions

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.github.pgreze.reactions.PopupGravity.*
import kotlin.math.*


/**
 * This ViewGroup displays Reactions and handles interactions with them.
 *
 * It should most often be used within a ReactionPopup
 * and given height and width attributes as match_parent to properly draw the View.
 */
@SuppressLint("ViewConstructor")
class ReactionViewGroup(
    context: Context,
    private val config: ReactionsConfig
) : ViewGroup(context) {

    private companion object {
        private const val TAG = "Reaction"
    }

    private val horizontalPadding: Int = config.horizontalMargin
    private val verticalPadding: Int = config.verticalMargin

    private var iconDivider: Int = config.iconDivider ?: horizontalPadding

    private var smallIconSize: Int
    private var mediumIconSize: Int = config.reactionSize
    private var largeIconSize: Int = (config.scaleFactor * mediumIconSize).toInt()

    private var firstClick = Point()
    private var parentLocation = Point()
    private var parentSize: Size = Size(0, 0)

    private var dialogWidth: Int
    private var dialogHeight: Int = mediumIconSize + 2 * verticalPadding

    init {
        val nIcons = config.reactions.size
        val nDividers = max(1, nIcons - 1)

        // [horizontalPadding][...[iconDivider][mediumIconSize][iconDivider]...][horizontalPadding]
        dialogWidth = horizontalPadding * 2 +
                mediumIconSize * nIcons +
                iconDivider * nDividers

        // [horizontalPadding][largeIconSize][[iconDivider][smallIconSize][iconDivider]...][horizontalPadding]
        smallIconSize = (dialogWidth
                - horizontalPadding * 2
                - largeIconSize
                - iconDivider * nDividers
                ) / nDividers
    }

    private val background = CardView(context).also {
        it.layoutParams = LayoutParams(dialogWidth, dialogHeight)
        config.popupColor?.let { color -> it.setCardBackgroundColor(color) }
        it.radius = config.popupCornerRadius
        it.cardElevation = config.popupElevation
        addView(it)
    }

    private val reactions: List<ReactionView> = config.reactions
        .map { reaction ->
            ReactionView(context, reaction).also { reactionView ->
                reactionView.layoutParams = LayoutParams(mediumIconSize, mediumIconSize)
                addView(reactionView)
            }
        }
        .toList()
    private val reactionText: TextView = config.onTooltip?.invoke(context) ?: TextView(context)
        .also {
            if (config.typeface != null) {
                it.typeface = config.typeface
            }
            it.textSize = config.textSize
            it.setTextColor(config.textColor)
            it.setPadding(
                config.textHorizontalPadding,
                config.textVerticalPadding,
                config.textHorizontalPadding,
                config.textVerticalPadding
            )
            it.background = config.textBackground
            it.visibility = View.GONE
            addView(it)
        }

    private var dialogX: Int = 0
    private var dialogY: Int = 0

    private var currentState: ReactionViewState? = null
        set(value) {
            if (field == value) return

            val oldValue = field
            field = value
            Log.i(TAG, "State: $oldValue -> $value")
            when (value) {
                is ReactionViewState.Boundary -> {
                    animTranslationY(value)
                }
                is ReactionViewState.WaitingSelection -> {
                    //background.layoutParams = LayoutParams(dialogWidth, mediumIconSize)
                    //background.requestLayout()
                    background.startAnimation(ResizeAnimation(background, dialogWidth, mediumIconSize).apply {
                        duration = config.scaleDuration
                    })
                    animSize(null)
                }
                is ReactionViewState.Selected -> {
                    //background.layoutParams = LayoutParams(dialogWidth, smallIconSize)
                    //background.requestLayout()
                    background.startAnimation(ResizeAnimation(background, dialogWidth, smallIconSize).apply {
                        duration = config.scaleDuration
                    })
                    animSize(value)
                }
            }
        }

    private var currentAnimator: ValueAnimator? = null
        set(value) {
            field?.cancel()

            field = value
            reactionText.visibility = View.GONE
            field?.start()
        }

    private var isFirstTouchAlwaysInsideButton = true
    private var isIgnoringFirstReaction: Boolean = false

    var reactionSelectedListener: ReactionSelectedListener? = null

    var reactionPopupStateChangeListener: ReactionPopupStateChangeListener? = null

    var dismissListener: (() -> Unit)? = null

    // onLayout/onMeasure https://newfivefour.com/android-custom-views-onlayout-onmeasure.html
    // Detailed  https://proandroiddev.com/android-draw-a-custom-view-ef79fe2ff54b
    // Advanced sample: https://github.com/frogermcs/LikeAnimation/tree/master/app/src/main/java/frogermcs/io/likeanimation

    override fun onSizeChanged(width: Int, height: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(width, height, oldW, oldH)

        dialogX = when (config.popupGravity) {
            DEFAULT -> // Slightly on right of parent's left position
                firstClick.x - horizontalPadding - mediumIconSize / 2
            PARENT_LEFT -> // Fallback to SCREEN_RIGHT
                parentLocation.x
                    .takeUnless { it + dialogWidth > width }
                    ?: width - dialogWidth - config.popupMargin
            PARENT_RIGHT -> // Fallback to SCREEN_LEFT
                (parentLocation.x + parentSize.width - dialogWidth)
                    .takeUnless { it < 0 }
                    ?: config.popupMargin
            SCREEN_LEFT ->
                config.popupMargin
            SCREEN_RIGHT ->
                width - dialogWidth - config.popupMargin
            CENTER ->
                (width - dialogWidth) / 2
        }
        // Fallback to center if invalid position
        if (dialogX < 0 || dialogX + dialogWidth >= width) {
            dialogX = max(0, (width - dialogWidth) / 2)
        }

        // Y position will be slightly on top of parent view
        dialogY = parentLocation.y - (dialogHeight + config.upwardPopupMargin)
        if (dialogY < config.downwardPopupPoint) {
            // Below parent view
            dialogY = parentLocation.y + parentSize.height + config.downwardPopupMargin
        }
    }

    // Since we have ViewGroup children, we need to notify how much available space for them
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        background.also { view ->
            val translationX = view.translationX.toInt()
            val translationY = view.translationY.toInt()
            view.layout(
                dialogX + translationX,
                dialogY + mediumIconSize - view.layoutParams.height + translationY,
                dialogX + dialogWidth + translationX,
                dialogY + dialogHeight + translationY
            )
        }

        var prevX = 0
        reactions.forEach { view ->
            val translationX = 0
            val translationY = 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // higher layer than background
                view.z = background.cardElevation + 1f
            }
            val w = view.measuredWidth
            val h = view.measuredHeight
            val bottom = dialogY + dialogHeight - verticalPadding + translationY
            val top = bottom - h + translationY
            val left = dialogX + horizontalPadding + prevX + translationX
            val right = left + w + translationX
            view.layout(left.coerceAtLeast(l).coerceAtMost(right), top.coerceAtLeast(t).coerceAtMost(bottom), right.coerceAtMost(r), bottom.coerceAtMost(b))

            prevX += w + iconDivider
        }

        if (reactionText.visibility == View.VISIBLE) {
            reactionText.measure(0, 0)
            val selectedView = (currentState as? ReactionViewState.Selected)?.view ?: return
            val marginBottom = config.textMarginBottom ?: 3.dp
            val top = selectedView.top - min(
                selectedView.measuredHeight,
                reactionText.measuredHeight
            ) - marginBottom.toFloat()
            val bottom = top + reactionText.measuredHeight
            val left = (selectedView.right - selectedView.left).let { width ->
                selectedView.left + width / 2f - reactionText.measuredWidth / 2f
            }
            val right = left + reactionText.measuredWidth
            reactionText.layout(left.toInt(), top.roundToInt(), right.toInt(), bottom.roundToInt())
        }
    }

    var showPoint: Point? = null

    fun show(event: MotionEvent, parent: View) {
        showPoint = Point(event.rawX.roundToInt(), event.rawY.roundToInt())
        this.firstClick = Point(event.rawX.roundToInt(), event.rawY.roundToInt())
        this.parentLocation = IntArray(2)
            .also(parent::getLocationOnScreen)
            .let { Point(it[0], it[1]) }
        parentSize = Size(parent.width, parent.height)
        isFirstTouchAlwaysInsideButton = true
        isIgnoringFirstReaction = true

        // Resize, could be fixed with later resolved width/height
        onSizeChanged(width, height, width, height)

        // Appear effect
        visibility = View.VISIBLE
        currentState = ReactionViewState.Boundary.Appear(path = dialogHeight to 0)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isAnimating) return false
        isFirstTouchAlwaysInsideButton = isFirstTouchAlwaysInsideButton && inInsideParentView(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Track first moves of the first click, avoiding to auto-select first reaction
                if (isIgnoringFirstReaction) {
                    val v = reactions.first()
                    val isBelowFirstReaction = event.rawX in v.x..v.right.toFloat() &&
                            event.rawY in (v.y + v.height)..(v.y + v.height + dialogHeight)
                    isIgnoringFirstReaction = isIgnoringFirstReaction &&
                            (isBelowFirstReaction || isFirstTouchAlwaysInsideButton)
                    if (isIgnoringFirstReaction) return true
                }

                // Ignores when appearing
                if (currentState is ReactionViewState.Boundary.Appear) return true

                val view = getIntersectedIcon(event.rawX, event.rawY)
                if (view == null) {
                    currentState = ReactionViewState.WaitingSelection
                } else if ((currentState as? ReactionViewState.Selected)?.view != view) {
                    currentState = ReactionViewState.Selected(view)
                }
            }
            MotionEvent.ACTION_UP -> {
                // Ignores it if first move was always inside parent view
                isIgnoringFirstReaction = false
                if (isFirstTouchAlwaysInsideButton) {
                    isFirstTouchAlwaysInsideButton = false
                    return true
                }

                val reaction = getIntersectedIcon(event.rawX, event.rawY)?.reaction
                val position = reaction?.let { config.reactions.indexOf(it) } ?: -1
                if (reactionSelectedListener?.invoke(position)?.not() == true) {
                    currentState = ReactionViewState.WaitingSelection
                } else { // reactionSelectedListener == null or reactionSelectedListener() == true
                    dismiss()
                }

            }
            MotionEvent.ACTION_CANCEL -> {
                currentState = ReactionViewState.WaitingSelection
            }
        }
        return true
    }

    fun resetChildrenToNormalSize() {
        currentState = ReactionViewState.WaitingSelection
    }

    private fun onDismissed() {
        visibility = View.GONE
        currentState = null
        // Notify listener
        dismissListener?.invoke()
        showPoint = null
    }

    var isAnimating = false

    fun dismiss() {
        reactionPopupStateChangeListener?.invoke(false)

        if (currentState == null) return

        currentState = ReactionViewState.Boundary.Disappear(
            (currentState as? ReactionViewState.Selected)?.view,
            0 to dialogHeight
        )
    }

    private fun inInsideParentView(event: MotionEvent): Boolean =
        event.rawX >= parentLocation.x
                && event.rawX <= parentLocation.x + parentSize.width
                && event.rawY >= parentLocation.y
                && event.rawY <= parentLocation.y + parentSize.height

    private fun getIntersectedIcon(x: Float, y: Float): ReactionView? =
        reactions.firstOrNull {
            x.roundToInt() in (it.location.x - horizontalPadding)..(it.location.x + it.measuredWidth + iconDivider)
                    && y.roundToInt() in (it.location.y - horizontalPadding)..(it.location.y + it.measuredHeight + dialogHeight + iconDivider)
        }

    private fun animTranslationY(boundary: ReactionViewState.Boundary) {
        // Init views
        val isDisappear = boundary is ReactionViewState.Boundary.Disappear
        val selectedView = (boundary as? ReactionViewState.Boundary.Disappear)?.selectedView
        val initialAlpha = if (boundary is ReactionViewState.Boundary.Appear) 0f else 1f
        forEach {
            it.alpha = initialAlpha
            it.translationY = boundary.path.first.toFloat()
            if (boundary is ReactionViewState.Boundary.Appear) {
                it.layoutParams.size = mediumIconSize
                it.translationX = 0f
                it.translationY = 0f
            }
        }
        requestLayout()
        selectedView?.let { v -> if (config.dismissAnimationEnabled) {
            isAnimating = true
            v.animate()
                .withEndAction {
                    isAnimating = false
                    onDismissed()
                }
                .apply { duration = config.shootDuration + 1 }
                .scaleX(0.1f).scaleY(0.1f)
                .start()
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = config.shootDuration
            animator.interpolator = AccelerateDecelerateInterpolator()
            val targetX = config.targetX?.invoke(v) ?: (v.left - (showPoint?.x?.minus(horizontalPadding) ?: v.x).toFloat())
            val targetY = config.targetY?.invoke(v) ?: (v.bottom - (showPoint?.y?.minus(verticalPadding) ?: v.y).toFloat())
            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                v.translationX = -targetX * value

                // 0   0.5    1
                // 0.5   0   0.5

                //v.translationY = deltaY * value + ((((value - 0.5f).absoluteValue) - 0.5).absoluteValue * value * 100).toInt()
                v.translationY = -targetY * value
            }
            animator.start()
        } }

        // TODO: animate selected index if boundary == Disappear
        currentAnimator = ValueAnimator.ofFloat(0f, 1f)
            .apply {
                duration = config.dismissDuration
                addUpdateListener { animator ->
                    val progress = animator.animatedValue as Float
                    val translationY = boundary.path.progressMove(progress).toFloat()

                    forEach {
                        if (it != selectedView || !config.dismissAnimationEnabled) {
                            it.translationY = translationY
                            it.alpha = if (boundary is ReactionViewState.Boundary.Appear) {
                                progress
                            } else {
                                1 - progress
                            }
                        }
                    }

                    // Invalidate children positions
                    requestLayout()
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {}

                    override fun onAnimationEnd(animation: Animator?) {
                        when (boundary) {
                            is ReactionViewState.Boundary.Appear -> {
                                currentState = ReactionViewState.WaitingSelection
                            }
                            is ReactionViewState.Boundary.Disappear -> {
                                if (selectedView == null || !config.dismissAnimationEnabled) {
                                    onDismissed()
                                }
                            }
                        }
                    }

                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationStart(animation: Animator?) {}
                })
            }
    }

    private fun animSize(state: ReactionViewState.Selected?) {
        val paths = reactions.map {
            it.layoutParams.size to when {
                state == null -> mediumIconSize
                state.view == it -> largeIconSize
                else -> smallIconSize
            }
        }

        currentAnimator = ValueAnimator.ofFloat(0f, 1f)
            .apply {
                duration = config.scaleDuration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val progress = it.animatedValue as Float

                    reactions.forEachIndexed { index, view ->
                        val size = paths[index].progressMove(progress)
                        view.layoutParams.size = size
                    }

                    // Invalidate children positions
                    requestLayout()
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {}

                    override fun onAnimationEnd(animation: Animator?) {
                        val index = state?.view ?: return
                        reactionText.text =
                            config.reactionTextProvider(reactions.indexOf(index))
                                ?: return
                        reactionText.visibility = View.VISIBLE
                        requestLayout()
                    }

                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationStart(animation: Animator?) {}
                })
            }
    }
}

private var ViewGroup.LayoutParams.size: Int
    get() = width
    set(value) {
        width = value
        height = value
    }

/** Replace with [android.util.Size] when minSdkVersion = 21 */
private class Size(val width: Int, val height: Int)

inline fun ViewGroup.forEach(action: (View) -> Unit) {
    for (child in 0 until childCount) {
        action(getChildAt(child))
    }
}

private fun progressMove(from: Int, to: Int, progress: Float): Int =
    from + ((to - from) * progress).toInt()

private fun Pair<Int, Int>.progressMove(progress: Float): Int =
    progressMove(first, second, progress)

sealed class ReactionViewState {

    sealed class Boundary(val path: Pair<Int, Int>) : ReactionViewState() {

        /** All views are moving from +translationY to 0 with normal size */
        class Appear(path: Pair<Int, Int>) : Boundary(path)

        /**
         * Different behaviour considering [selectedView]:
         * - if no [selectedView], going down with normal size
         * - otherwise going down
         *   while [selectedView] is going (idx=0=up, other=up/left) and decreasing size
         */
        class Disappear(val selectedView: ReactionView?, path: Pair<Int, Int>) : Boundary(path)
    }

    object WaitingSelection : ReactionViewState()

    /**
     * Increase size of selected [view] while others are decreasing.
     */
    class Selected(val view: ReactionView) : ReactionViewState()
}

fun ViewPropertyAnimator.resize(
    view: View,
    toWidth: Int,
    toHeight: Int,
) = let {
    scaleX((toWidth / view.layoutParams.width.toFloat()))
    scaleY((toHeight / view.layoutParams.height.toFloat()))
}

class ResizeAnimation(
    private val view: View,
    private val toWidth: Int,
    private val toHeight: Int,
    private val fromWidth: Int? = null,
    private val fromHeight: Int? = null,
) : Animation() {
    override fun applyTransformation(
        interpolatedTime: Float,
        t: Transformation?
    ) {
        val fromWidth = fromWidth ?: view.layoutParams.width.takeIf { it >= 0 } ?: view.width
        val fromHeight = fromHeight ?: view.layoutParams.height.takeIf { it >= 0 } ?: view.height
        view.layoutParams = view.layoutParams.also {
            it.height = ((toHeight - fromHeight) * interpolatedTime + fromHeight).toInt()
            it.width = ((toWidth - fromWidth) * interpolatedTime + fromWidth).toInt()
        }
        view.requestLayout()
    }
}

val Float.dp get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this,
    Resources.getSystem().displayMetrics)

val Int.dp get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics)
