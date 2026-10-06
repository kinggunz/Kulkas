package com.gunzdev.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.view.View;

/** Logo Gunzdev: huruf G + V digabung. Bisa digambar bertahap (animasi). Viewport 108x108. */
public class LogoView extends View {
    private final Path g = new Path(), v = new Path(), tmp = new Path();
    private final Paint pg = new Paint(Paint.ANTI_ALIAS_FLAG), pv = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PathMeasure mg, mv;
    private float progress = 1f;

    public LogoView(Context c) {
        super(c);
        g.arcTo(new RectF(30, 30, 78, 78), -35f, -325f, true);
        g.lineTo(68, 54);
        v.moveTo(43, 45);
        v.lineTo(54, 65);
        v.lineTo(65, 45);
        mg = new PathMeasure(g, false);
        mv = new PathMeasure(v, false);
        for (Paint p : new Paint[]{pg, pv}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(7);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        pg.setColor(Ui.CYAN);
        pv.setColor(Ui.VIOLET);
    }

    public void setProgress(float p) { progress = p; invalidate(); }

    private static float clamp(float x) { return x < 0 ? 0 : (x > 1 ? 1 : x); }

    private void part(Canvas c, Path full, PathMeasure m, float f, Paint p) {
        if (f <= 0) return;
        if (f >= 1) { c.drawPath(full, p); return; }
        tmp.rewind();
        m.getSegment(0, m.getLength() * f, tmp, true);
        c.drawPath(tmp, p);
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 108f;
        c.save();
        c.translate((getWidth() - 108 * s) / 2f, (getHeight() - 108 * s) / 2f);
        c.scale(s, s);
        part(c, g, mg, clamp(progress / 0.65f), pg);
        part(c, v, mv, clamp((progress - 0.55f) / 0.45f), pv);
        c.restore();
    }
}
