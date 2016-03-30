package com.northeastern_uni.caipengli.cplinput.domain;

import android.graphics.drawable.Drawable;

import java.util.Vector;

/**
 * Created by caipengli on 16年2月23日.
 * 相当于做个缓存  各个键盘都可以共用的东西
 * 好吧我坦白就是个享元模式
 */
public class SoftKeyboardTemplate {
    private int mTemplateId;
    private Drawable mSkbBg;
    private Drawable mBalloonBg;
    private Drawable mPopupBg;
    private float mXMargin = 0;
    private float mYMargin = 0;
    private Vector<SoftKeyType> mKeyTypes = new Vector<SoftKeyType>();
    private Vector<KeyIconRecord> mKeyIconRecords = new Vector<KeyIconRecord>();
    private Vector<KeyRecord> mKeyRecords = new Vector<KeyRecord>();

    public SoftKeyboardTemplate(int templateId) {
        this.mTemplateId = templateId;
    }

    public void setBackgrounds(Drawable skbBg, Drawable balloonBg,
                               Drawable popupBg) {
        mSkbBg = skbBg;
        mBalloonBg = balloonBg;
        mPopupBg = popupBg;
    }

    public SoftKeyType createKeyType(int id, Drawable bg, Drawable highlightBg) {
        return new SoftKeyType(id, bg, highlightBg);
    }

    public boolean addKeyType(SoftKeyType keyType) {
        //需要有正确的id
        if (mKeyTypes.size() != keyType.mKeyTypeId) return false;
        mKeyTypes.add(keyType);
        return true;
    }

    public SoftKeyType getKeyType(int typeId) {
        if (typeId < 0 || typeId > mKeyTypes.size()) {
            return null;
        } else {
            return mKeyTypes.elementAt(typeId);
        }
    }

    public void addDefaultKeyIcons(int keyCode, Drawable icon, Drawable iconPopup) {
        if (null == icon || null == iconPopup) return;
        KeyIconRecord keyIconRecord = new KeyIconRecord();
        keyIconRecord.keyCode = keyCode;
        keyIconRecord.icon = icon;
        keyIconRecord.iconPopup = iconPopup;
        int size = mKeyIconRecords.size();
        int pos = 0;
        while (pos < size) {
            //寻找到合适的位置
            if (mKeyIconRecords.get(pos).keyCode >= keyCode) break;
            pos++;
        }
        mKeyIconRecords.add(pos, keyIconRecord);
    }

    public Drawable getDefaultKeyIcon(int keyCode) {
        int size = mKeyIconRecords.size();
        int pos = 0;
        while (pos < size) {
            KeyIconRecord keyIconRecord = mKeyIconRecords.elementAt(pos);
            if (keyIconRecord.keyCode < keyCode) {
                pos++;
                continue;
            }
            if (keyIconRecord.keyCode == keyCode) { //找到一样的了
                return keyIconRecord.icon;
            }
            return null;//大的更不可能
        }
        return null;
    }

    public Drawable getDefaultKeyIconPopup(int keyCode) {
        int size = mKeyIconRecords.size();
        int pos = 0;
        while (pos < size) {
            KeyIconRecord keyIconRecord = mKeyIconRecords.get(pos);
            if (keyIconRecord.keyCode < keyCode) {
                pos++;
                continue;
            }
            if (keyIconRecord.keyCode == keyCode) {
                return keyIconRecord.iconPopup;
            }
            return null;
        }
        return null;
    }

    public void addDefaultKey(int keyId, SoftKey softKey) {
        if (null == softKey) return;
        KeyRecord keyRecord = new KeyRecord();
        keyRecord.keyId = keyId;
        keyRecord.softKey = softKey;

        int size = mKeyRecords.size();
        int pos = 0;
        while (pos < size) {
            if (mKeyRecords.get(pos).keyId >= keyId) {
                break;
            }
            pos++;
        }
        mKeyRecords.add(pos, keyRecord);
    }

    public SoftKey getDefaultKey(int keyId) {
        int size = mKeyRecords.size();
        int pos = 0;
        while (pos < size) {
            KeyRecord keyRecord = mKeyRecords.get(pos);
            if (keyRecord.keyId < keyId) {
                pos++;
                continue;
            }
            if (keyRecord.keyId == keyId) {
                return keyRecord.softKey;
            }
            return null;
        }
        return null;
    }

    public Drawable getSkbBackground() {
        return mSkbBg;
    }

    public Drawable getBalloonBackground() {
        return mBalloonBg;
    }

    public Drawable getPopupBackground() {
        return mPopupBg;
    }

    public void setMargins(float xMargin, float yMargin) {
        mXMargin = xMargin;
        mYMargin = yMargin;
    }

    public int getTemplateId() {
        return mTemplateId;
    }
    public float getXMargin() {
        return mXMargin;
    }

    public float getYMargin() {
        return mYMargin;
    }
}

class KeyIconRecord {
    int keyCode;
    Drawable icon;
    Drawable iconPopup;
}

class KeyRecord {
    int keyId;
    SoftKey softKey;
}