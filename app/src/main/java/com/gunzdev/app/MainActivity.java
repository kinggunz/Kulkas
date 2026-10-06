package com.gunzdev.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
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
        int p = Ui.dp(this, 32);
        col.setPadding(p, 0, p, 0);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        col.addView(new LogoView(this), new LinearLayout.LayoutParams(Ui.dp(this, 120), Ui.dp(this, 120)));
        TextView t = Ui.text(this, "GUNZDEV", 24, Ui.TEXT, true);
        t.setLetterSpacing(0.2f);
        col.addView(t);
        TextView s = Ui.text(this, "Ubah project web (ZIP) jadi APK", 13, Ui.MUTED, false);
        s.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 40));
        s.setGravity(Gravity.CENTER);
        col.addView(s);

        Button create = Ui.button(this, "+  Create Project", true);
        create.setTextSize(18);
        create.setPadding(Ui.dp(this, 28), Ui.dp(this, 18), Ui.dp(this, 28), Ui.dp(this, 18));
        create.setOnClickListener(v -> startActivity(new Intent(this, CreateActivity.class)));
        col.addView(create, new LinearLayout.LayoutParams(-1, -2));

        TextView credit = Ui.text(this, "Credit : Gunz", 12, Ui.MUTED, false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Ui.dp(this, 24);
        root.addView(credit, lp);
        setContentView(root);
    }
}
