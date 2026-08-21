#!/bin/bash

# Останавливать выполнение при любой ошибке
set -e

# Переменные конфигурации
MODULE_ZIP_NAME="build/magisk/CoolPerseus_Magisk.zip"
TEMP_BUILD_DIR="magisk_module_tmp"
APK_BUILD_TYPE="release"

# Пути для keyhandler
DEVICE_SETTINGS_APK_SOURCE="keyhandler/build/outputs/apk/${APK_BUILD_TYPE}/keyhandler-${APK_BUILD_TYPE}.apk"
DEVICE_SETTINGS_MAGISK_DIR="${TEMP_BUILD_DIR}/system/priv-app/DeviceSettings"

# Пути для CoolPerseus
COOL_PERSEUS_APK_SOURCE="coolperseus/build/outputs/apk/${APK_BUILD_TYPE}/coolperseus-${APK_BUILD_TYPE}.apk"
COOL_PERSEUS_MAGISK_DIR="${TEMP_BUILD_DIR}/system/priv-app/CoolPerseus"

# Путь для разрешений
PERMISSIONS_MAGISK_DIR="${TEMP_BUILD_DIR}/system/etc/permissions"

# Цвета для вывода
CYAN='\033[0;36m'
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo -e "${CYAN}=== Magisk Module Build Script ===${NC}"

# Шаг 1: Проверка module.prop
if [ ! -f "module.prop" ]; then
    echo -e "${RED}[ERROR] module.prop not found in the root directory!${NC}"
    exit 1
fi

# Шаг 2: Сборка APK через Gradle
echo -e "[1/5] Compiling APKs using gradlew..."
# Преобразование "release" -> "Release" для задачи Gradle
GRADLE_TASK="assemble${APK_BUILD_TYPE^}"
./gradlew "$GRADLE_TASK"

# Шаг 3: Проверка существования APK
if [ ! -f "$DEVICE_SETTINGS_APK_SOURCE" ]; then
    echo -e "${RED}[ERROR] DeviceSettings APK not found at: $DEVICE_SETTINGS_APK_SOURCE${NC}"
    exit 1
fi
if [ ! -f "$COOL_PERSEUS_APK_SOURCE" ]; then
    echo -e "${RED}[ERROR] CoolPerseus APK not found at: $COOL_PERSEUS_APK_SOURCE${NC}"
    exit 1
fi

# Шаг 4: Подготовка структуры Magisk
echo -e "[2/5] Preparing Magisk folder structure..."
rm -rf "$TEMP_BUILD_DIR"
mkdir -p "$DEVICE_SETTINGS_MAGISK_DIR"
mkdir -p "$COOL_PERSEUS_MAGISK_DIR"
mkdir -p "$PERMISSIONS_MAGISK_DIR"
mkdir -p "$(dirname "$MODULE_ZIP_NAME")"

# Шаг 5: Копирование файлов
echo -e "[3/5] Copying files..."
cp "module.prop" "$TEMP_BUILD_DIR/"
cp "service.sh" "$TEMP_BUILD_DIR/"
cp "privapp-permissions-coolperseus.xml" "$PERMISSIONS_MAGISK_DIR/"

cp "$DEVICE_SETTINGS_APK_SOURCE" "$DEVICE_SETTINGS_MAGISK_DIR/DeviceSettings.apk"
cp "$COOL_PERSEUS_APK_SOURCE" "$COOL_PERSEUS_MAGISK_DIR/CoolPerseus.apk"

# Шаг 6: Создание zip-архива
echo -e "[4/5] Zipping the Magisk module..."
if ! command -v zip &> /dev/null; then
    echo -e "${RED}[ERROR] 'zip' utility not found. Please install it (e.g., sudo apt install zip).${NC}"
    exit 1
fi

ABS_ZIP_PATH="$(pwd)/$MODULE_ZIP_NAME"
rm -f "$ABS_ZIP_PATH"

# Переходим в папку, чтобы структура архива начиналась с корня
pushd "$TEMP_BUILD_DIR" > /dev/null
zip -r9 "$ABS_ZIP_PATH" ./*
popd > /dev/null

# Шаг 7: Очистка
echo -e "[5/5] Cleaning up temporary files..."
rm -rf "$TEMP_BUILD_DIR"

echo -e "${GREEN}=== Build Successful! ===${NC}"
echo -e "${GREEN}Module saved as: $MODULE_ZIP_NAME${NC}"