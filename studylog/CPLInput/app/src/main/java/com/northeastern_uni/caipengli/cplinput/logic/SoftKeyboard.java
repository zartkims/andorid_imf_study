package com.northeastern_uni.caipengli.cplinput.logic;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;

import com.northeastern_uni.caipengli.cplinput.domain.SoftKey;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyToggle;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyboardTemplate;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;
import com.northeastern_uni.caipengli.cplinput.utils.SwitchKeyboardManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caipengli on 16年2月23日.
 * 软键盘逻辑
 * 各个属性都会从ｘｍｌ中读取
 * 另外一些主要的操作逻辑也在这里
 */
public class SoftKeyboard {

    private int mSkbXmlId;
    /**是否缓存软键盘*/
    private boolean mCacheFlag;
    private int mCacheId;
    private boolean mStickyFlag;
    private boolean mNewlyLoadedFlag = true;//新加载的还是从pool里面读取的

    //真软键盘的的宽高
    private int mSkbCoreWidth;
    private int mSkbCoreHeight;

    private SoftKeyboardTemplate mSkbTemplate;
    private boolean mIsQwerty;
    private boolean mIsQwertyUpperCase;
    private int mEnabledRowId;
    private List<KeyRow> mKeyRows;

    /**以下一系列的属性会从xml中读取*/
    private Drawable mSkbBg;
    private Drawable mBalloonBg;
    private Drawable mPopupBg;
    private float mKeyXMargin = 0;
    private float mKeyYMargin = 0;

    private Rect mTempRect = new Rect();

    public SoftKeyboard(int skbXmlId, SoftKeyboardTemplate skbTemplate, int skbWidth,
                        int skbHeight) {
        mSkbXmlId = skbXmlId;
        mSkbTemplate = skbTemplate;
        mSkbCoreWidth = skbWidth;
        mSkbCoreHeight = skbHeight;
    }

    public void setFlags(boolean cacheFlag, boolean stickyFlag,
                         boolean isQwerty, boolean isQwertyUpperCase) {
        mCacheFlag = cacheFlag;
        mStickyFlag = stickyFlag;
        mIsQwerty = isQwerty;
        mIsQwertyUpperCase = isQwertyUpperCase;
    }

    public boolean getCacheFlag() {
        return mCacheFlag;
    }

    public void setCacheId(int cacheId) {
        mCacheId = cacheId;
    }

    public boolean getStickyFlag() {
        return mStickyFlag;
    }

    public void setSkbBackground(Drawable skbBg) {
        mSkbBg = skbBg;
    }

    public void setPopupBackground(Drawable popupBg) {
        mPopupBg = popupBg;
    }

    public void setKeyBalloonBackground(Drawable balloonBg) {
        mBalloonBg = balloonBg;
    }

    public void setKeyMargins(float xMargin, float yMargin) {
        mKeyXMargin = xMargin;
        mKeyYMargin = yMargin;
    }
    public int getCacheId() {
        return mCacheId;
    }

    public void reset() {
        if (null != mKeyRows) mKeyRows.clear();
    }

    public void setNewlyLoadedFlag(boolean newlyLoadedFlag) {
        mNewlyLoadedFlag = newlyLoadedFlag;
    }

    public boolean getNewlyLoadedFlag() {
        return mNewlyLoadedFlag;
    }

    public int getSkbXmlId() {
        return mSkbXmlId;
    }
    public int getSkbCoreWidth() {
        return mSkbCoreWidth;
    }

    public int getSkbCoreHeight() {
        return mSkbCoreHeight;
    }

    /***
     * 开启新的一行
     * @param rowId
     * @param yStartingPos　开始的定点位置
     */
    public void beginNewRow(int rowId, float yStartingPos) {
        if (null == mKeyRows) mKeyRows = new ArrayList<KeyRow>();
        KeyRow keyRow = new KeyRow();
        keyRow.mRowId = rowId;
        keyRow.mTopF = yStartingPos;
        keyRow.mBottomF = yStartingPos;
        keyRow.mSoftKeys = new ArrayList<SoftKey>();
        mKeyRows.add(keyRow);
    }

    public boolean addSoftKey(SoftKey softKey) {
        if (mKeyRows.size() == 0) return false;
        KeyRow keyRow = mKeyRows.get(mKeyRows.size() - 1);
        if (null == keyRow) return false;
        List<SoftKey> softKeys = keyRow.mSoftKeys;
        softKey.setSkbCoreSize(mSkbCoreWidth, mSkbCoreHeight);
        softKeys.add(softKey);

//        MYLOG.LOGI("softKey.mTopF:" + softKey.mTopF + " row top " + keyRow.mTopF);
        //下面这个操作是为了取合适的行高度,以最大的按键高为高
        if (softKey.mTopF < keyRow.mTopF) {
            keyRow.mTopF = softKey.mTopF;
            //MYLOG.LOGI("resize change keyrow top");
        }
        if (softKey.mBottomF > keyRow.mBottomF) {
            keyRow.mBottomF = softKey.mBottomF;
            //MYLOG.LOGI("resize keyrow bottom");
        }
        return true;
    }

