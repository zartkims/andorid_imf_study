package com.northeastern_uni.caipengli.cplinput.logic;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

/**
 * Created by caipengli on 16年3月3日.
 */
public class EnglishProcessor {
    private int mLastKeyCode = KeyEvent.KEYCODE_UNKNOWN;

    public boolean processKey(InputConnection ic, KeyEvent event, boolean upperCase, boolean realAction) {
        if (null == ic || null == event) return false;
        int keyCode = event.getKeyCode();

        int keyChar = 0;
        if (KeyEvent.KEYCODE_A <= keyCode && keyCode <= KeyEvent.KEYCODE_Z) {
            keyChar = !upperCase ? 'a' + (keyCode - KeyEvent.KEYCODE_A) : 'A' + (keyCode - KeyEvent.KEYCODE_A);
        } else if (KeyEvent.KEYCODE_0 <= keyCode && keyCode <= KeyEvent.KEYCODE_9) {
            keyChar = keyCode - KeyEvent.KEYCODE_0 + '0';
        } else if (keyCode == KeyEvent.KEYCODE_COMMA) {
            keyChar = ',';
        } else if (keyCode == KeyEvent.KEYCODE_PERIOD) {
            keyChar = '.';
        } else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) {
            keyChar = '\'';
        } else if (keyCode == KeyEvent.KEYCODE_AT) {
            keyChar = '@';
        } else if (keyCode == KeyEvent.KEYCODE_SLASH) {
            keyChar = '/';
        }

        if (0 == keyChar) {
            mLastKeyCode = keyCode;
            String insert = null;
            if (KeyEvent.KEYCODE_DEL == keyCode) {
                if (realAction)  {
                    ic.deleteSurroundingText(1, 0);
                }
            } else if (KeyEvent.KEYCODE_ENTER == keyCode) {
                insert = "\n";
            } else if (KeyEvent.KEYCODE_SPACE == keyCode) {
                insert = " ";
            } else {
                return false;
            }

            if (null != insert && realAction) {
                ic.commitText(insert, insert.length());
            }
            return true;
        }

        if (!realAction) return true;
        if (KeyEvent.KEYCODE_SHIFT_LEFT == mLastKeyCode
                || KeyEvent.KEYCODE_SHIFT_RIGHT == mLastKeyCode) {
            if (keyChar >= 'a' && keyChar <= 'z')
                keyChar = keyChar - 'a' + 'A';
        } else if (KeyEvent.KEYCODE_ALT_LEFT == mLastKeyCode) {
        }

        String result = String.valueOf((char) keyChar);
        ic.commitText(result, result.length());
        mLastKeyCode = keyCode;
        return true;
    }
}
