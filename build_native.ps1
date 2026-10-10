# 构建 gkt-jni 原生库（Rust）并分发到本工程
#
# 用法:
#   .\build_native.ps1                     # 使用默认 Rust 工程路径
#   .\build_native.ps1 -RustRepo D:\path\to\gekitotsu_rust
#
# 产物:
#   native\gkt_jni.dll                  (工作目录运行时可直接加载)
#   src\main\resources\native\gkt_jni.dll (打包进 fat JAR / jpackage)
#   target\classes\native\gkt_jni.dll   (javac 直编运行时)

param(
    [string]$RustRepo = "D:\program\gekitotsu_ultra_fast"
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
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Copy-Item $dll (Join-Path $dir "gkt_jni.dll") -Force
}
Write-Host "gkt_jni.dll 已构建并分发: $dll"
