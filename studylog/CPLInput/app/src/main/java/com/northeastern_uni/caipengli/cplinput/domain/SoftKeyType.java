package com.northeastern_uni.caipengli.cplinput.domain;

import android.graphics.drawable.Drawable;
/**
 * Created by caipengli on 16年2月29日.
 * function key or normal key
 */
public class SoftKeyType {
    public static int NORMAL_KEY_TYPE_ID = 0;

    public int mKeyTypeId;
    public Drawable mKeyBg;
    public Drawable mKeyHlBg;//highlight
    public int mColor;
    public int mColorHl;
    public int mColorBalloon;

    public SoftKeyType(int id, Drawable background, Drawable hlBackground) {
        this.mKeyTypeId = id;
        this.mKeyBg = background;
        this.mKeyHlBg = hlBackground;
    }

    public void setColors(int color, int colorHl, int colorBalloon) {
        mColor = color;
        mColorHl = colorHl;
        mColorBalloon = colorBalloon;
    }
}
