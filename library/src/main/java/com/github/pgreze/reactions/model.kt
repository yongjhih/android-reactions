package com.github.pgreze.reactions

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

/**
 * Selected reaction callback.
 * @param position selected item position, or -1.
 * @return if reaction selector should close.
 */
typealias ReactionSelectedListener = (position: Int) -> Boolean

/**
 * Reaction text provider.
 * @param position position of current selected item in [ReactionsConfig.reactions].
 * @return optional reaction text, null for no text.
 */
typealias ReactionTextProvider = (position: Int) -> CharSequence?

/**
 * Popup state change listener
 * @param isShowing flag denoting reaction pop is being displayed or not
 */
typealias ReactionPopupStateChangeListener = (isShowing: Boolean) -> Unit

data class Reaction(
    val onView: (Context) -> View,
    val scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER
)

data class ReactionsConfig(
    val reactions: Collection<Reaction>,
    @Px val reactionSize: Int,
    @Px val horizontalMargin: Int,
    @Px val verticalMargin: Int,

    /** Horizontal gravity compare to parent view or screen */
    val popupGravity: PopupGravity,
    /** Margin between dialog and screen border used by [PopupGravity] screen related values. */
    val popupMargin: Int,
    val popupCornerRadius: Float,
    val popupElevation: Float,
    @ColorInt val popupColor: Int,
    @IntRange(from = 0, to = 255) val popupAlphaValue: Int,
    /** Margin between dialog and parent view when dialog opens upward */
    val upwardPopupMargin: Int,
    /** Margin between dialog and parent view when dialog opens downward */
    val downwardPopupMargin: Int,

    /** Point X location for when the dialog will open downward if dialogY is less than it */
    val downwardPopupPoint: Int,

    val reactionTextProvider: ReactionTextProvider,
    val textBackground: Drawable,
    @ColorInt val textColor: Int,
    val textHorizontalPadding: Int,
    val textVerticalPadding: Int,
    val textSize: Float,
    val typeface: Typeface?,
    val scaleFactor: Float,
    val targetX: Float?,
    val targetY: Float?,
    val dismissAnimationEnabled: Boolean,
)

private val NO_TEXT_PROVIDER: ReactionTextProvider = { _ -> null }

enum class PopupGravity {
    /** Default position, similar to Facebook app. */
    DEFAULT,

    /** Align dialog left side with left side of the parent view. */
    PARENT_LEFT,

    /** Align dialog right side with right side of the parent view. */
    PARENT_RIGHT,

    /** Position dialog on left side of the screen. */
    SCREEN_LEFT,

    /** Position dialog on right side of the screen. */
    SCREEN_RIGHT,

    /** Position dialog on center of the screen. */
    CENTER
}

class ReactionsConfigBuilder(val context: Context) {

    // DSL friendly property based values, with default or empty values replaced during build

    var reactions: Collection<Reaction> = emptyList()

    // reactions = listOf(R.drawable.img1, R.drawable.img2, ...)
    var reactionsIds: IntArray
        get() = throw NotImplementedError()
        set(value) {
            withReactions(value)
        }

    @Px
    var reactionSize: Int =
        context.resources.getDimensionPixelSize(R.dimen.reactions_item_size)

    @Px
    var horizontalMargin: Int =
        context.resources.getDimensionPixelSize(R.dimen.reactions_item_margin)

    @Px
    var verticalMargin: Int = horizontalMargin

    var popupGravity: PopupGravity = PopupGravity.DEFAULT

    var customTypeface: Typeface? = null

    var popupMargin: Int = horizontalMargin

    var upwardPopupMargin: Int = horizontalMargin

    var downwardPopupMargin: Int = horizontalMargin

    var downwardPopupPoint: Int = 0

    var popupCornerRadius: Float = 90f

    var popupElevation: Float = 8f

    @ColorInt
    var popupColor: Int = Color.WHITE

    var popupAlpha: Int = 230

    var reactionTextProvider: ReactionTextProvider = NO_TEXT_PROVIDER

    var reactionTexts: Int
        get() = throw NotImplementedError()
        set(@ArrayRes value) {
            withReactionTexts(value)
        }

    var textBackground: Drawable? = null

    @ColorInt
    var textColor: Int = Color.WHITE

    var textHorizontalPadding: Int = 0

    var textVerticalPadding: Int = 0

    var textSize: Float = 0f

