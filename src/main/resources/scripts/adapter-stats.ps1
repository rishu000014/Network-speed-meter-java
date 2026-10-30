$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Get-NetAdapterStatistics | ForEach-Object {
    Write-Output ("STAT|{0}|{1}|{2}" -f $_.Name, [int64]$_.ReceivedBytes, [int64]$_.SentBytes)
}
