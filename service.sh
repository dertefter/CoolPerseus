#!/system/bin/sh

PKG="com.dertefter.coolperseus"
APK_PATH="/system/priv-app/CoolPerseus/CoolPerseus.apk"

until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 5
done

sleep 10

if ! pm list packages | grep -q "$PKG"; then
    pm install -r -g "$APK_PATH"
fi

appops set $PKG SYSTEM_ALERT_WINDOW allow
appops set $PKG RUN_ANY_IN_BACKGROUND allow
dumpsys deviceidle whitelist +$PKG