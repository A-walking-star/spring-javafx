@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: 获取脚本所在目录（绝对路径）
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

:: 设置 Java 路径（把随包分发的 JDK 解压到脚本同级的 jdk-x64 目录）
set "JAVA_HOME=%SCRIPT_DIR%\jdk-x64"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

:: 检查 Java 是否存在
if not exist "%JAVA_EXE%" (
    echo [错误] 找不到 Java 可执行文件: %JAVA_EXE%
    echo 请把 JDK 解压到脚本同级的 jdk-x64 目录。
    pause
    exit /b 1
)

:: 自动查找当前目录下的 jar 包
set "JAR_FILE="
for %%f in ("%SCRIPT_DIR%\*.jar") do (
    set "JAR_FILE=%%f"
    goto :found
)

:found
if not defined JAR_FILE (
    echo [错误] 当前目录下没有找到 .jar 文件。
    pause
    exit /b 1
)

echo 使用 JDK: %JAVA_HOME%
echo 运行 JAR: %JAR_FILE%
echo.

echo ==========================================
echo 正在启动应用 ...
echo ==========================================

:: 运行 jar 包（保留所有命令行参数）
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -jar "%JAR_FILE%" %*

echo.
pause