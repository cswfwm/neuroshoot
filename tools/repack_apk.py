#!/usr/bin/env python3
"""给 assembleRelease 产出的 APK 做 4 字节对齐并签名。

不再重写 zip 结构：安卓 12 及以上要求 resources.arsc 以未压缩方式存储并
4 字节对齐，手工用 zipfile 重写会把它压掉（DEFLATE），导致安装时报
"Targeting R+ requires resources.arsc to be stored uncompressed and aligned"。
正确顺序是 zipalign 对齐（会让 arsc 保持 STORED 并补对齐填充）再 apksigner 签名，
签名必须放在对齐之后，否则会破坏对齐。
"""
import os
import subprocess
import sys

JAVA = "/usr/lib/jvm/java-17-openjdk/bin/java"
APKSIGNER = "/opt/android-sdk/build-tools/34.0.0/lib/apksigner.jar"
ZIPALIGN = "/opt/arm64-tools/zipalign.real"
SHIM = "/opt/arm64-tools/libshim.so"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KEYSTORE = os.path.join(ROOT, "android", "keystore.p12")
PASS = "neuroshoot123"
ALIAS = "neuroshoot"


def align(src, dst):
    env = dict(os.environ, LD_PRELOAD=SHIM)
    subprocess.run([ZIPALIGN, "-f", "-p", "4", src, dst], check=True, env=env)


def sign(src, dst):
    subprocess.run([JAVA, "-jar", APKSIGNER, "sign",
                    "--ks", KEYSTORE, "--ks-type", "PKCS12",
                    "--ks-pass", "pass:" + PASS, "--key-pass", "pass:" + PASS,
                    "--ks-key-alias", ALIAS,
                    "--out", dst, src], check=True)


def verify(path):
    env = dict(os.environ, LD_PRELOAD=SHIM)
    subprocess.run([ZIPALIGN, "-c", "4", path], check=True, env=env)
    subprocess.run([JAVA, "-jar", APKSIGNER, "verify", path], check=True)


def main():
    if len(sys.argv) < 3:
        print("用法: repack_apk.py <输入.apk> <输出.apk>")
        return 2
    src, dst = sys.argv[1], sys.argv[2]
    aligned = dst + ".aligned"
    align(src, aligned)
    sign(aligned, dst)
    os.remove(aligned)
    verify(dst)
    print("已对齐并签名:", dst, os.path.getsize(dst), "字节")
    return 0


if __name__ == "__main__":
    sys.exit(main())
