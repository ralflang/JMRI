# Create virtual COM port using com0com + hub4com for MCAN TCP Bridge
#
# This helper script creates a virtual COM port for use with the MCAN Protocol
# Serial To TCP Multiplexer Bridge, allowing serial-only software (e.g., programmer
# tools) to work alongside JMRI by sharing the same CC-Schnitte adapter.
#
# Author: Ralf Lang (ralf.lang@ralf-lang.de)
# Copyright: (C) 2026 JMRI Community
#
# Usage: .\create-mcan-virtual-port-com0com.ps1 -ComPort COM99 -TcpHost localhost -TcpPort 15731
#
# Prerequisites:
#   1. Install com0com from: https://sourceforge.net/projects/com0com/
#   2. Create port pair using com0com setup (e.g., COM98 <-> COM99)
#   3. hub4com is included with com0com installation
#
# This script connects one end of the COM port pair to the TCP server.
# Your programmer software uses the other end of the pair.

param(
    [Parameter(Mandatory=$true, HelpMessage="COM port name from com0com pair (e.g., COM99)")]
    [string]$ComPort,

    [Parameter(Mandatory=$true, HelpMessage="TCP host to connect to (e.g., localhost)")]
    [string]$TcpHost,

    [Parameter(Mandatory=$true, HelpMessage="TCP port number (e.g., 15731)")]
    [int]$TcpPort,

    [Parameter(Mandatory=$false, HelpMessage="Baud rate (default: 115200)")]
    [int]$BaudRate = 115200
)

# Check if hub4com is installed
$hub4comPath = Get-Command hub4com -ErrorAction SilentlyContinue
if (-not $hub4comPath) {
    # Try common installation paths
    $possiblePaths = @(
        "C:\Program Files\com0com\hub4com.exe",
        "C:\Program Files (x86)\com0com\hub4com.exe",
        "$env:ProgramFiles\com0com\hub4com.exe"
    )

    $found = $false
    foreach ($path in $possiblePaths) {
        if (Test-Path $path) {
            $hub4comPath = $path
            $found = $true
            break
        }
    }

    if (-not $found) {
        Write-Host "Error: hub4com is not installed or not in PATH" -ForegroundColor Red
        Write-Host ""
        Write-Host "Install com0com (includes hub4com):" -ForegroundColor Yellow
        Write-Host "  Download: https://sourceforge.net/projects/com0com/" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "After installation:" -ForegroundColor Yellow
        Write-Host "  1. Run 'setupc' to create COM port pair (e.g., COM98 <-> COM99)" -ForegroundColor White
        Write-Host "  2. Use this script with one end of the pair" -ForegroundColor White
        Write-Host "  3. Connect your programmer to the other end" -ForegroundColor White
        exit 1
    }
}

Write-Host "Creating COM port bridge with com0com + hub4com..." -ForegroundColor Green
Write-Host "  COM port:     $ComPort" -ForegroundColor Cyan
Write-Host "  TCP server:   ${TcpHost}:${TcpPort}" -ForegroundColor Cyan
Write-Host "  Baud rate:    $BaudRate" -ForegroundColor Cyan
Write-Host ""
Write-Host "Your programmer software should connect to the paired COM port." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

# Start hub4com to bridge COM port to TCP
# Options:
#   --baud=...       - Set baud rate
#   --route=All:...  - Route all data between endpoints
#   \\.\COMx         - COM port endpoint
#   host:port        - TCP endpoint
try {
    $comDevice = "\\.\$ComPort"
    $tcpEndpoint = "${TcpHost}:${TcpPort}"

    if ($hub4comPath -is [string]) {
        & $hub4comPath --baud=$BaudRate --route=All:$ComPort,TCP $comDevice $tcpEndpoint
    } else {
        & hub4com --baud=$BaudRate --route=All:$ComPort,TCP $comDevice $tcpEndpoint
    }
}
catch {
    Write-Host ""
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "  1. Verify $ComPort exists and is part of com0com pair:" -ForegroundColor White
    Write-Host "     mode" -ForegroundColor Cyan
    Write-Host "  2. Verify TCP server is running:" -ForegroundColor White
    Write-Host "     netstat -an | findstr `"$TcpPort`"" -ForegroundColor Cyan
    Write-Host "  3. Check com0com configuration:" -ForegroundColor White
    Write-Host "     setupc list" -ForegroundColor Cyan
    exit 1
}
