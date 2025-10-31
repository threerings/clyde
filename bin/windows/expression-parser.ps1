$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$run  = Join-Path $here 'runjava.ps1'

& $run 'com.threerings.expr.ExpressionParser' @args
exit $LASTEXITCODE