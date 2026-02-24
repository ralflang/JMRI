# Create virtual COM port that connects to MCAN TCP Bridge server
#
# This helper script creates a virtual COM port for use with the MCAN Protocol
# Serial To TCP Multiplexer Bridge, allowing serial-only software (e.g., programmer
# tools) to work alongside JMRI by sharing the same CC-Schnitte adapter.
#
# Author: Ralf Lang (ralf.lang@ralf-lang.de)
# Copyright: (C) 2026 JMRI Community
#
# Usage: .\create-mcan-virtual-port.ps1 -VirtualPort COM99 -TcpHost localhost -TcpPort 15731
#
# Prerequisites:
#   Install com2tcp: winget install --id=Eterlogic.com2tcp -e
#
# Note: This requires a physical or com0com virtual COM port pair.
# For true virtual ports, see create-mcan-virtual-port-com0com.ps1

param(
    [Parameter(Mandatory=$true, HelpMessage="Virtual COM port name (e.g., COM99)")]
    [string]$VirtualPort,

    [Parameter(Mandatory=$true, HelpMessage="TCP host to connect to (e.g., localhost)")]
    [string]$TcpHost,

    [Parameter(Mandatory=$true, HelpMessage="TCP port number (e.g., 15731)")]
    [int]$TcpPort,

    [Parameter(Mandatory=$false, HelpMessage="Baud rate (default: 115200)")]
    [int]$BaudRate = 115200
)

# Check if com2tcp is installed
$com2tcpPath = Get-Command com2tcp -ErrorAction SilentlyContinue
if (-not $com2tcpPath) {
    Write-Host "Error: com2tcp is not installed" -ForegroundColor Red
    Write-Host ""
    Write-Host "Install with winget:" -ForegroundColor Yellow
    Write-Host "  winget install --id=Eterlogic.com2tcp -e" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Or download from: https://com2tcp.com/" -ForegroundColor Yellow
    exit 1
}

Write-Host "Creating virtual serial port..." -ForegroundColor Green
Write-Host "  Virtual port: $VirtualPort" -ForegroundColor Cyan
Write-Host "  TCP server:   ${TcpHost}:${TcpPort}" -ForegroundColor Cyan
Write-Host "  Baud rate:    $BaudRate" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

# Start com2tcp
# Note: com2tcp expects the COM port to already exist (physical or com0com virtual)
try {
    & com2tcp --baud $BaudRate "\\.\$VirtualPort" $TcpHost $TcpPort
}
catch {
    Write-Host ""
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "  1. Verify $VirtualPort exists:" -ForegroundColor White
    Write-Host "     mode" -ForegroundColor Cyan
    Write-Host "  2. If port doesn't exist, install com0com to create virtual port pairs" -ForegroundColor White
    Write-Host "     Download: https://sourceforge.net/projects/com0com/" -ForegroundColor Cyan
    Write-Host "  3. Or use a real COM port" -ForegroundColor White
    exit 1
}
