package com.gunzdev.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mengganti string di string-pool AndroidManifest biner (AXML). Dipakai untuk package name & label. */
final class AxmlPatcher {
    private AxmlPatcher() { }

    static byte[] patch(byte[] in, Map<String, String> replace) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(in).order(ByteOrder.LITTLE_ENDIAN);
        if (in.length < 36 || (b.getShort(0) & 0xffff) != 0x0003) throw new IOException("Bukan AXML valid");
        final int ps = 8;
        if ((b.getShort(ps) & 0xffff) != 0x0001) throw new IOException("String pool tidak ditemukan");
        int headerSize = b.getShort(ps + 2) & 0xffff;
        int poolSize = b.getInt(ps + 4);
        int count = b.getInt(ps + 8);
        int styleCount = b.getInt(ps + 12);
        int flags = b.getInt(ps + 16);
        int stringsStart = b.getInt(ps + 20);
        int stylesStart = b.getInt(ps + 24);
        boolean utf8 = (flags & 0x100) != 0;
        int offTable = ps + headerSize;

        List<String> strs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int p = ps + stringsStart + b.getInt(offTable + 4 * i);
            if (utf8) {
                int u16 = b.get(p++) & 0xff;
                if ((u16 & 0x80) != 0) { u16 = ((u16 & 0x7f) << 8) | (b.get(p++) & 0xff); }
                int u8 = b.get(p++) & 0xff;
                if ((u8 & 0x80) != 0) { u8 = ((u8 & 0x7f) << 8) | (b.get(p++) & 0xff); }
                strs.add(new String(in, p, u8, StandardCharsets.UTF_8));
            } else {
                int n = b.getShort(p) & 0xffff;
                p += 2;
                if ((n & 0x8000) != 0) { n = ((n & 0x7fff) << 16) | (b.getShort(p) & 0xffff); p += 2; }
                strs.add(new String(in, p, n * 2, StandardCharsets.UTF_16LE));
            }
        }

        for (Map.Entry<String, String> e : replace.entrySet()) {
            boolean found = false;
            for (int i = 0; i < strs.size(); i++) {
                if (strs.get(i).equals(e.getKey())) { strs.set(i, e.getValue()); found = true; }
            }
            if (!found) throw new IOException("String '" + e.getKey() + "' tidak ada di manifest template");
        }

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int[] offs = new int[count];
        for (int i = 0; i < count; i++) {
            offs[i] = data.size();
            String s = strs.get(i);
            if (utf8) {
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                len8(data, s.length());
                len8(data, bytes.length);
                data.write(bytes, 0, bytes.length);
                data.write(0);
            } else {
                int n = s.length();
                if (n > 0x7fff) { w16(data, (n >> 16) | 0x8000); w16(data, n & 0xffff); } else w16(data, n);
                byte[] bytes = s.getBytes(StandardCharsets.UTF_16LE);
                data.write(bytes, 0, bytes.length);
                w16(data, 0);
            }
        }
        while (data.size() % 4 != 0) data.write(0);

        byte[] styleOffs = new byte[styleCount * 4];
        byte[] styles = new byte[0];
        if (styleCount > 0) {
            System.arraycopy(in, offTable + 4 * count, styleOffs, 0, styleOffs.length);
            int end = ps + poolSize;
            styles = new byte[end - (ps + stylesStart)];
            System.arraycopy(in, ps + stylesStart, styles, 0, styles.length);
        }

        int newStringsStart = headerSize + count * 4 + styleCount * 4;
        int newStylesStart = styleCount > 0 ? newStringsStart + data.size() : 0;
        int newPoolSize = newStringsStart + data.size() + styles.length;

        ByteBuffer pool = ByteBuffer.allocate(newPoolSize).order(ByteOrder.LITTLE_ENDIAN);
        pool.put(in, ps, headerSize);           // header lama
        pool.putInt(4, newPoolSize);
        pool.putInt(20, newStringsStart);
        pool.putInt(24, newStylesStart);
        pool.position(headerSize);
        for (int o : offs) pool.putInt(o);
        pool.put(styleOffs);
        pool.put(data.toByteArray());
        pool.put(styles);

        int restStart = ps + poolSize;
        int total = ps + newPoolSize + (in.length - restStart);
        ByteBuffer out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        out.put(in, 0, ps);
        out.putInt(4, total);
        out.position(ps);
        out.put(pool.array());
        out.put(in, restStart, in.length - restStart);
        return out.array();
    }

    private static void len8(ByteArrayOutputStream o, int n) {
        if (n > 0x7f) { o.write((n >> 8) | 0x80); o.write(n & 0xff); } else o.write(n);
    }

    private static void w16(ByteArrayOutputStream o, int v) { o.write(v & 0xff); o.write((v >> 8) & 0xff); }
}
