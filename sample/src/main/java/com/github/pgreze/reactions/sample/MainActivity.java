package com.github.pgreze.reactions.sample;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.github.pgreze.reactions.PopupGravity;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfigBuilder;

public class MainActivity extends AppCompatActivity {

    private final String[] strings = {"like", "love", "laugh", "wow", "sad", "angry"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sampleCenterLeft();
        sampleTopLeft();
        sampleBottomLeft();
        KotlinSamplesKt.setupTopRight(this);
        KotlinSamplesKt.setupRight(this);
    }

    private void sampleCenterLeft() {
        ReactionPopup popup = new ReactionPopup(
                this,
                new ReactionsConfigBuilder(this)
                        .withReactions(new int[]{
                                R.drawable.ic_fb_like,
                                R.drawable.ic_fb_love,
                                R.drawable.ic_fb_laugh,
                                R.drawable.ic_fb_wow,
                                R.drawable.ic_fb_sad,
                                R.drawable.ic_fb_angry,
                        })
                        .withReactionTexts(position -> strings[position])
                        .withPopupGravity(PopupGravity.CENTER)
                        .withHorizontalMargin(50)
                        .build());

        findViewById(R.id.facebook_btn).setOnTouchListener(popup);
    }

    private void sampleTopLeft() {
        ReactionPopup popup = new ReactionPopup(
                this,
                new ReactionsConfigBuilder(this)
                        .withReactions(new int[]{
                                R.drawable.ic_fb_like,
                                R.drawable.ic_fb_love,
                                R.drawable.ic_fb_laugh,
                        })
                        .withPopupAlpha(20)
                        .withReactionTexts(position -> strings[position])
                        .withTextBackground(new ColorDrawable(Color.TRANSPARENT))
                        .withTextColor(Color.BLACK)
                        .withTextHorizontalPadding(0)
                        .withTextVerticalPadding(0)
                        .withTextSize(getResources().getDimension(R.dimen.reactions_text_size))
                        .build(),
                position -> true);

        findViewById(R.id.top_btn).setOnTouchListener(popup);
    }

    private void sampleBottomLeft() {
        int margin = getResources().getDimensionPixelSize(R.dimen.crypto_item_margin);

        ReactionPopup popup = new ReactionPopup(this, new ReactionsConfigBuilder(this)
                .withReactions(new int[]{
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
                })
                .withReactionTexts(R.array.crypto_symbols)
                .withPopupColor(Color.LTGRAY)
                .withReactionSize(getResources().getDimensionPixelSize(R.dimen.crypto_item_size))
                .withHorizontalMargin(margin)
                .withVerticalMargin(margin / 2)
                .withTextBackground(new ColorDrawable(Color.TRANSPARENT))
                .withTextColor(Color.BLACK)
                .withTextSize(getResources().getDimension(R.dimen.reactions_text_size) * 1.5f)
                .build());

        popup.setReactionSelectedListener((position) -> {
            Log.i("Reactions", "Selection position=" + position);
            // Close selector if not invalid item (testing purpose)
            return position != 3;
        });

        findViewById(R.id.crypto_bottom_left).setOnTouchListener(popup);
    }
}