    public void setSkbCoreSize(int skbCoreWidth, int skbCoreHeight) {
        if ((mSkbCoreWidth == skbCoreWidth && mSkbCoreHeight == skbCoreHeight) || null == mKeyRows) {
            MYLOG.LOGI("all size is same need not change");
            //return;
        }
        for (int i = 0; i < mKeyRows.size(); i++) {
            KeyRow row = mKeyRows.get(i);
            List<SoftKey> keys = row.mSoftKeys;
            row.mTop = (int) (skbCoreHeight * row.mTopF);
            row.mBottom = (int) (skbCoreHeight * row.mBottomF);
            for (int j = 0; j < keys.size(); j++){
                SoftKey key = keys.get(j);
                key.setSkbCoreSize(skbCoreWidth, skbCoreHeight);
            }
        }
        this.mSkbCoreWidth = skbCoreWidth;
        this.mSkbCoreHeight = skbCoreHeight;
    }

    public int getSkbTotalWidth() {
        Rect padding = getPadding();
        return mSkbCoreWidth + padding.left + padding.right;
    }

    public int getSkbTotalHeight() {
        Rect padding = getPadding();
        return mSkbCoreHeight +padding.top + padding.bottom;
    }

    public int getKeyXMargin() {
        return (int) (mKeyXMargin * mSkbCoreWidth * InputEnvironment.getInstance().getKeyXMarginFactor());
    }

    public int getKeyYMargin() {
        return (int) (mKeyYMargin * mSkbCoreHeight * InputEnvironment.getInstance().getKeyYMarginFactor());
    }

    public Rect getPadding() {
        mTempRect.set(0, 0, 0, 0);
        Drawable skbBg = getSkbBackground();
        if (null == skbBg) return mTempRect;
        skbBg.getPadding(mTempRect);
        return mTempRect;
    }

    public SoftKey getKey(int row, int location) {
        if (null != mKeyRows && mKeyRows.size() > row) {
            List<SoftKey> keys = mKeyRows.get(row).mSoftKeys;
            if(location < keys.size()) {
                return keys.get(location);
            }
        }
        return null;
    }

    /**
     * 很核心的定位key方法
     * @return
     */
    public SoftKey mapToKey(int x, int y) {
        if (null == mKeyRows) {
            return null;
        }

        int rowNum = mKeyRows.size();
        for (int i = 0; i < rowNum; i++) {
            KeyRow row = mKeyRows.get(i);
            if (KeyRow.ALWAYS_SHOW_ROW_ID != row.mRowId && row.mRowId != mEnabledRowId) {
                continue;
            }
//            MYLOG.LOGI(x + "_"+ y +"  row top bottom: " + row.mTop + "_" + row.mBottom);
            if (y < row.mTop || row.mBottom < y) { //不在本行
                continue;
            }
            List<SoftKey> keyList = row.mSoftKeys;
            for (int j = 0; j < keyList.size(); j++) {
                SoftKey key = keyList.get(j);
                if (key.mLeft <= x && x < key.mRight
                        && key.mTop <= y && y < key.mBottom) {
                    return key;//点击在按键范围内s
                }
            }
        }

        //离点击距离最近的一个key
        SoftKey nearestKey = null;
        float nearestDis = Float.MAX_VALUE;
        for (int i = 0; i< rowNum; i++) {
            KeyRow row = mKeyRows.get(i);
            if (KeyRow.ALWAYS_SHOW_ROW_ID != row.mRowId && row.mRowId != mEnabledRowId) {
                continue;
            }
            if (row.mTop > y || row.mBottom <= y) { //不在本行
                continue;
            }
            //以中心点距离为准,找出离点击距离最近的一个按键设定为按键
            List<SoftKey> keyList = row.mSoftKeys;
            for (int j = 0; j < keyList.size(); j++) {
                SoftKey key = keyList.get(j);
                int disX = (key.mLeft + key.mRight) / 2 - x;
                int disY = (key.mTop + key.mBottom) / 2 - y;
                float dis = disX * disX + disY * disY;//不开平方了
                if (dis < nearestDis) {
                    nearestDis = dis;
                    nearestKey = key;
                }
            }
        }
        return nearestKey;
    }

