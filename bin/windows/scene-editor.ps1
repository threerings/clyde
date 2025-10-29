$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$run  = Join-Path $here 'runjava.ps1'

& $run 'com.threerings.tudey.tools.SceneEditor' @args
exit $LASTEXITCODE