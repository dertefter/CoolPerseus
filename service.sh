#!/system/bin/sh

PKG="com.dertefter.coolperseus"

until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 5
done

sleep 10

appops set $PKG SYSTEM_ALERT_WINDOW allow
appops set $PKG RUN_ANY_IN_BACKGROUND allow
dumpsys deviceidle whitelist +$PKG