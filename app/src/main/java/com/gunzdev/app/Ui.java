package com.gunzdev.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.TextView;

final class Ui {
    static final int BG = 0xFF0B1220, CARD = 0xFF131C2E, LINE = 0xFF263552,
            CYAN = 0xFF22D3EE, VIOLET = 0xFFA78BFA, TEXT = 0xFFE5E7EB, MUTED = 0xFF94A3B8;

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static GradientDrawable box(Context c, int color, float radius, boolean outline) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radius));
        if (outline) g.setStroke(dp(c, 1), LINE);
        return g;
    }

    static GradientDrawable gradient(Context c, float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{CYAN, VIOLET});
        g.setCornerRadius(dp(c, radius));
        return g;
    }

    static Button button(Context c, String text, boolean primary) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? BG : TEXT);
        b.setBackground(primary ? gradient(c, 14) : box(c, CARD, 14, true));
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        return b;
    }

    static TextView text(Context c, String t, float sp, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }
}
