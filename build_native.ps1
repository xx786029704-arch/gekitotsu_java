# 构建 gkt-jni 原生库（Rust）并分发到本工程
#
# 用法:
#   .\build_native.ps1                     # 使用默认 Rust 工程路径
#   .\build_native.ps1 -RustRepo D:\path\to\gekitotsu_rust
#   .\build_native.ps1 -SkipRender         # 只构建 gkt_jni.dll，跳过观战渲染器
#
# 产物:
#   native\gkt_jni.dll                  (工作目录运行时可直接加载)
#   src\main\resources\native\gkt_jni.dll (打包进 fat JAR / jpackage)
#   target\classes\native\gkt_jni.dll   (javac 直编运行时)
#   native\gkt-render.exe               (观战渲染器，独立进程)

param(
    [string]$RustRepo = $(if (Test-Path "D:\program\gekitotsu_ultra_fast\Cargo.toml") { "D:\program\gekitotsu_ultra_fast" } else { Join-Path (Split-Path -Parent $PSScriptRoot) "gekitotsu_ultra_fast" }),
    [switch]$SkipRender
)

$ErrorActionPreference = "Stop"
$manifest = Join-Path $RustRepo "Cargo.toml"
if (-not (Test-Path $manifest)) {
    throw "找不到 Rust 工程: $manifest（可用 -RustRepo 指定）"
}

cargo build --release --manifest-path $manifest -p gkt-jni
if ($LASTEXITCODE -ne 0) {
    throw "cargo build 失败"
}

$dll = Join-Path $RustRepo "target\release\gkt_jni.dll"
if (-not (Test-Path $dll)) {
    throw "未生成 $dll"
}

foreach ($dir in @("native", "src\main\resources\native", "target\classes\native")) {
    $dir = Join-Path $PSScriptRoot $dir
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Copy-Item $dll (Join-Path $dir "gkt_jni.dll") -Force
}
Write-Host "gkt_jni.dll 已构建并分发: $dll"

if ($SkipRender) {
    return
}

# 观战渲染器 gkt-render.exe（独立进程，由「对战模拟」页的观战按钮启动）。
# PGO profile 只针对批量模拟热点，渲染器不需要；清空 RUSTFLAGS 走普通 release 构建。
$savedFlags = $env:RUSTFLAGS
try {
    $env:RUSTFLAGS = ""
    cargo build --release --manifest-path $manifest -p gkt-render
    if ($LASTEXITCODE -ne 0) {
        throw "cargo build gkt-render 失败"
    }
} finally {
    $env:RUSTFLAGS = $savedFlags
}

$exe = Join-Path $RustRepo "target\release\gkt-render.exe"
if (-not (Test-Path $exe)) {
    throw "未生成 $exe"
}
# exe 无法从 JAR 内启动，只分发到 native\（打包时随 jpackage 输入目录一起带上）
New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot "native") | Out-Null
Copy-Item $exe (Join-Path $PSScriptRoot "native\gkt-render.exe") -Force
Write-Host "gkt-render.exe 已构建并分发: $exe"
