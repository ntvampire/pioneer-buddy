# Standard UTF-8 header for Windows PowerShell 5.1
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$AndroidSdk = "D:\Projects\AndroidSDK"
$EmulatorExe = Join-Path $AndroidSdk "emulator\emulator.exe"
$AdbExe = Join-Path $AndroidSdk "platform-tools\adb.exe"
$AvdName = "Pixel_Phone_34"
$ApkPath = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Pioneer Buddy — Запуск эмулятора Android " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Проверяем наличие SDK
if (-not (Test-Path $EmulatorExe)) {
    Write-Error "Не найден emulator.exe по пути: $EmulatorExe"
    exit 1
}

# 2. Проверяем, запущен ли уже эмулятор
$runningDevice = (& $AdbExe devices) | Where-Object { $_ -match "emulator-\d+\s+device" }

if (-not $runningDevice) {
    Write-Host "[1/4] Запуск виртуального устройства $AvdName..." -ForegroundColor Yellow
    Start-Process -FilePath $EmulatorExe -ArgumentList "-avd", $AvdName, "-gpu", "swiftshader_indirect", "-no-snapshot"
    
    Write-Host "[2/4] Ожидание подключения ADB..." -ForegroundColor Yellow
    & $AdbExe wait-for-device
    
    Write-Host "[3/4] Ожидание завершения загрузки Android..." -ForegroundColor Yellow
    while ($true) {
        $boot = (& $AdbExe shell getprop sys.boot_completed 2>$null) -replace '\s',''
        if ($boot -eq '1') { break }
        Start-Sleep -Seconds 2
    }
} else {
    Write-Host "[1/3] Эмулятор уже запущен и готов к работе." -ForegroundColor Green
}

# 3. Установка свежего APK
if (Test-Path $ApkPath) {
    Write-Host "[*] Установка Pioneer Buddy ($ApkPath)..." -ForegroundColor Yellow
    & $AdbExe install -r -t -g $ApkPath
} else {
    Write-Warning "Файл APK не найден по пути $ApkPath. Сначала соберите проект: .\gradlew assembleDebug"
}

# 4. Запуск приложения на экране
Write-Host "[*] Запуск интерфейса Pioneer Buddy..." -ForegroundColor Green
& $AdbExe shell am start -n "com.pioneer.buddy.debug/com.pioneer.buddy.MainActivity"

Write-Host "`nГотово! Приложение запущено на экране эмулятора." -ForegroundColor Cyan