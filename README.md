# Gunzdev  (Credit: Gunz)

APK untuk membuat APK lain. Upload ZIP project web (harus ada `index.html`) -> jadi APK yang bisa di-install.

## Cara pakai aplikasi
1. Buka Gunzdev -> animasi loading (bisa **Lewati**) -> **Create Project**.
2. Isi **nama aplikasi**. Package name terisi otomatis (`com.gunz.<nama>`).
3. Logo: pilih dari galeri (otomatis diperkecil/disesuaikan) atau kosongkan -> otomatis jadi alias (inisial nama).
4. **Upload ZIP** (index.html + aset) atau **1 file HTML** saja (jadi index.html).
5. **Build APK** -> lihat **Logs** -> **Download APK** (simpan) atau **Install**.

## Build Gunzdev lewat GitHub Actions
1. Upload isi folder ini ke repo GitHub (branch `main`).
2. Tab **Actions** -> workflow **Build Gunzdev** jalan otomatis.
3. Download artifact **Gunzdev** -> `app-debug.apk`.

## Build lewat Termux
```
pkg install unzip -y
unzip Gunzdev.zip && cd Gunzdev
bash scripts/termux-build.sh nama-repo
```
Script akan push ke GitHub, menunggu Actions selesai, lalu menyalin APK ke `/sdcard/Download/Gunzdev.apk`.
(Build Android langsung di Termux tidak disarankan karena SDK/aapt2 tidak tersedia stabil.)

## Catatan
- Kunci sign APK hasil ada di `app/src/main/assets/gunz.pk8` + `gunz.crt`. Ganti dengan kunci milik sendiri
  jika repo dibuat publik (`openssl` perintahnya ada di bagian bawah).
- APK hasil: WebView yang menampilkan `index.html` dari ZIP; permission hanya INTERNET.
- Logo: `logo-gunzdev.png` (G + V digabung).

Buat kunci baru:
```
openssl req -x509 -newkey rsa:2048 -nodes -keyout k.pem -out c.pem -days 36500 -subj "/CN=Gunzdev"
openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in k.pem -out app/src/main/assets/gunz.pk8
openssl x509 -outform DER -in c.pem -out app/src/main/assets/gunz.crt
```

## Upload ke GitHub
Ekstrak ZIP ini, lalu upload SEMUA isinya (termasuk folder `.github`) ke root repo. Dari HP lebih aman pakai
Termux: `bash scripts/termux-build.sh nama-repo` (folder tersembunyi `.github` ikut ter-push).
Catatan: file HTML tunggal tidak membawa gambar/CSS/JS terpisah, gunakan ZIP jika project punya aset.

## Kompatibilitas Android
- Gunzdev: minSdk 26 (Android 8.0) sampai Android 15, targetSdk 34 (tampilan tidak dipaksa edge-to-edge di Android 15).
- APK hasil buatan Gunzdev: minSdk 21, targetSdk 34, ditandatangani v1+v2 (diwajibkan Android 11+), resources.arsc ter-align.
- Workflow Actions mengecek nilai SDK ini otomatis; build gagal jika tidak sesuai.
