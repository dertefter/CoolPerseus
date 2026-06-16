package com.android.internal.os;

import android.view.KeyEvent;

public interface DeviceKeyHandler {
    @SuppressWarnings("unused")
    KeyEvent handleKeyEvent(KeyEvent event);

}