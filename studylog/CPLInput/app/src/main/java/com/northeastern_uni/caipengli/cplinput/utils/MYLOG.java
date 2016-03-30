package com.northeastern_uni.caipengli.cplinput.utils;

import android.util.Log;

/**
 * Created by caipengli on 16年3月8日.
 */
public class MYLOG {
    public static boolean sDebug = true;
    public static final String TAG = "cpl!";
    public static void LOGD(String s) {
        if (sDebug) {
            Log.d(TAG, s);
        }
    }
    public static void LOGD(String TAG, String s) {
        if (sDebug) {
            Log.d(TAG, s);
        }
    }
    public static void LOGI(String s) {
        if (sDebug) {
            Log.i(TAG, s);
        }
    }
    public static void LOGI(String TAG, String s) {
        if (sDebug) {
            Log.i(TAG, s);
        }
    }
    public static void LOGW(String s) {
        if (sDebug) {
            Log.w(TAG, s);
        }
    }
    public static void LOGW(String TAG, String s) {
        if (sDebug) {
            Log.w(TAG, s);
        }
    }
    public static void LOGE(String s) {
        if (sDebug) {
            Log.e(TAG, s);
        }
    }
    public static void LOGE(String TAG, String s) {
        if (sDebug) {
            Log.e(TAG, s);
        }
    }

}
