$ErrorActionPreference = 'Stop'

# Configuration variables
$moduleZipName = "DeviceSettings_Magisk.zip"
$tempBuildDir = "magisk_module_tmp"
$apkBuildType = "release"

# Paths for the first keyhandler: DeviceSettings (Module: keyhandler)
$deviceSettingsApkSource = "keyhandler\build\outputs\apk\$apkBuildType\keyhandler-$apkBuildType.apk"
$deviceSettingsMagiskDir = "$tempBuildDir\system\priv-app\DeviceSettings"

# Paths for the second app: CoolPerseus (Module: coolperseus)
$coolPerseusApkSource = "coolperseus\build\outputs\apk\$apkBuildType\coolperseus-$apkBuildType.apk"
$coolPerseusMagiskDir = "$tempBuildDir\system\priv-app\CoolPerseus"

Write-Host "=== Magisk Module Build Script ===" -ForegroundColor Cyan

# Step 1: Check for module.prop
if (-Not (Test-Path "module.prop")) {
    Write-Host "[ERROR] module.prop not found in the root directory!" -ForegroundColor Red
    exit 1
}

# Step 2: Build the APKs using Gradle
Write-Host "[1/5] Compiling APKs using gradlew..."
# This command builds all modules in the project for the specified build type
.\gradlew.bat assemble$($apkBuildType.Substring(0,1).ToUpper() + $apkBuildType.Substring(1).ToLower())
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Gradle build failed. Exiting." -ForegroundColor Red
    exit 1
}

# Step 3: Verify both APKs exist
if (-Not (Test-Path $deviceSettingsApkSource)) {
    Write-Host "[ERROR] DeviceSettings APK not found at: $deviceSettingsApkSource" -ForegroundColor Red
    exit 1
}
if (-Not (Test-Path $coolPerseusApkSource)) {
    Write-Host "[ERROR] CoolPerseus APK not found at: $coolPerseusApkSource" -ForegroundColor Red
    exit 1
}

# Step 4: Prepare Magisk directory structure
Write-Host "[2/5] Preparing Magisk folder structure..."
if (Test-Path $tempBuildDir) {
    Remove-Item -Path $tempBuildDir -Recurse -Force
}
New-Item -ItemType Directory -Path $deviceSettingsMagiskDir | Out-Null
New-Item -ItemType Directory -Path $coolPerseusMagiskDir | Out-Null

# Step 5: Copy files to the temporary directory
Write-Host "[3/5] Copying module.prop and APKs..."
Copy-Item -Path "module.prop" -Destination "$tempBuildDir\"
Copy-Item -Path $deviceSettingsApkSource -Destination "$deviceSettingsMagiskDir\DeviceSettings.apk"
Copy-Item -Path $coolPerseusApkSource -Destination "$coolPerseusMagiskDir\CoolPerseus.apk"

# Step 6: Create the flashable zip archive using jar.exe
Write-Host "[4/5] Zipping the Magisk module using jar.exe..."
$Jar = "$env:JAVA_HOME\bin\jar.exe"
if (-Not (Test-Path $Jar)) {
    Write-Host "[ERROR] jar.exe not found. Is JAVA_HOME set correctly?" -ForegroundColor Red
    exit 1
}

$absoluteZipPath = Join-Path (Get-Location) $moduleZipName
if (Test-Path $absoluteZipPath) {
    Remove-Item -Path $absoluteZipPath -Force
}

# Change directory to the temp folder so the zip structure is flat at the root
Push-Location $tempBuildDir
& $Jar cfM $absoluteZipPath *
Pop-Location

# Step 7: Cleanup temporary files
Write-Host "[5/5] Cleaning up temporary files..."
Remove-Item -Path $tempBuildDir -Recurse -Force

Write-Host "=== Build Successful! ===" -ForegroundColor Green
Write-Host "Module saved as: $moduleZipName" -ForegroundColor Green