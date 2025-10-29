# resolve project root (parent of /bin-equivalent folder... except we're one level deeper)
$WinScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ScriptDir = Split-Path $WinScriptDir -Parent
$Root = Split-Path $ScriptDir -Parent

# build classpath using Maven output + project classes
# we try quiet mode first; fall back if needed.
$mvnOutput = & mvn -q dependency:build-classpath 2>$null
if (-not $mvnOutput) {
  $mvnOutput = & mvn dependency:build-classpath 2>$null
}

# extract the last non-[INFO] line as the actual dependency classpath
$mvnPath = ($mvnOutput | Where-Object { $_ -and ($_ -notmatch '^\[INFO\]') } | Select-Object -Last 1)

# start building classpath
$classpathParts = @()
if ($mvnPath) { $classpathParts += $mvnPath }
$classpathParts += @(
  (Join-Path $Root "dist\test-classes"),
  (Join-Path $Root "dist\classes")
)

# add zip/jar files from <root>\dist\lib and %JAVA_LIBS%, excluding *clyde*.*
$dirs = @(
  (Join-Path $Root "dist\lib"),
  $env:JAVA_LIBS
) | Where-Object { $_ -and (Test-Path $_) }

foreach ($dir in $dirs) {
  Get-ChildItem -Path $dir -File -Include *.jar,*.zip -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'clyde' } |
    ForEach-Object { $classpathParts += $_.FullName }
}

# final classpath
$classpath = ($classpathParts -join ';')

# java system properties
$rootArgs = @(
  "-Dno_unpack_resources=true",
  "-Dresource_dir=$(Join-Path $Root 'dist\rsrc')",
  "-Djava.library.path=$(Join-Path $Root 'lib\windows')",
  "-Drsrc_cache_dir=$env:TEMP",
  "-Dno_log_redir=true",
  "-Dtest_dir=$Root",
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.ref=ALL-UNNAMED",
  "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED",
  "--enable-native-access=ALL-UNNAMED"
)

# parse our args: handle -p <pidfile> and strip a bare `--`
$pidFile   = $null
$passArgs  = @()
$parsing   = $true
$skipNext  = $false

for ($i = 0; $i -lt $args.Count; $i++) {
  if ($skipNext) { $skipNext = $false; continue }
  $a = $args[$i]

  if ($parsing -and $a -eq "--") {
    # drop the '--' and pass the rest through
    $parsing = $false
    continue
  }

  if ($parsing -and $a -eq "-p") {
    if ($i + 1 -lt $args.Count) {
      $pidFile = $args[$i + 1]
      $skipNext = $true
      continue
    } else {
      Write-Error "Missing value for -p"
      exit 2
    }
  }

  $passArgs += $a
}

# if requested, write current ps process PID to file
if ($pidFile) {
  try {
    Set-Content -Path $pidFile -Value $PID -Encoding ASCII
  } catch {
    Write-Error "Failed to write PID to '$pidFile': $_"
    exit 3
  }
}

# build the java command
$javaArgs = @(
  "-Xmx512M",
  "-classpath", $classpath
) + $rootArgs + $passArgs

Write-Host "java $($javaArgs -join ' ')"

# execute and propagate exit code
& java @javaArgs
exit $LASTEXITCODE
