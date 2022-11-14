package com.github.pgreze.reactions.sample

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import com.airbnb.lottie.LottieAnimationView
import com.github.pgreze.reactions.*
import com.github.pgreze.reactions.dsl.reactionConfig
import com.github.pgreze.reactions.dsl.reactionPopup
import com.github.pgreze.reactions.dsl.reactions

fun MainActivity.setupTopRight() {
    // Popup DSL + listener via function
    val popup1 = reactionPopup(this, ::onReactionSelected, ::onReactionPopupStateChanged) {
        reactions {
            resId { R.drawable.ic_crypto_btc }
            resId { R.drawable.ic_crypto_eth }
            resId { R.drawable.ic_crypto_ltc }
            reaction { R.drawable.ic_crypto_dash scale ImageView.ScaleType.FIT_CENTER }
            reaction { R.drawable.ic_crypto_xrp scale ImageView.ScaleType.FIT_CENTER }
            resId { R.drawable.ic_crypto_xmr }
            resId { R.drawable.ic_crypto_doge }
            resId { R.drawable.ic_crypto_steem }
            resId { R.drawable.ic_crypto_kmd }
            resId { R.drawable.ic_crypto_zec }
        }
        reactionTexts = R.array.crypto_symbols
        popupCornerRadius = 40
        popupColor = Color.LTGRAY
        popupAlpha = 255
        reactionSize = resources.getDimensionPixelSize(R.dimen.crypto_item_size)
        horizontalMargin = resources.getDimensionPixelSize(R.dimen.crypto_item_margin)
        verticalMargin = horizontalMargin / 2
    }
    // Setter also available
    popup1.reactionSelectedListener = { position ->
        toast("$position selected")
        true
    }
    findViewById<View>(R.id.top_right_btn).setOnTouchListener(popup1)
}

fun MainActivity.setupRight() {
    // Config DSL + listener in popup constructor
    val config = reactionConfig(this) {
        reactionsIds = intArrayOf(
            R.drawable.ic_crypto_btc,
            R.drawable.ic_crypto_eth,
            R.drawable.ic_crypto_ltc,
            R.drawable.ic_crypto_dash,
            R.drawable.ic_crypto_xrp,
            R.drawable.ic_crypto_xmr,
            R.drawable.ic_crypto_doge,
            R.drawable.ic_crypto_steem,
            R.drawable.ic_crypto_kmd,
            R.drawable.ic_crypto_zec
        )
        /*
        withReactions(listOf(
            Reaction(
                onView = { ctx -> LottieAnimationView(ctx).apply {
                    setAnimation(R.raw.love)
                    playAnimation()
                    repeatCount = ValueAnimator.INFINITE
                } },
            ),
            Reaction(
                onView = { ctx -> LottieAnimationView(ctx).apply {
                    setAnimation(R.raw.cheer)
                    playAnimation()
                    repeatCount = ValueAnimator.INFINITE
                } },
            ),
            Reaction(
                onView = { ctx -> LottieAnimationView(ctx).apply {
                    setAnimation(R.raw.rocket)
                    playAnimation()
                    repeatCount = ValueAnimator.INFINITE
                } },
            ),
            Reaction(
                onView = { ctx -> LottieAnimationView(ctx).apply {
                    setAnimation(R.raw.like)
                    playAnimation()
                    repeatCount = ValueAnimator.INFINITE
                } },
            ),
            Reaction(
                onView = { ctx -> LottieAnimationView(ctx).apply {
                    setAnimation(R.raw.happy)
                    playAnimation()
                    repeatCount = ValueAnimator.INFINITE
                } },
            ),
        ))
        */
        reactionTextProvider = { position -> "Item $position" }
        popupGravity = PopupGravity.PARENT_RIGHT
        popupMargin = resources.getDimensionPixelSize(R.dimen.crypto_item_size)
        textBackground = ColorDrawable(Color.TRANSPARENT)
        textColor = Color.BLACK
        textHorizontalPadding = 0
        textVerticalPadding = 0
        textSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        popupAlpha = 255
    }
    val popup2 = ReactionPopup(this, config, object : ReactionSelectedListener {
        override fun invoke(position: Int): Boolean = true.also {
            toast("$position selected")
        }
    }, object : ReactionPopupStateChangeListener {
        override fun invoke(isShowing: Boolean) = toast("Popup is showing => $isShowing")
    })
    findViewById<View>(R.id.right_btn).setOnTouchListener(popup2)
}

fun MainActivity.onReactionSelected(position: Int) = true.also {
    toast("$position selected")
}

fun MainActivity.onReactionPopupStateChanged(isShowing: Boolean) =
    toast("Popup is showing => $isShowing")

fun MainActivity.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT)
        .apply { setGravity(Gravity.CENTER, 0, 300) }
        .show()
}

@Suppress("DEPRECATION")
fun MainActivity.drawable(@DrawableRes id: Int): Drawable =
    resources.getDrawable(id)
