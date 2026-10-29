#!/bin/bash
set -e

# 脚本所在目录（绝对路径）
DIR="$(cd "$(dirname "$0")" && pwd)"

# ===== 1. 根据系统架构选择 JDK 目录 =====
ARCH=$(uname -m)
case "$ARCH" in
    x86_64|amd64)
        JDK_DIR="${DIR}/jdk-x64"
        ;;
    aarch64|arm64|armv7l|armhf)
        JDK_DIR="${DIR}/jdk-arm"
        ;;
    *)
        echo "错误：不支持的系统架构 '$ARCH'"
        echo "本脚本仅支持 x86_64(x64) 和 ARM(aarch64/armv7l)"
        exit 1
        ;;
esac

# ===== 2. 验证 JDK 可用性 =====
JAVA="${JDK_DIR}/bin/java"
if [ ! -x "$JAVA" ]; then
    echo "错误：未找到适用于 ${ARCH} 的 Java 可执行文件"
    echo "请预先将 JDK 解压到：${JDK_DIR}"
    echo "  期望路径: ${JAVA}"
    exit 1
fi

# ===== 3. 查找 JAR 包（同级目录下的应用 jar，排除 sources/javadoc）=====
JAR_FILE="${DIR}/spring-javafx-demo.jar"
if [ ! -f "$JAR_FILE" ]; then
    JAR_FILE=$(find "$DIR" -maxdepth 1 -name "*.jar" \
        ! -name "*-sources.jar" ! -name "*-javadoc.jar" | head -1)
fi

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "错误：未找到可运行的 JAR 包"
    echo "请把应用 jar 放到本脚本同级目录（与启动脚本一起分发）"
    exit 1
fi

# ===== 4. 启动 =====
echo "========================================"
echo " 系统架构: ${ARCH}"
echo " JDK 路径: ${JDK_DIR}"
echo " Java 版本: $("${JAVA}" -version 2>&1 | head -1)"
echo " JAR 路径: ${JAR_FILE}"
echo "========================================"

exec "$JAVA" -Dfile.encoding=UTF-8 -jar "$JAR_FILE" "$@"