    var scaleFactor = 2.0f
    var targetX: Float? = null
    var targetY: Float? = null
    var dismissAnimationEnabled: Boolean = true

    // Builder pattern for Java

    fun withReactions(reactions: Collection<Reaction>) = this.also {
        this.reactions = reactions
    }

    @JvmOverloads
    fun withReactions(
        res: IntArray,
        scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER
    ) = withReactions(res.map { r -> Reaction(
        onView = { ctx -> ImageView(ctx).apply {
            setImageResource(r)
            this.scaleType = scaleType
        } },
        scaleType,
    ) })

    fun withReactionTexts(reactionTextProvider: ReactionTextProvider) = this.also {
        this.reactionTextProvider = reactionTextProvider
    }

    fun withReactionTexts(@ArrayRes res: Int) = this.also {
        reactionTextProvider = context.resources.getStringArray(res)::get
    }

    fun withReactionSize(reactionSize: Int) = this.also {
        this.reactionSize = reactionSize
    }

    fun withHorizontalMargin(horizontalMargin: Int) = this.also {
        this.horizontalMargin = horizontalMargin
    }

    fun withVerticalMargin(verticalMargin: Int) = this.also {
        this.verticalMargin = verticalMargin
    }

    fun withPopupGravity(popupGravity: PopupGravity) = this.also {
        this.popupGravity = popupGravity
    }

    fun withPopupMargin(popupMargin: Int) = this.also {
        this.popupMargin = popupMargin
    }

    fun withPopupCornerRadius(popupCornerRadius: Float) = this.also {
        this.popupCornerRadius = popupCornerRadius
    }

    fun withPopupColor(@ColorInt popupColor: Int) = this.also {
        this.popupColor = popupColor
    }

    fun withPopupAlpha(@IntRange(from = 0, to = 255) popupAlpha: Int) = this.also {
        this.popupAlpha = popupAlpha
    }

    fun withUpwardPopupMargin(value: Int) = this.also {
        this.upwardPopupMargin = value
    }

    fun withDownwardPopupMargin(value: Int) = this.also {
        this.downwardPopupMargin = value
    }

    fun withDownwardPopupPoint(value: Int) = this.also {
        this.downwardPopupPoint = value
    }

    fun withTextBackground(textBackground: Drawable) = this.also {
        this.textBackground = textBackground
    }

    fun withTextColor(@ColorInt textColor: Int) = this.also {
        this.textColor = textColor
    }

    fun withTextHorizontalPadding(textHorizontalPadding: Int) = this.also {
        this.textHorizontalPadding = textHorizontalPadding
    }

    fun withTextVerticalPadding(textVerticalPadding: Int) = this.also {
        this.textVerticalPadding = textVerticalPadding
    }

    fun withTextSize(textSize: Float) = this.also {
        this.textSize = textSize
    }

    fun withTypeface(typeface: Typeface?) = this.also {
        this.customTypeface = typeface
    }

    fun build() = ReactionsConfig(
        reactions = reactions.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Empty reactions"),
        popupGravity = popupGravity,
        popupMargin = popupMargin,
        popupCornerRadius = popupCornerRadius,
        popupElevation = popupElevation,
        popupColor = popupColor,
        upwardPopupMargin = upwardPopupMargin,
        downwardPopupMargin = downwardPopupMargin,
        downwardPopupPoint = downwardPopupPoint,
        popupAlphaValue = popupAlpha,
        reactionSize = reactionSize,
        horizontalMargin = horizontalMargin,
        verticalMargin = verticalMargin,
        reactionTextProvider = reactionTextProvider,
        textBackground = textBackground
            ?: ContextCompat.getDrawable(context, R.drawable.reactions_text_background)!!,
        textColor = textColor,
        textHorizontalPadding = textHorizontalPadding.takeIf { it != 0 }
            ?: context.resources.getDimension(R.dimen.reactions_text_horizontal_padding)
                .roundToInt(),
        textVerticalPadding = textVerticalPadding.takeIf { it != 0 }
            ?: context.resources.getDimension(R.dimen.reactions_text_vertical_padding).roundToInt(),
        textSize = textSize.takeIf { it != 0f }
            ?: context.resources.getDimension(R.dimen.reactions_text_size),
        typeface = customTypeface,
        scaleFactor = scaleFactor,
        targetX = targetX,
        targetY = targetY,
        dismissAnimationEnabled = dismissAnimationEnabled,
    )
}