    public void enableToggleState(int toggleStateId, boolean resetIfNotFound) {
        int rowNum = mKeyRows.size();
        for (int i = 0; i < rowNum; i++) {
            KeyRow row = mKeyRows.get(i);
            List<SoftKey> keyList = row.mSoftKeys;
            for (int j = 0; j < keyList.size(); j++) {
                SoftKey sKey = keyList.get(j);
                if (sKey instanceof SoftKeyToggle) {
                    ((SoftKeyToggle) sKey).enableToggleState(toggleStateId,
                            resetIfNotFound);
                }
            }
        }
    }

    /**取消状态*/
    public void disableToggleState(int toggleStateId, boolean resetIfNotFound) {
        int rowNum = mKeyRows.size();
        for (int i = 0; i < rowNum; i++) {
            KeyRow row = mKeyRows.get(i);
            List<SoftKey> keyList = row.mSoftKeys;
            for (int j = 0; j < keyList.size(); j++) {
                SoftKey sKey = keyList.get(j);
                if (sKey instanceof SoftKeyToggle) {
                    ((SoftKeyToggle) sKey).disableToggleState(toggleStateId,
                            resetIfNotFound);
                }
            }
        }
    }
    /**
     * 让键盘各个键都转到合适的状态上
     *
     * @param toggleStates
     */
    public void enableToggleStates(SwitchKeyboardManager.ToggleStates toggleStates) {
        if (null == toggleStates) return;
        enableRow(toggleStates.mRowIdToEnable);
        boolean isQwerty = toggleStates.mQwerty;
        boolean isQwertyUpperCase = toggleStates.mQwertyUpperCase;
        boolean needUpdateQwerty = (isQwerty && mIsQwerty && (mIsQwertyUpperCase != isQwertyUpperCase));
        int states[] = toggleStates.mKeyStates;
        int stateNum = toggleStates.mKeyStatesNum;

        int rowNum = mKeyRows.size();
        for (int i = 0; i < rowNum; i++) {
            KeyRow row = mKeyRows.get(i);
            if (KeyRow.ALWAYS_SHOW_ROW_ID != row.mRowId
                    && row.mRowId != mEnabledRowId) {
                continue;
            }
            List<SoftKey> keyList = row.mSoftKeys;
            int keyNum = keyList.size();
            for (int j = 0; j < keyNum; j++) {
                SoftKey key = keyList.get(j);
                if (key instanceof SoftKeyToggle) {
                    for (int statePos = 0; statePos < stateNum; statePos++) {
                        ((SoftKeyToggle) key).enableToggleState(states[statePos], statePos == 0);
                    }
                    if (0 == stateNum) {
                        ((SoftKeyToggle) key).disableAllToggleStates();
                    }
                }
                if (needUpdateQwerty) {
                    if (key.mKeyCode >= KeyEvent.KEYCODE_A
                            && key.mKeyCode <= KeyEvent.KEYCODE_Z) {
                        key.changeCase(isQwertyUpperCase);
                    }
                }
            }
        }
        mIsQwertyUpperCase = isQwertyUpperCase;
    }

    public boolean enableRow(int rowId) {
        if (KeyRow.ALWAYS_SHOW_ROW_ID == rowId) return false;
        boolean enabled =false;
        int rowNum = mKeyRows.size();
        for (int i = 0; i < rowNum; i++) {
            if (mKeyRows.get(i).mRowId == rowId) {
                enabled = true;
                break;
            }
        }
        if (enabled) {
            mEnabledRowId = rowId;
        }
        return enabled;
    }

    public Drawable getSkbBackground() {
        return null != mSkbBg ? mSkbBg : mSkbTemplate.getSkbBackground();
    }

    public Drawable getBalloonBackground() {
        return null != mBalloonBg ? mBalloonBg : mSkbTemplate.getBalloonBackground();
    }

    public Drawable getPopupBackground() {
        return null != mPopupBg ? mPopupBg : mSkbTemplate.getPopupBackground();
    }

    public int getRowNum() {
        return null != mKeyRows ? mKeyRows.size() : 0;
    }

    public KeyRow getKeyRowForDisplay(int position) {
        if (null != mKeyRows && mKeyRows.size() > position) {
            KeyRow keyRow = mKeyRows.get(position);
            if (KeyRow.ALWAYS_SHOW_ROW_ID == keyRow.mRowId
                    || keyRow.mRowId == mEnabledRowId) {
                return keyRow;
            }
        }
        return null;
    }

    public class KeyRow {
        public static final int ALWAYS_SHOW_ROW_ID = -1;
        public static final int DEFAULT_ROW_ID = 0;
        public List<SoftKey> mSoftKeys;
        public int mRowId;
        public float mTopF;
        public float mBottomF;
        public int mTop;
        public int mBottom;
    }
}
