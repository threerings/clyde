$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$run  = Join-Path $here 'runjava.ps1'

& $run 'com.threerings.opengl.effect.tools.ParticleEditor' @args
exit $LASTEXITCODE