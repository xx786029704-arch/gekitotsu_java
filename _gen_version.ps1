param(
    [Parameter(Mandatory=$true)][string]$Version,
    [Parameter(Mandatory=$true)][string]$VersionFile
)

$content = @"
package org.example;

/** 版本号常量。由 build_exe.bat 从 pom.xml 自动生成，请勿手动修改。 */
public final class Version {
    public static final String VERSION = "$Version";
    private Version() {}
}
"@

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText((Resolve-Path $VersionFile).Path, $content, $utf8NoBom)
Write-Host "[OK] Generated Version.java (VERSION=$Version)"
