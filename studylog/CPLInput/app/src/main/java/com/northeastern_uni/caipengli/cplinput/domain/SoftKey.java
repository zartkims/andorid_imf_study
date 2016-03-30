package com.northeastern_uni.caipengli.cplinput.domain;

import android.graphics.drawable.Drawable;

/**
 * Created by caipengli on 16年2月23日.
 * 自定义软键盘按键
 */
public class SoftKey {
    protected static final int KEYMASK_REPEAT = 0x10000000;
    protected static final int KEYMASK_BALLOON = 0x20000000;

    //兼容一部分点击的偏差
    public static final int MAX_MOVE_TOLERANCE_X = 0;
    public static final int MAX_MOVE_TOLERANCE_Y = 0;

    protected int mKeyMask;
    public int mKeyCode;
    public SoftKeyType mKeyType;
    protected Drawable mKeyIcon;
    protected Drawable mKeyIconPopup;
    protected String mKeyLabel;

    public int mPopupSkbId;
    public float mLeftF;
    public float mRightF;
    public float mTopF;
    public float mBottomF;
    public int mLeft;
    public int mRight;
    public int mTop;
    public int mBottom;

    public void setKeyType(SoftKeyType keyType, Drawable keyIcon, Drawable keyIconPopup) {
        mKeyType = keyType;
        mKeyIcon = keyIcon;
        mKeyIconPopup = keyIconPopup;
    }

    /**皆为百分比*/
    public void setKeyDimensions(float left, float top, float right, float bottom) {
        mLeftF = left;
        mTopF = top;
        mRightF = right;
        mBottomF = bottom;
    }

    public void setKeyAttribute(int keyCode, String label, boolean repeat,
                                boolean balloon) {
        mKeyCode = keyCode;
        mKeyLabel = label;

        if (repeat) {
            mKeyMask |= KEYMASK_REPEAT;
        } else {
            mKeyMask &= (~KEYMASK_REPEAT);
        }

        if (balloon) {
            mKeyMask |= KEYMASK_BALLOON;
        } else {
            mKeyMask &= (~KEYMASK_BALLOON);
        }
    }

    public void setPopupSkbId(int popupSkbId) {
        mPopupSkbId = popupSkbId;
    }

    /**
     * 在setKeyDimensions后调用s
     * @param skbWidth
     * @param skbHeight
     */
    public void setSkbCoreSize(int skbWidth, int skbHeight) {
        mLeft = (int) (mLeftF * skbWidth);
        mRight = (int) (mRightF * skbWidth);
        mTop = (int) (mTopF * skbHeight);
        mBottom = (int) (mBottomF * skbHeight);
    }

    public Drawable getKeyIcon() {
        return mKeyIcon;
    }

    public Drawable getKeyIconPopup() {
        if (null != mKeyIconPopup) {
            return mKeyIconPopup;
        }
        return mKeyIcon;
    }

    public int getKeyCode() {
        return mKeyCode;
    }

    public String getKeyLabel() {
        return mKeyLabel;
    }

    public void changeCase(boolean upperCase) {
        if (null != mKeyLabel) {
            if (upperCase)
                mKeyLabel = mKeyLabel.toUpperCase();
            else
                mKeyLabel = mKeyLabel.toLowerCase();
        }
    }

    public Drawable getKeyBg() {
        return mKeyType.mKeyBg;
    }

    public Drawable getKeyHlBg() {
        return mKeyType.mKeyHlBg;
    }

    public int getColor() {
        return mKeyType.mColor;
    }

    public int getColorHl() {
        return mKeyType.mColorHl;
    }

    public int getColorBalloon() {
        return mKeyType.mColorBalloon;
    }

    /**
     * 通用的那种key
     * @return
     */
    public boolean isKeyCodeKey() {
        if (mKeyCode > 0) return true;
        return false;
    }

    /**用户的定义的键*/
    public boolean isUserDefKey() {
        if (mKeyCode < 0) return true;
        return false;
    }

    /**
     * 是否是unicode类按键
     * @return
     */
    public boolean isUniStrKey() {
        if (null != mKeyLabel && mKeyCode == 0) return true;
        return false;
    }

    public boolean needBalloon() {
        return (mKeyMask & KEYMASK_BALLOON) != 0;
    }

    public boolean repeatable() {
        return (mKeyMask & KEYMASK_REPEAT) != 0;
    }

    public int getPopupResId() {
        return mPopupSkbId;
    }

    public int width() {
        return mRight - mLeft;
    }

    public int height() {
        return mBottom - mTop;
    }

    public boolean moveWithinKey(int x, int y) {
        if (mLeft - MAX_MOVE_TOLERANCE_X <= x
                && mTop - MAX_MOVE_TOLERANCE_Y <= y
                && mRight + MAX_MOVE_TOLERANCE_X > x
                && mBottom + MAX_MOVE_TOLERANCE_Y > y) {
            return true;
        }
        return false;
    }
}
