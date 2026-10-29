# 把 Windows / Linux / macOS 三个平台的 demo jar 上传到 Gitee 发行版。
#
# 为什么不在 GitHub Actions 里传：Gitee 的附件接口从 GitHub 的 runner（美国）访问很慢，
# 43MB x 3 经常超时；在本地（国内网络）传只要一两分钟。
#
# 用法：
#   $env:GITEE_OWNER = '你的Gitee用户名'
#   $env:GITEE_TOKEN = 'Gitee 私人令牌（projects 权限）'
#   powershell -ExecutionPolicy Bypass -File tools\publish-gitee.ps1 -Tag v1.0.0
#
# 参数：
#   -Tag        要发布到的 Gitee 发行版（对应已存在的 git tag），默认 v1.0.0
#   -SkipBuild  跳过本地构建，直接用 dist\ 下已有的 jar
#   -Mvn        指定 mvn 命令，默认 mvn

param(
    [string]$Tag = 'v1.0.0',
    [string]$Owner = $env:GITEE_OWNER,
    [string]$Repo = 'spring-javafx',
    [string]$Token = $env:GITEE_TOKEN,
    [string]$Mvn = 'mvn',
    [string]$Dist = 'dist',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

if (-not $Owner) { throw '缺少 Gitee 用户名：设置环境变量 GITEE_OWNER 或传 -Owner' }
if (-not $Token) { throw '缺少 Gitee 令牌：设置环境变量 GITEE_TOKEN 或传 -Token' }

$root = Split-Path -Parent $PSScriptRoot
$api = "https://gitee.com/api/v5/repos/$Owner/$Repo/releases"
$platforms = @('win', 'linux', 'mac')
$ver = $Tag.TrimStart('v')

$bodyTemplate = @'
已发布到 Maven Central，无需额外配置仓库：

```xml
<dependency>
    <groupId>io.github.a-walking-star</groupId>
    <artifactId>spring-javafx</artifactId>
    <version>VERSION</version>
</dependency>
```

附件是可直接运行的示例程序（内置对应平台的 JavaFX 原生库）：
- `spring-javafx-demo-__VER__-win-x64.jar`（Windows）
- `spring-javafx-demo-__VER__-linux-x64.jar`（Linux）
- `spring-javafx-demo-__VER__-mac-x64.jar`（macOS Intel）

运行：`java -jar <对应的 jar>`（需 JDK 8/11/17；JDK 11+ 的运行时依赖见 README）。
'@

function Invoke-Gitee {
    param([string]$Uri, [string]$Method = 'Get', [object]$Payload)
    $headers = @{ 'Content-Type' = 'application/json;charset=UTF-8' }
    if ($Payload) {
        $json = $Payload | ConvertTo-Json -Depth 5 -Compress
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        return Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers -Body $bytes
    }
    return Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers
}

# ---------- 1. 构建三个平台的 jar ----------
if (-not (Test-Path $Dist)) { New-Item -ItemType Directory -Path $Dist | Out-Null }

if (-not $SkipBuild) {
    Push-Location $root
    try {
        foreach ($p in $platforms) {
            Write-Host "==> Building $p ..." -ForegroundColor Cyan
            & $Mvn -B -ntp -DskipTests "-Djavafx.platform=$p" package
            if ($LASTEXITCODE -ne 0) { throw "build failed: $p" }
            Copy-Item "demo\target\spring-javafx-demo.jar" "$Dist\spring-javafx-demo-$ver-$p-x64.jar" -Force
        }
    } finally {
        Pop-Location
    }
}

$files = Get-ChildItem (Join-Path $root $Dist) -Filter *.jar -ErrorAction SilentlyContinue
if (-not $files) { throw "no jars in $Dist; drop -SkipBuild to build first" }

# ---------- 2. 找或建发行版 ----------
$release = $null
$body = $bodyTemplate.Replace('VERSION', $ver).Replace('__VER__', $ver)
try {
    $release = Invoke-Gitee "$api/tags/$Tag`?access_token=$Token"
    Write-Host "==> reuse release: $($release.name)" -ForegroundColor Yellow
    # 同步说明（旧说明可能只写了 Windows 一个附件）
    try {
        Invoke-Gitee "$api/$($release.id)" 'Patch' @{
            access_token = $Token
            name         = "spring-javafx $Tag"
            body         = $body
        } | Out-Null
        Write-Host "==> release description updated" -ForegroundColor Yellow
    } catch {
        Write-Host "==> description update failed (ignored)" -ForegroundColor DarkYellow
    }
} catch {
    Write-Host "==> creating release $Tag ..." -ForegroundColor Cyan
    $release = Invoke-Gitee $api 'Post' @{
        access_token     = $Token
        tag_name         = $Tag
        name             = "spring-javafx $Tag"
        body             = $body
        target_commitish = 'main'
        prerelease       = $false
    }
    Write-Host "==> created: $($release.html_url)" -ForegroundColor Green
}

# ---------- 3. 逐个上传附件 ----------
$existing = @()
if ($release.assets) { $existing = @($release.assets | ForEach-Object { $_.name }) }

$curl = (Get-Command curl.exe -ErrorAction SilentlyContinue).Source
if (-not $curl) { throw 'curl.exe not found (Windows 10+ ships it)' }

$failed = 0
foreach ($f in $files) {
    if ($existing -contains $f.Name) {
        Write-Host "==> $($f.Name) already exists, skip" -ForegroundColor Yellow
        continue
    }
    Write-Host "==> uploading $($f.Name) ($([math]::Round($f.Length / 1MB, 1)) MB) ..." -ForegroundColor Cyan
    $url = "$api/$($release.id)/attach_files?access_token=$Token"
    & $curl -sS --connect-timeout 30 --max-time 1800 -X POST $url -F "file=@$($f.FullName)" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "   FAILED (rerun the script; uploaded files are skipped)" -ForegroundColor Red
        $failed++
    } else {
        Write-Host "   done" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "release: $($release.html_url)" -ForegroundColor Green
if ($failed -gt 0) { throw "$failed attachment(s) failed, rerun the script" }
