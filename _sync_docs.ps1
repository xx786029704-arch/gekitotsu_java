param(
    [Parameter(Mandatory=$true)][string]$Version
)

$files = @('CLAUDE.md', '使用手册.md')
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

foreach ($f in $files) {
    if (Test-Path $f) {
        $c = Get-Content -Raw -Encoding UTF8 $f
        $c = $c -replace '(?<![\d.])1\.\d+\.\d+(?![\d.])', $Version
        [System.IO.File]::WriteAllText((Resolve-Path $f).Path, $c, $utf8NoBom)
        Write-Host "[OK] Synced version in $f"
    }
}
