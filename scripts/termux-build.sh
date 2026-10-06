#!/data/data/com.termux/files/usr/bin/bash
# Build Gunzdev lewat Termux: push ke GitHub -> GitHub Actions build -> APK di-download otomatis.
# Pemakaian:  bash scripts/termux-build.sh [nama-repo]
set -e
cd "$(dirname "$0")/.."
REPO="${1:-gunzdev}"

pkg update -y && pkg install -y git gh
gh auth status >/dev/null 2>&1 || gh auth login

[ -d .git ] || git init -q -b main
git add -A
git -c user.name="gunz" -c user.email="gunz@local" commit -qm "Gunzdev build" || true
git remote | grep -q origin || gh repo create "$REPO" --private --source=. --remote=origin
git push -u origin HEAD:main --force

echo "Menunggu GitHub Actions..."
sleep 10
RUN=$(gh run list --workflow build.yml -L 1 --json databaseId -q '.[0].databaseId')
gh run watch "$RUN" --exit-status

OUT="$HOME/Gunzdev-apk"
rm -rf "$OUT"; mkdir -p "$OUT"
gh run download "$RUN" -n Gunzdev -D "$OUT"
[ -d /sdcard/Download ] && cp "$OUT/app-debug.apk" /sdcard/Download/Gunzdev.apk && echo "Tersalin ke /sdcard/Download/Gunzdev.apk"
echo "SELESAI: $OUT/app-debug.apk"
