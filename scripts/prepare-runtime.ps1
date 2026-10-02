param(
    [Parameter(Mandatory = $true)]
    [string]$DistDir
)

$ErrorActionPreference = 'Stop'
$AppHome = (Resolve-Path $DistDir).Path
$JdkHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    throw 'A JDK with jdeps.exe and jlink.exe is required. Set JAVA_HOME to JDK 21.'
}

$Jdeps = Join-Path $JdkHome 'bin\jdeps.exe'
$Jlink = Join-Path $JdkHome 'bin\jlink.exe'
if (-not (Test-Path $Jdeps) -or -not (Test-Path $Jlink)) {
    throw "JAVA_HOME does not point to a full JDK: $JdkHome"
}

$Jre = Join-Path $AppHome 'jre'
if (Test-Path $Jre) { Remove-Item -Recurse -Force $Jre }

$jars = Get-ChildItem (Join-Path $AppHome 'lib') -Filter '*.jar' -File | Sort-Object FullName
if ($jars.Count -eq 0) { throw "No application jars found under $AppHome\lib" }

$jdepsArgs = @('--multi-release', '21', '--ignore-missing-deps', '--print-module-deps', '--recursive') + $jars.FullName
$moduleOutput = & $Jdeps @jdepsArgs
if ($LASTEXITCODE -ne 0) { throw 'jdeps failed while calculating the runtime module set.' }
$modules = ($moduleOutput | Select-Object -Last 1).Trim()
if ([string]::IsNullOrWhiteSpace($modules)) { throw 'jdeps returned no runtime modules.' }
$modules = "$modules,jdk.crypto.ec,java.net.http"

& $Jlink '--add-modules' $modules '--bind-services' '--strip-debug' '--no-header-files' '--no-man-pages' '--output' $Jre
if ($LASTEXITCODE -ne 0) { throw 'jlink failed while creating the bundled Java runtime.' }

$script = Join-Path $AppHome 'bin\codecontext.bat'
if (-not (Test-Path $script)) { throw "Launcher not found: $script" }
$text = Get-Content -Raw $script
if ($text -notmatch 'VERICORE_BUNDLED_JAVA') {
    $needle = '@rem Add default JVM options here.'
    $insert = "if exist `"%APP_HOME%\jre\bin\java.exe`" set JAVA_HOME=%APP_HOME%\jre`r`n@rem VERICORE_BUNDLED_JAVA"
    if ($text -notmatch [regex]::Escape($needle)) { throw 'Could not find launcher insertion point.' }
    $text = $text.Replace($needle, "$insert`r`n$needle")
    Set-Content -Path $script -Value $text -NoNewline
}

& (Join-Path $Jre 'bin\java.exe') -version
& $script --version
