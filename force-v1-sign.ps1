# force-v1-sign.ps1 <apkPath>
#
# Re-signs a release APK with the v1 (JAR) + v2 + v3 schemes using the keystore
# configured in local.properties. AGP 9.x silently drops v1 signing when
# minSdk >= 24 (it ignores enableV1Signing on the legacy variant API), leaving a
# v2-only APK. Some OEM package installers abort a v2-only sideload with a vague
# "scaricamento interrotto" / parse error, so we force v1 back in here after the
# gradle build.
#
# Reads keystore.file / keystore.password / keystore.alias / keystore.key.password
# from android/local.properties (same keys the gradle build uses).

param([Parameter(Mandatory = $true)][string]$ApkPath)
$ErrorActionPreference = 'Stop'

if (-not (Test-Path $ApkPath)) { Write-Error "APK not found: $ApkPath"; exit 1 }

# --- locate apksigner (latest build-tools under the SDK) -------------------
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = "$env:LOCALAPPDATA\Android\Sdk" }
$btDir = Join-Path $sdk 'build-tools'
$bt = Get-ChildItem $btDir -Directory -ErrorAction SilentlyContinue |
    Sort-Object { [version]($_.Name -replace '[^0-9.].*$','0') } | Select-Object -Last 1
if (-not $bt) { Write-Error "No build-tools found under $btDir"; exit 1 }
$apksigner = Join-Path $bt.FullName 'apksigner.bat'

# --- read keystore config from local.properties ----------------------------
$lp = Join-Path $PSScriptRoot 'local.properties'
if (-not (Test-Path $lp)) { Write-Error "local.properties not found at $lp"; exit 1 }
$props = @{}
foreach ($line in Get-Content $lp) {
    if ($line -match '^\s*([^#=]+?)\s*=\s*(.*)$') {
        # local.properties escapes ':' and '\' Java-style — unescape for use.
        $props[$Matches[1]] = ($Matches[2] -replace '\\(.)', '$1')
    }
}
$ksFile = $props['keystore.file']
$ksPass = $props['keystore.password']
$ksAlias = $props['keystore.alias']
$keyPass = $props['keystore.key.password']
if (-not ($ksFile -and $ksPass -and $ksAlias -and $keyPass)) {
    Write-Error "local.properties missing one of keystore.file/.password/.alias/.key.password"; exit 1
}

# apksigner is a JBR-backed .bat that writes conscrypt warnings to stderr; under
# EAP 'Stop' PowerShell 5.1 turns that native stderr into a terminating
# NativeCommandError. Switch to Continue for the native calls below — we guard
# correctness with explicit $LASTEXITCODE / output checks instead.
$ErrorActionPreference = 'Continue'

# --- sign (v1+v2+v3) in place ----------------------------------------------
& $apksigner sign `
    --ks $ksFile --ks-key-alias $ksAlias `
    --ks-pass "pass:$ksPass" --key-pass "pass:$keyPass" `
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true `
    $ApkPath
if ($LASTEXITCODE -ne 0) { Write-Error "apksigner sign failed ($LASTEXITCODE)"; exit 1 }

# --- verify v1 actually present (check at minSdk 21 so v1 is evaluated) -----
$v = & $apksigner verify -v --min-sdk-version 21 $ApkPath
$v1 = ($v | Select-String 'v1 scheme \(JAR signing\): true')
if (-not $v1) {
    Write-Error "v1 signature missing after re-sign. apksigner output:`n$($v -join "`n")"; exit 1
}
Write-Host "  signed v1+v2+v3 OK: $([IO.Path]::GetFileName($ApkPath))"
exit 0
