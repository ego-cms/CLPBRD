package com.ego_cms.copypaste;


import android.view.KeyEvent;

import org.jetbrains.annotations.NotNull;

public interface KeyEventListener {

    boolean dispatchKeyEvent(KeyEvent event);

    boolean dispatchKeyShortcutEvent(KeyEvent event);

    boolean onKeyDown(int keyCode, @NotNull KeyEvent event);

    boolean onKeyLongPress(int keyCode, KeyEvent event);

    boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event);

    boolean onKeyShortcut(int keyCode, KeyEvent event);

    boolean onKeyUp(int keyCode, KeyEvent event);
}
