@file:Suppress("PackageDirectoryMismatch")

package com.github.pgreze.reactions.dsl

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.github.pgreze.reactions.Reaction
import com.github.pgreze.reactions.ReactionPopup
import com.github.pgreze.reactions.ReactionPopupStateChangeListener
import com.github.pgreze.reactions.ReactionSelectedListener
import com.github.pgreze.reactions.ReactionsConfig
import com.github.pgreze.reactions.ReactionsConfigBuilder
import androidx.core.content.ContextCompat.getDrawable as getDrawableCompat

fun reactionPopup(
    context: Context,
    reactionSelectedListener: ReactionSelectedListener? = null,
    reactionPopupStateChangeListener: ReactionPopupStateChangeListener? = null,
    init: ReactionsConfigBuilder.() -> Unit
): ReactionPopup = ReactionPopup(
    context,
    reactionConfig(context, init),
    reactionSelectedListener,
    reactionPopupStateChangeListener
)

fun reactionConfig(
    context: Context,
    init: ReactionsConfigBuilder.() -> Unit
): ReactionsConfig = ReactionsConfigBuilder(context)
    .apply(init)
    .build()

/** Reaction declaration block */
fun ReactionsConfigBuilder.reactions(
    scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    config: ReactionsConfiguration.() -> Unit
) {
    withReactions(mutableListOf<Reaction>().also {
        ReactionsConfiguration(context, scaleType, it).apply(config)
    })
}

class ReactionsConfiguration(
    private val context: Context,
    private val scaleType: ImageView.ScaleType,
    private val reactions: MutableList<Reaction>
) {
    fun resId(block: () -> Int) {
        reactions += Reaction(
            onView = { ImageView(it).apply {
                //ContextCompat.getDrawable(it, block())!!
                setImageResource(block())
                scaleType = scaleType
            } },
            scaleType = scaleType,
        )
    }

    fun reaction(block: ReactionBuilderBlock.() -> Reaction) {
        reactions += ReactionBuilderBlock(context).run(block)
    }
}

class ReactionBuilderBlock(private val context: Context) {

    infix fun Int.scale(scaleType: ImageView.ScaleType) = Reaction(
        onView = { ImageView(it).also {
            //ContextCompat.getDrawable(it, block())!!
            it.setImageResource(this)
        } },
        scaleType = scaleType
    )
}
