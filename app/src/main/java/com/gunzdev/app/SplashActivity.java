package com.gunzdev.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private boolean left;
    private ValueAnimator anim;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        final LogoView logo = new LogoView(this);
        col.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 190), Ui.dp(this, 190)));

        final TextView title = Ui.text(this, "GUNZDEV", 28, Ui.TEXT, true);
        title.setLetterSpacing(0.25f);
        title.setAlpha(0f);
        col.addView(title);
        TextView sub = Ui.text(this, "APK Builder", 13, Ui.MUTED, false);
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 28));
        col.addView(sub);

        final ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(1000);
        pb.setProgressTintList(ColorStateList.valueOf(Ui.CYAN));
        pb.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.LINE));
        col.addView(pb, new LinearLayout.LayoutParams(Ui.dp(this, 170), Ui.dp(this, 6)));
        TextView loading = Ui.text(this, "Memuat...", 12, Ui.MUTED, false);
        loading.setPadding(0, Ui.dp(this, 10), 0, 0);
        col.addView(loading);

        TextView skip = Ui.text(this, "Lewati  \u203A", 14, Ui.CYAN, true);
        skip.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 12));
        skip.setOnClickListener(v -> go());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        lp.topMargin = Ui.dp(this, 36);
        root.addView(skip, lp);

        setContentView(root);

        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(2800);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            logo.setProgress(f);
            pb.setProgress((int) (f * 1000));
            title.setAlpha(Math.max(0f, Math.min(1f, (f - 0.45f) / 0.35f)));
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) { go(); }
        });
        anim.start();
    }

    private void go() {
        if (left) return;
        left = true;
        if (anim != null) anim.cancel();
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        left = true;
        if (anim != null) anim.cancel();
        super.onDestroy();
    }
}
