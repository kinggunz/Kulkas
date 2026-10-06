package com.gunzdev.app;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** ZIP writer: entry STORED otomatis di-align 4 byte (wajib untuk resources.arsc di Android 11+). */
final class ZipOut implements Closeable {
    private static final class Count extends FilterOutputStream {
        long n;
        Count(OutputStream o) { super(o); }
        @Override public void write(int b) throws IOException { out.write(b); n++; }
        @Override public void write(byte[] b, int off, int len) throws IOException { out.write(b, off, len); n += len; }
    }

    private final Count count;
    private final ZipOutputStream zos;

    ZipOut(OutputStream os) {
        count = new Count(os);
        zos = new ZipOutputStream(count);
        zos.setLevel(6);
    }

    void put(String name, byte[] data, boolean stored) throws IOException {
        ZipEntry e = new ZipEntry(name);
        if (stored) {
            e.setMethod(ZipEntry.STORED);
            e.setSize(data.length);
            e.setCompressedSize(data.length);
            CRC32 crc = new CRC32();
            crc.update(data);
            e.setCrc(crc.getValue());
            long base = count.n + 30 + name.getBytes(StandardCharsets.UTF_8).length;
            int pad = (int) ((4 - base % 4) % 4);
            if (pad > 0 && pad < 4) pad += 4;
            if (pad > 0) {
                byte[] ex = new byte[pad];
                ex[0] = (byte) 0x35;
                ex[1] = (byte) 0xD9;
                ex[2] = (byte) (pad - 4);
                e.setExtra(ex);
            }
        } else {
            e.setMethod(ZipEntry.DEFLATED);
        }
        zos.putNextEntry(e);
        zos.write(data);
        zos.closeEntry();
    }

    @Override public void close() throws IOException { zos.close(); }
}
