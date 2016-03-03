package com.android.inputmethod.pinyin.constants;

import android.content.Context;

/**
 * Created by caipengli on 16年2月23日.
 */
public class Constants {
    public static final int LEFT_PADDING = 5;
    public static final int RIGHT_PADDING = 5;
    public static final int TOP_PADDING = 5;
    public static final int BOTTOM_PADDING = 5;
    private static float sDensity = 0;
    public static float getDensity(Context context) {
        if (sDensity == 0) {
           sDensity = context.getResources().getDisplayMetrics().density;
        }
        return sDensity;
    }
}
