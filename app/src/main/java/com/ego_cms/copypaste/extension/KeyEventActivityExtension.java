package com.ego_cms.copypaste.extension;

import android.app.Activity;
import android.view.KeyEvent;

import com.ego_cms.copypaste.KeyEventListener;
import com.ego_cms.copypaste.util.ExtensionBase;

import org.jetbrains.annotations.NotNull;

/**
 * An activity key event extension that enables an activity to have an external key listener delegate.
 */
public class KeyEventActivityExtension<T extends Activity & KeyEventActivityExtension.ExtensionProvider> extends ExtensionBase<T> {

    public interface ExtensionProvider {
        void setKeyEventListener(KeyEventListener l);
    }


    public KeyEventActivityExtension(@NotNull T activityExtended) {
        super(activityExtended);
    }


    KeyEventListener listener;

    public void setKeyEventListener(KeyEventListener listener) {
        this.listener = listener;

        getInstanceExtended().takeKeyEvents(listener != null);
    }


    public boolean dispatchKeyEvent(KeyEvent event) {
        return listener != null && listener.dispatchKeyEvent(event) || getInstanceExtended().dispatchKeyEvent(event);
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return listener != null && listener.dispatchKeyShortcutEvent(event) || getInstanceExtended().dispatchKeyShortcutEvent(event);
    }


    public boolean onKeyDown(int keyCode, @NotNull KeyEvent event) {
        return listener != null && listener.onKeyDown(keyCode, event) || getInstanceExtended().onKeyDown(keyCode, event);
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return listener != null && listener.onKeyLongPress(keyCode, event) || getInstanceExtended().onKeyLongPress(keyCode, event);
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return listener != null && listener.onKeyMultiple(keyCode, repeatCount, event) || getInstanceExtended().onKeyMultiple(keyCode, repeatCount, event);
    }

    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return listener != null && listener.onKeyShortcut(keyCode, event) || getInstanceExtended().onKeyShortcut(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return listener != null && listener.onKeyUp(keyCode, event) || getInstanceExtended().onKeyUp(keyCode, event);
    }
}
