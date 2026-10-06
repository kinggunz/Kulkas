package com.gunzdev.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateActivity extends Activity {
    private static final int REQ_LOGO = 11, REQ_ZIP = 12, REQ_SAVE = 13;

    private EditText nameEt;
    private ImageView preview;
    private TextView pkgTv, zipTv, logTv;
    private ScrollView scroll;
    private Button buildBtn;
    private LinearLayout resultRow;
    private Bitmap logo;
    private Uri zipUri;
    private File outApk;
    private volatile boolean busy;
    private final SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss", Locale.US);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 20);
        col.setPadding(pad, Ui.dp(this, 30), pad, pad);
        scroll.addView(col);

        col.addView(Ui.text(this, "Create Project", 26, Ui.TEXT, true));
        col.addView(label("Nama aplikasi"));
        nameEt = new EditText(this);
        nameEt.setHint("contoh: Toko Saya");
        nameEt.setHintTextColor(Ui.MUTED);
        nameEt.setTextColor(Ui.TEXT);
        nameEt.setSingleLine();
        nameEt.setBackground(Ui.box(this, Ui.CARD, 12, true));
        nameEt.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        nameEt.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            public void onTextChanged(CharSequence s, int a, int c, int d) { }
            public void afterTextChanged(Editable s) { refresh(); }
        });
        col.addView(nameEt, new LinearLayout.LayoutParams(-1, -2));

        col.addView(label("Logo (opsional)"));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        preview = new ImageView(this);
        preview.setBackground(Ui.box(this, Ui.CARD, 14, true));
        preview.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
        row.addView(preview, new LinearLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 76)));
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.VERTICAL);
        btns.setPadding(Ui.dp(this, 12), 0, 0, 0);
        Button pick = Ui.button(this, "Pilih dari galeri", false);
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
            startActivityForResult(i, REQ_LOGO);
        });
        Button reset = Ui.button(this, "Pakai alias otomatis", false);
        reset.setOnClickListener(v -> { logo = null; refresh(); log("Logo direset ke alias otomatis"); });
        btns.addView(pick, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
        rl.topMargin = Ui.dp(this, 6);
        btns.addView(reset, rl);
        row.addView(btns, new LinearLayout.LayoutParams(0, -2, 1f));
        col.addView(row, new LinearLayout.LayoutParams(-1, -2));
        TextView hint = Ui.text(this, "Kosong = otomatis jadi alias (inisial nama). Gambar besar disesuaikan otomatis.", 12, Ui.MUTED, false);
        hint.setPadding(0, Ui.dp(this, 6), 0, 0);
        col.addView(hint);

        col.addView(label("Package name (otomatis)"));
        pkgTv = Ui.text(this, "", 14, Ui.CYAN, false);
        pkgTv.setTypeface(Typeface.MONOSPACE);
        pkgTv.setBackground(Ui.box(this, Ui.CARD, 12, true));
        pkgTv.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        col.addView(pkgTv, new LinearLayout.LayoutParams(-1, -2));

        col.addView(label("Project (ZIP berisi index.html, atau 1 file HTML)"));
        Button zipBtn = Ui.button(this, "Upload ZIP / HTML", false);
        zipBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "text/html", "application/octet-stream"});
            startActivityForResult(i, REQ_ZIP);
        });
        col.addView(zipBtn, new LinearLayout.LayoutParams(-1, -2));
        zipTv = Ui.text(this, "Belum ada file", 12, Ui.MUTED, false);
        zipTv.setPadding(0, Ui.dp(this, 6), 0, 0);
        col.addView(zipTv);

        buildBtn = Ui.button(this, "Build APK", true);
        buildBtn.setTextSize(17);
        buildBtn.setOnClickListener(v -> startBuild());
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
        bl.topMargin = Ui.dp(this, 22);
        col.addView(buildBtn, bl);

        col.addView(label("Logs"));
        logTv = Ui.text(this, "Menunggu...", 12, 0xFF86EFAC, false);
        logTv.setTypeface(Typeface.MONOSPACE);
        logTv.setBackground(Ui.box(this, 0xFF060B14, 12, true));
        logTv.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        logTv.setMinHeight(Ui.dp(this, 150));
        col.addView(logTv, new LinearLayout.LayoutParams(-1, -2));

        resultRow = new LinearLayout(this);
        resultRow.setVisibility(View.GONE);
        Button save = Ui.button(this, "Download APK", true);
        save.setOnClickListener(v -> saveApk());
        Button install = Ui.button(this, "Install", false);
        install.setOnClickListener(v -> installApk());
        resultRow.addView(save, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(0, -2, 1f);
        il.leftMargin = Ui.dp(this, 10);
        resultRow.addView(install, il);
        LinearLayout.LayoutParams rr = new LinearLayout.LayoutParams(-1, -2);
        rr.topMargin = Ui.dp(this, 14);
        col.addView(resultRow, rr);

        TextView credit = Ui.text(this, "Credit : Gunz", 12, Ui.MUTED, false);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, Ui.dp(this, 28), 0, 0);
        col.addView(credit, new LinearLayout.LayoutParams(-1, -2));

        setContentView(scroll);
        refresh();
    }

    private TextView label(String t) {
        TextView v = Ui.text(this, t, 13, Ui.MUTED, true);
        v.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 6));
        return v;
    }

    private String name() { return nameEt.getText().toString().trim(); }

    private void refresh() {
        pkgTv.setText(ApkBuilder.makePackage(name()));
        preview.setImageBitmap(logo != null ? logo : ApkBuilder.aliasIcon(name(), 192));
    }

    private void log(String msg) {
        runOnUiThread(() -> {
            String cur = logTv.getText().toString();
            if (cur.equals("Menunggu...")) cur = "";
            logTv.setText(cur + "[" + tf.format(new Date()) + "] " + msg + "\n");
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void startBuild() {
        if (busy) return;
        final String n = name();
        if (n.isEmpty()) { Toast.makeText(this, "Isi nama aplikasi dulu", Toast.LENGTH_SHORT).show(); return; }
        if (zipUri == null) { Toast.makeText(this, "Upload ZIP atau file HTML dulu", Toast.LENGTH_SHORT).show(); return; }
        busy = true;
        buildBtn.setEnabled(false);
        buildBtn.setText("Sedang build...");
        resultRow.setVisibility(View.GONE);
        logTv.setText("");
        final String pkg = ApkBuilder.makePackage(n);
        final Bitmap lg = logo;
        final Uri zip = zipUri;
        new Thread(() -> {
            try {
                File f = new ApkBuilder(getApplicationContext(), this::log).build(n, pkg, lg, zip);
                outApk = f;
                runOnUiThread(() -> resultRow.setVisibility(View.VISIBLE));
            } catch (Throwable t) {
                log("GAGAL: " + (t.getMessage() != null ? t.getMessage() : t.toString()));
            } finally {
                busy = false;
                runOnUiThread(() -> { buildBtn.setEnabled(true); buildBtn.setText("Build APK"); });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (res != RESULT_OK || d == null || d.getData() == null) return;
        final Uri u = d.getData();
        if (rc == REQ_LOGO) {
            new Thread(() -> {
                try {
                    Bitmap bm = ApkBuilder.fitIcon(getContentResolver(), u, 192);
                    runOnUiThread(() -> { logo = bm; refresh(); });
                    log("Logo dipilih, otomatis disesuaikan ke 192x192");
                } catch (Throwable t) {
                    log("Logo gagal: " + t.getMessage());
                }
            }).start();
        } else if (rc == REQ_ZIP) {
            zipUri = u;
            String fn = displayName(u);
            zipTv.setText(fn);
            log("File dipilih: " + fn);
        } else if (rc == REQ_SAVE && outApk != null) {
            try (InputStream in = new FileInputStream(outApk); OutputStream o = getContentResolver().openOutputStream(u)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
                Toast.makeText(this, "APK tersimpan", Toast.LENGTH_SHORT).show();
                log("APK disimpan");
            } catch (Exception e) {
                log("Gagal menyimpan: " + e.getMessage());
            }
        }
    }

    private String displayName(Uri u) {
        try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) { }
        return "project";
    }

    private void saveApk() {
        if (outApk == null) return;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/vnd.android.package-archive").putExtra(Intent.EXTRA_TITLE, outApk.getName());
        startActivityForResult(i, REQ_SAVE);
    }

    private void installApk() {
        if (outApk == null) return;
        if (!getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(this, "Izinkan Gunzdev memasang aplikasi, lalu tekan Install lagi", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
            return;
        }
        Uri u = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", outApk);
        startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }
}
