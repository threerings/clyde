$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$run  = Join-Path $here 'runjava.ps1'

& $run 'com.threerings.opengl.model.tools.ModelViewer' @args
exit $LASTEXITCODE