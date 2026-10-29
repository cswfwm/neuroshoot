#!/bin/sh
# 一键构建 APK：生成素材 → 裁剪字体 → 编译打包 → 对齐签名 → 输出到 dist/
#
# 用法：sh build.sh
# 产物：dist/神经枪战-android5.1.apk
set -e
cd "$(dirname "$0")"

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
GRADLE=/opt/gradle-8.9/gradle-8.9/bin/gradle
SDK_APKSIGNER=/opt/android-sdk/build-tools/34.0.0/lib/apksigner.jar
ZIPALIGN=/opt/arm64-tools/zipalign.real

echo "[1/5] 生成美术素材"
python3 tools/gen_art.py

echo "[2/5] 裁剪字体"
python3 tools/subset_font.py

echo "[3/5] 编译打包"
"$GRADLE" :android:assembleRelease --no-daemon --console=plain

echo "[4/5] 对齐并签名"
APK=android/build/outputs/apk/release/android-release.apk
python3 tools/repack_apk.py "$APK" "dist/神经枪战-android5.1.apk"

echo "[5/5] 校验"
LD_PRELOAD=/opt/arm64-tools/libshim.so "$ZIPALIGN" -c 4 "dist/神经枪战-android5.1.apk"
"$JAVA_HOME/bin/java" -jar "$SDK_APKSIGNER" verify "dist/神经枪战-android5.1.apk"
echo "完成：dist/神经枪战-android5.1.apk"
