package com.gunzdev.app;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;

import com.android.apksig.ApkSigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Membuat APK baru: template.apk (WebView) + isi ZIP user + nama/package/logo, lalu sign. Semua di perangkat. */
final class ApkBuilder {
    interface Log { void log(String msg); }

    static final String OLD_PKG = "com.gunzdev.shell", OLD_LABEL = "GunzShell";

    private final Context ctx;
    private final Log log;

    ApkBuilder(Context ctx, Log log) { this.ctx = ctx; this.log = log; }

    // ---------- helper publik ----------

    static String makePackage(String name) {
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (s.isEmpty()) s = "app";
        if (Character.isDigit(s.charAt(0))) s = "a" + s;
        if (s.length() > 20) s = s.substring(0, 20);
        return "com.gunz." + s;
    }

    static Bitmap aliasIcon(String name, int size) {
        String n = name == null ? "" : name.trim();
        Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.HSVToColor(new float[]{(n.hashCode() & 0x7fffffff) % 360, 0.55f, 0.72f}));
        float r = size * 0.22f;
        c.drawRoundRect(new RectF(0, 0, size, size), r, r, p);
        StringBuilder ini = new StringBuilder();
        for (String w : n.split("\\s+")) {
            if (!w.isEmpty() && ini.length() < 2) ini.append(Character.toUpperCase(w.charAt(0)));
        }
        if (ini.length() == 0) ini.append("A");
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setTextSize(size * (ini.length() > 1 ? 0.42f : 0.52f));
        Paint.FontMetrics fm = p.getFontMetrics();
        c.drawText(ini.toString(), size / 2f, size / 2f - (fm.ascent + fm.descent) / 2f, p);
        return bm;
    }

    /** Decode gambar besar dengan aman lalu muat ke kotak size x size (proporsi tetap, latar transparan). */
    static Bitmap fitIcon(ContentResolver cr, Uri uri, int size) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) { BitmapFactory.decodeStream(in, null, o); }
        if (o.outWidth <= 0 || o.outHeight <= 0) throw new IOException("File bukan gambar yang valid");
        int s = 1;
        while (Math.max(o.outWidth, o.outHeight) / s > 1024) s *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = s;
        Bitmap src;
        try (InputStream in = cr.openInputStream(uri)) { src = BitmapFactory.decodeStream(in, null, o); }
        if (src == null) throw new IOException("Gagal membaca gambar");
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        float sc = Math.min((float) size / src.getWidth(), (float) size / src.getHeight());
        float dw = src.getWidth() * sc, dh = src.getHeight() * sc;
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        new Canvas(out).drawBitmap(src, null, new RectF((size - dw) / 2, (size - dh) / 2, (size + dw) / 2, (size + dh) / 2), p);
        src.recycle();
        return out;
    }

    // ---------- proses build ----------

    File build(String name, String pkg, Bitmap logo, Uri zip) throws Exception {
        log.log("Mulai build \"" + name + "\"");
        log.log("Package: " + pkg);

        final boolean zipIn = isZip(zip);
        String root = "";
        if (zipIn) {
            log.log("Membaca ZIP project...");
            root = findRoot(zip);
            log.log("index.html ditemukan (root: " + (root.isEmpty() ? "/" : root) + ")");
        } else {
            log.log("File HTML tunggal -> dijadikan index.html");
        }

        byte[] icon = png(logo != null ? logo : aliasIcon(name, 192));
        log.log(logo != null ? "Logo: dari galeri (disesuaikan 192x192)" : "Logo: alias otomatis dari nama");

        File dir = new File(ctx.getCacheDir(), "build");
        deleteRec(dir);
        if (!dir.mkdirs()) throw new IOException("Gagal membuat folder kerja");
        File unsigned = new File(dir, "unsigned.apk");

        Map<String, String> map = new HashMap<>();
        map.put(OLD_PKG, pkg);
        map.put(OLD_LABEL, name);

        int icons = 0, files = 0;
        log.log("Menyiapkan template...");
        try (ZipOut z = new ZipOut(new FileOutputStream(unsigned))) {
            try (ZipInputStream zi = new ZipInputStream(ctx.getAssets().open("template.apk"))) {
                ZipEntry e;
                while ((e = zi.getNextEntry()) != null) {
                    if (e.isDirectory()) continue;
                    String n = e.getName();
                    if (n.startsWith("META-INF/") || n.startsWith("assets/www/")) continue;
                    byte[] d = readAll(zi);
                    if (n.equals("AndroidManifest.xml")) {
                        d = AxmlPatcher.patch(d, map);
                        log.log("Manifest: nama & package diganti");
                    } else if (n.startsWith("res/") && isPng(d)) {
                        // template hanya punya 1 PNG di res/ = ikon (nama file bisa diperpendek oleh Gradle)
                        d = icon;
                        icons++;
                        log.log("  ikon: " + n);
                    }
                    z.put(n, d, n.equals("resources.arsc"));
                }
            }
            log.log(icons > 0 ? "Logo dipasang (" + icons + " file ikon)" : "PERINGATAN: file ikon template tidak ditemukan");

            log.log("Memasukkan file project...");
            Set<String> seen = new HashSet<>();
            if (!zipIn) {
                try (InputStream in = ctx.getContentResolver().openInputStream(zip)) {
                    byte[] html = readAll(in);
                    if (html.length == 0) throw new IOException("File HTML kosong");
                    z.put("assets/www/index.html", html, false);
                    files++;
                }
            }
            try (ZipInputStream zi = new ZipInputStream(zipIn
                    ? ctx.getContentResolver().openInputStream(zip) : new ByteArrayInputStream(new byte[0]))) {
                ZipEntry e;
                while ((e = zi.getNextEntry()) != null) {
                    if (e.isDirectory()) continue;
                    String rel = cleanRel(e.getName(), root);
                    if (rel == null) continue;
                    String out = "assets/www/" + rel;
                    if (!seen.add(out)) continue;
                    z.put(out, readAll(zi), false);
                    files++;
                    if (files <= 8) log.log("  + " + rel);
                }
            }
            if (files > 8) log.log("  ... dan " + (files - 8) + " file lainnya");
            log.log(files + " file dimasukkan");
        }

        log.log("Menandatangani APK (v1+v2)...");
        File out = new File(ctx.getFilesDir(), "out");
        if (!out.exists() && !out.mkdirs()) throw new IOException("Gagal membuat folder output");
        File signed = new File(out, name.replaceAll("[^A-Za-z0-9_-]", "_") + ".apk");
        if (signed.exists() && !signed.delete()) throw new IOException("Gagal menimpa APK lama");

        PrivateKey key;
        try (InputStream in = ctx.getAssets().open("gunz.pk8")) {
            key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(readAll(in)));
        }
        X509Certificate cert;
        try (InputStream in = ctx.getAssets().open("gunz.crt")) {
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        ApkSigner.SignerConfig sc = new ApkSigner.SignerConfig.Builder("gunz", key, Collections.singletonList(cert)).build();
        new ApkSigner.Builder(Collections.singletonList(sc))
                .setInputApk(unsigned)
                .setOutputApk(signed)
                .setMinSdkVersion(21)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                .sign();

        log.log("Memeriksa hasil APK...");
        try (ZipFile zf = new ZipFile(signed)) {
            boolean sf = false;
            for (java.util.Enumeration<? extends ZipEntry> en = zf.entries(); en.hasMoreElements(); ) {
                if (en.nextElement().getName().endsWith(".SF")) sf = true;
            }
            if (zf.getEntry("AndroidManifest.xml") == null || zf.getEntry("classes.dex") == null || !sf)
                throw new IOException("APK hasil tidak lengkap (manifest/dex/signature)");
        }
        deleteRec(dir);

        log.log("SELESAI: " + signed.getName() + " (" + (signed.length() / 1024) + " KB)");
        return signed;
    }

    // ---------- util ----------

    private boolean isZip(Uri u) throws IOException {
        try (InputStream in = ctx.getContentResolver().openInputStream(u)) {
            byte[] h = new byte[4];
            int n = 0, r;
            while (n < 4 && (r = in.read(h, n, 4 - n)) > 0) n += r;
            return n == 4 && h[0] == 'P' && h[1] == 'K' && (h[2] == 3 || h[2] == 5);
        }
    }

    private String findRoot(Uri zip) throws IOException {
        String best = null;
        int bestDepth = Integer.MAX_VALUE;
        try (ZipInputStream zi = new ZipInputStream(ctx.getContentResolver().openInputStream(zip))) {
            ZipEntry e;
            while ((e = zi.getNextEntry()) != null) {
                String n = e.getName().replace('\\', '/');
                if (e.isDirectory() || n.startsWith("__MACOSX/")) continue;
                if (n.equals("index.html") || n.endsWith("/index.html")) {
                    int depth = n.split("/").length;
                    if (depth < bestDepth) { bestDepth = depth; best = n.substring(0, n.length() - "index.html".length()); }
                }
            }
        }
        if (best == null) throw new IOException("index.html tidak ada di dalam ZIP. Pastikan project web punya index.html");
        return best;
    }

    private static String cleanRel(String entry, String root) {
        String n = entry.replace('\\', '/');
        if (!n.startsWith(root)) return null;
        String rel = n.substring(root.length());
        if (rel.isEmpty() || rel.startsWith("/") || rel.contains("../") || rel.startsWith("__MACOSX/")
                || rel.endsWith(".DS_Store")) return null;
        return rel;
    }

    private static boolean isPng(byte[] d) {
        return d.length > 8 && (d[0] & 0xff) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
    }

    private static byte[] png(Bitmap b) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, o);
        return o.toByteArray();
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        return o.toByteArray();
    }

    private static void deleteRec(File f) {
        File[] ch = f.listFiles();
        if (ch != null) for (File c : ch) deleteRec(c);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
