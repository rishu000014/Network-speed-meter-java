$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$adapters = @(Get-NetAdapter | Where-Object { $_.Status -eq 'Up' -and $_.Name -notmatch 'Loopback' })

foreach ($adapter in $adapters) {
    $ipObject = Get-NetIPAddress -InterfaceIndex $adapter.ifIndex -AddressFamily IPv4 -ErrorAction SilentlyContinue | Select-Object -First 1
    $ip = if ($ipObject) { $ipObject.IPAddress } else { '-' }
    $mac = if ($adapter.MacAddress) { $adapter.MacAddress } else { '-' }
    Write-Output ("NIC|{0}|{1}|{2}|{3}|{4}|{5}|{6}" -f `
        ([string]$adapter.Name), `
        ([string]$adapter.InterfaceDescription), `
        $mac, `
        ([string]$adapter.LinkSpeed), `
        ([string]$adapter.Status), `
        ([int]$adapter.ifIndex), `
        $ip)
}
