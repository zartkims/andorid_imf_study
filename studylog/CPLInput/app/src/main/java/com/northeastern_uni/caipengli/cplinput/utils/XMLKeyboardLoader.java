package com.northeastern_uni.caipengli.cplinput.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.northeastern_uni.caipengli.cplinput.logic.SoftKeyboard;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKey;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyToggle;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyType;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyboardPool;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyboardTemplate;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by caipengli on 16年3月1日.
 * 显然就是加载xml的
 */
public class XMLKeyboardLoader {
    /***
     * 先定义出xml文件中的各个部分的tag标签
     */
    private static final String XML_TAG_SKB_TEMPLATE = "skb_template";
    private static final String XML_TAG_KEYTYPE = "key_type";
    private static final String XML_TAG_KEYICON = "key_icon";

    /**skb_template下的属性*/
    private static final String XML_ATTR_KEY_XMARGIN = "key_xmargin";
    private static final String XML_ATTR_KEY_YMARGIN = "key_ymargin";
    private static final String XML_ATTR_SKB_BG = "skb_bg";
    private static final String XML_ATTR_BALLOON_BG = "balloon_bg";
    private static final String XML_ATTR_POPUP_BG = "popup_bg";
    private static final String XML_ATTR_COLOR = "color";
    private static final String XML_ATTR_COLOR_HIGHLIGHT = "color_highlight";
    private static final String XML_ATTR_COLOR_BALLOON = "color_balloon";

    /**key_type下的属性*/
    private static final String XML_ATTR_ID = "id";
    private static final String XML_ATTR_KEYTYPE_BG = "bg";
    private static final String XML_ATTR_KEYTYPE_HLBG = "hlbg";

    /**key下的属性*/
    private static final String XML_ATTR_START_POS_X = "start_pos_x";
    private static final String XML_ATTR_START_POS_Y = "start_pos_y";

    /**row下的属性*/
    private static final String XML_ATTR_ROW_ID = "row_id";

    /**keyboard相关的各种tag*/
    private static final String XML_TAG_KEYBOARD = "keyboard";
    private static final String XML_TAG_ROW = "row";
    private static final String XML_TAG_KEYS = "keys";
    private static final String XML_TAG_KEY = "key";
    private static final String XML_TAG_TOGGLE_STATE = "toggle_state";
    /**上面的对应attr属性*/
    private static final String XML_ATTR_SKB_TEMPLATE = "skb_template";
    private static final String XML_ATTR_TOGGLE_STATE_ID = "state_id";
    private static final String XML_ATTR_SKB_CACHE_FLAG = "skb_cache_flag";
    private static final String XML_ATTR_SKB_STICKY_FLAG = "skb_sticky_flag";
    private static final String XML_ATTR_QWERTY = "qwerty";
    private static final String XML_ATTR_QWERTY_UPPERCASE = "qwerty_uppercase";
    private static final String XML_ATTR_KEY_TYPE = "key_type";
    private static final String XML_ATTR_KEY_WIDTH = "width";
    private static final String XML_ATTR_KEY_HEIGHT = "height";
    private static final String XML_ATTR_KEY_REPEAT = "repeat";
    private static final String XML_ATTR_KEY_BALLOON = "balloon";
    /**key array　*/
    private static final String XML_ATTR_KEY_SPLITTER = "splitter";
    private static final String XML_ATTR_KEY_LABELS = "labels";
    private static final String XML_ATTR_KEY_CODES = "codes";
    /**key */
    private static final String XML_ATTR_KEY_LABEL = "label";
    private static final String XML_ATTR_KEY_CODE = "code";
    private static final String XML_ATTR_KEY_ICON = "icon";
    private static final String XML_ATTR_KEY_ICON_POPUP = "icon_popup";
    /**soft keyboard. */
    private static final String XML_ATTR_KEY_POPUP_SKBID = "popup_skb";

    private static boolean DEFAULT_SKB_CACHE_FLAG = true;
    private static boolean DEFAULT_SKB_STICKY_FLAG = true;
    private static final int KEYTYPE_ID_LAST = -1;

    private Context mContext;
    private Resources mResources;
    private int mXmlEventType;
    private SoftKeyboardTemplate mSkbTemplate;

    float mKeyXPos;
    float mKeyYPos;
    int mSkbWidth;
    int mSkbHeight;
    float mKeyXMargin = 0;
    float mKeyYMargin = 0;
    boolean mNextEventFetched = false;
    String mAttrTmp;

    /**key一些公共的属性*/
    class KeyCommonAttributes {
        XmlResourceParser mXrp;
        int mKeyType;
        float mKeyWidth;
        float mKeyHeight;
        boolean mRepeat;
        boolean mBalloon;
        KeyCommonAttributes(XmlResourceParser xrp) {
            mXrp = xrp;
            mBalloon = true;
        }

        boolean getAttributes(KeyCommonAttributes defAttr) {
            if (null == defAttr) return false;
            mKeyType = getInteger(mXrp, XML_ATTR_KEY_TYPE, defAttr.mKeyType);
            mKeyWidth = getFloat(mXrp, XML_ATTR_KEY_WIDTH, defAttr.mKeyWidth);
            mKeyHeight = getFloat(mXrp, XML_ATTR_KEY_HEIGHT, defAttr.mKeyHeight);
            mRepeat = getBoolean(mXrp, XML_ATTR_KEY_REPEAT, defAttr.mRepeat);
            mBalloon = getBoolean(mXrp, XML_ATTR_KEY_BALLOON, defAttr.mBalloon);
            if (mKeyType < 0 || mKeyWidth <= 0 || mKeyHeight <= 0) {
                return false;
            }
            return true;
        }
    }

    public XMLKeyboardLoader(Context context) {
        mContext = context;
        mResources = mContext.getResources();
    }

    /***
     * 解析template不解释
     * @param templateId
     * @return
     */
    public SoftKeyboardTemplate loadSkbTemplate(int templateId) {
        if (null == mContext || 0 == templateId) {
            return null;
        }
        MYLOG.LOGD("now in load template xml");
        XmlResourceParser xrp = mResources.getXml(templateId);
        KeyCommonAttributes attrDef = new KeyCommonAttributes(xrp);
        KeyCommonAttributes attrKey = new KeyCommonAttributes(xrp);
        mSkbTemplate = new SoftKeyboardTemplate(templateId);
        int lastKeyTypeId = KEYTYPE_ID_LAST;
        int globalColor = 0;
        int globalColorHl = 0;
        int globalColorBalloon = 0;
        try {
            mXmlEventType = xrp.next();
            while (mXmlEventType != XmlResourceParser.END_DOCUMENT) {
                mNextEventFetched = false;
//                MYLOG.LOGI("template in while mXmlEventType : " + mXmlEventType
//                        + " mXmlEventType in while name : " + xrp.getName());
                if (mXmlEventType == XmlResourceParser.START_TAG) {
                    String tag = xrp.getName();
                    if (XML_TAG_SKB_TEMPLATE.equals(tag)) {
                        Drawable skbBg = getDrawable(xrp, XML_ATTR_SKB_BG, null);
                        Drawable balloonBg = getDrawable(xrp, XML_ATTR_BALLOON_BG, null);
                        Drawable popupBg = getDrawable(xrp, XML_ATTR_POPUP_BG, null);
                        if (null == skbBg || null == balloonBg || null == popupBg) {
                            return null;
                        }
                        mSkbTemplate.setBackgrounds(skbBg, balloonBg, popupBg);

                        float xMargin = getFloat(xrp, XML_ATTR_KEY_XMARGIN, 0);
                        float yMargin = getFloat(xrp, XML_ATTR_KEY_YMARGIN, 0);
                        mSkbTemplate.setMargins(xMargin, yMargin);

                        globalColor = getColor(xrp, XML_ATTR_COLOR, 0);
                        globalColorHl = getColor(xrp, XML_ATTR_COLOR_HIGHLIGHT, 0xffffffff);
                        globalColorBalloon = getColor(xrp, XML_ATTR_COLOR_BALLOON, 0xffffffff);
                    } else if (XML_TAG_KEYTYPE.equals(tag)) {
                        int id = getInteger(xrp, XML_ATTR_ID, KEYTYPE_ID_LAST);
                        Drawable bg = getDrawable(xrp, XML_ATTR_KEYTYPE_BG, null);
                        Drawable hlBg = getDrawable(xrp, XML_ATTR_KEYTYPE_HLBG,
                                null);
                        int color = getColor(xrp, XML_ATTR_COLOR, globalColor);
                        int colorHl = getColor(xrp, XML_ATTR_COLOR_HIGHLIGHT,
                                globalColorHl);
                        int colorBalloon = getColor(xrp, XML_ATTR_COLOR_BALLOON,
                                globalColorBalloon);
                        if (id != lastKeyTypeId + 1) {
                            return null;
                        }
                        SoftKeyType keyType = mSkbTemplate.createKeyType(id,
                                bg, hlBg);
                        keyType.setColors(color, colorHl, colorBalloon);
                        if (!mSkbTemplate.addKeyType(keyType)) {
                            return null;
                        }
                        lastKeyTypeId = id;
                    } else if (XML_TAG_KEYICON.equals(tag)) {
                        int keyCode = getInteger(xrp, XML_ATTR_KEY_CODE, 0);
                        Drawable icon = getDrawable(xrp, XML_ATTR_KEY_ICON, null);
                        Drawable iconPopup = getDrawable(xrp,
                                XML_ATTR_KEY_ICON_POPUP, null);
                        if (null != icon && null != iconPopup) {
                            mSkbTemplate.addDefaultKeyIcons(keyCode, icon,
                                    iconPopup);
                        }
                    } else if (XML_TAG_KEY.equals(tag)) {
                        int keyId = this.getInteger(xrp, XML_ATTR_ID, -1);
                        if (-1 == keyId) return null;
                        if (!attrKey.getAttributes(attrDef)) {
                            return null;
                        }
                        mKeyXPos = getFloat(xrp, XML_ATTR_START_POS_X, 0);
                        mKeyYPos = getFloat(xrp, XML_ATTR_START_POS_Y, 0);
                        SoftKey softKey = getSoftKey(xrp, attrKey);//再解析key
                        if (null == softKey) {
                            return null;
                        }
                        mSkbTemplate.addDefaultKey(keyId, softKey);
                    }
                }
                //因为有可能在softkey的时候已经取了下一个标签所以这里判断下
                if (!mNextEventFetched) mXmlEventType = xrp.next();
            }
            xrp.close();
//            MYLOG.LOGI("load mskbTemplate normal " + (mSkbTemplate == null));
            return mSkbTemplate;
        } catch (XmlPullParserException e) {
            MYLOG.LOGE("XmlPullParserException : " + e.toString());
        } catch (IOException e) {
            MYLOG.LOGE("IOException : " + e.toString());
        }
        return null;
    }

    /**
     * 解析 key 标签
     * @param xrp
     * @param attrKey
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private SoftKey getSoftKey(XmlResourceParser xrp, KeyCommonAttributes attrKey) throws IOException, XmlPullParserException {
        int keyCode = getInteger(xrp, XML_ATTR_KEY_CODE, 0);
        String label = getString(xrp, XML_ATTR_KEY_LABEL, null);
        /***
         * icon 和 popuoIcon 是 load keyboard的时候的
         */
        Drawable keyIcon = getDrawable(xrp, XML_ATTR_KEY_ICON, null); //比如逗号句号
        Drawable keyIconPopup = getDrawable(xrp, XML_ATTR_KEY_ICON_POPUP, null);
//        if (null != keyIcon || null != keyIconPopup) {
//            Log.i("cpl", "keyicon not null, its label : " + label);
//        }
        int popupSkbId = xrp.getAttributeResourceValue(null,
                XML_ATTR_KEY_POPUP_SKBID, 0);
        if (null == label && null == keyIcon) {
            keyIcon = mSkbTemplate.getDefaultKeyIcon(keyCode);
            keyIconPopup = mSkbTemplate.getDefaultKeyIconPopup(keyCode);
            if (null == keyIcon || null == keyIconPopup) return null;
        }

        float left, top, right, bottom;
        left = mKeyXPos;
        top = mKeyYPos;
        right = left + attrKey.mKeyWidth;
        bottom = top + attrKey.mKeyHeight;
        if (right - left < 2 * mKeyXMargin) return null;
        if (bottom - top < 2 * mKeyYMargin) return null;

        //接下来判断key是否有多种状态如果是的话就创建toggleKey
        boolean isToggleKey = false;
        mXmlEventType = xrp.next();
        mNextEventFetched = true;//已经取了下一个了
        if (mXmlEventType == XmlResourceParser.START_TAG) {
            String toggleTAG = xrp.getName();
            if (XML_TAG_TOGGLE_STATE.equals(toggleTAG)) {
                isToggleKey = true;
            }
        }
        SoftKey softKey;
        if (isToggleKey) {
            softKey = new SoftKeyToggle();
            SoftKeyToggle.ToggleState state = getToggleStates(attrKey, (SoftKeyToggle) softKey, keyCode);
            if (!((SoftKeyToggle)softKey).setToggleStates(state)) {
                return null;
            }
        } else {
            softKey = new SoftKey();
        }
        //normal stateF
        softKey.setKeyAttribute(keyCode, label, attrKey.mRepeat, attrKey.mBalloon);
        softKey.setPopupSkbId(popupSkbId);
        softKey.setKeyType(mSkbTemplate.getKeyType(attrKey.mKeyType), keyIcon, keyIconPopup);
//        MYLOG.LOGI("the soft key " + softKey.getKeyLabel() + " is : " + left + " " + " " + top
//                + " " + right + " " + bottom);
        softKey.setKeyDimensions(left, top, right, bottom);
        return softKey;
    }

    //xpr此时已经进入了toggle_state标签

    /**
     * 解析toggle_state标签 一个key可以有多个状态
     * @param attrKey
     * @param softKey
     * @param keyCodeDef
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private SoftKeyToggle.ToggleState getToggleStates(KeyCommonAttributes attrKey, SoftKeyToggle softKey, int keyCodeDef) throws IOException, XmlPullParserException {
        XmlResourceParser xrp = attrKey.mXrp;
        int statedId = getInteger(xrp, XML_ATTR_TOGGLE_STATE_ID, 0);
        if (statedId == 0) return null;
        String keyLabel = getString(xrp, XML_ATTR_KEY_LABEL, null);
        int keyCode;
        if (null == keyLabel) {
            keyCode = getInteger(xrp, XML_ATTR_KEY_CODE, keyCodeDef);
        } else {
            keyCode = getInteger(xrp, XML_ATTR_KEY_CODE, 0);
        }
        Drawable icon = getDrawable(xrp, XML_ATTR_KEY_ICON, null);
        Drawable iconPopup = getDrawable(xrp, XML_ATTR_KEY_ICON_POPUP, null);
        if (null == icon && null == keyLabel) { //既没有文字也没有图片就是什么也木有
            return null;
        }
        SoftKeyToggle.ToggleState rootState = softKey.createToggleState();
        rootState.setStateId(statedId);
        rootState.mKeyType = null;
        int keyTypeId = getInteger(xrp, XML_ATTR_KEY_TYPE, KEYTYPE_ID_LAST);
        if (KEYTYPE_ID_LAST != keyTypeId) {/**如果有变*/
            rootState.mKeyType = mSkbTemplate.getKeyType(keyTypeId);
        }
        rootState.mKeyCode = keyCode;
        rootState.mKeyIcon = icon;
        rootState.mKeyIconPopup = iconPopup;
        rootState.mKeyLabel = keyLabel;

        boolean repeat = getBoolean(xrp, XML_ATTR_KEY_REPEAT, attrKey.mRepeat);
        boolean balloon = getBoolean(xrp, XML_ATTR_KEY_BALLOON, attrKey.mBalloon);
        rootState.setStateFlags(repeat, balloon);

        rootState.mNextState = null;//循环看还有没下个state
        mXmlEventType = xrp.next();
        while (mXmlEventType != XmlResourceParser.START_TAG
                && mXmlEventType != XmlResourceParser.END_DOCUMENT) {
            mXmlEventType = xrp.next(); //直到下一个标签的开始
        }
        if (mXmlEventType == XmlResourceParser.START_TAG) {
            String tempTag = xrp.getName();
            if (XML_TAG_TOGGLE_STATE.equals(tempTag)) {
                //递归本函数
                SoftKeyToggle.ToggleState nextState = getToggleStates(attrKey,
                        softKey, keyCodeDef);
                if (null == nextState) return null;
                rootState.mNextState = nextState;
            }
        }
        return rootState;
    }


    public SoftKeyboard loadKeyboard(int xmlId, int skbWidth, int skbHeight) {
        MYLOG.LOGD("now in loadKeyboard");
        if (null == mContext) {
            return null;
        }
        SoftKeyboardPool skbPool = SoftKeyboardPool.getInstance();
        XmlResourceParser xrp = mContext.getResources().getXml(xmlId);
        mSkbTemplate = null;
        SoftKeyboard softKeyboard = null;
        Drawable skbBg;
        Drawable popupBg;
        Drawable balloonBg;
        SoftKey softKey = null;

        KeyCommonAttributes attrDef = new KeyCommonAttributes(xrp);//最公共的
        KeyCommonAttributes attrSkb = new KeyCommonAttributes(xrp);//软键盘级别公共
        KeyCommonAttributes attrRow = new KeyCommonAttributes(xrp);//行级别
        KeyCommonAttributes attrKeys = new KeyCommonAttributes(xrp);//keys级别
        KeyCommonAttributes attrKey = new KeyCommonAttributes(xrp);//单独key

        mKeyXPos = 0;
        mKeyYPos = 0;
        mSkbWidth = skbWidth;
        mSkbHeight = skbHeight;

        try {
            mKeyXMargin = 0;
            mKeyYMargin = 0;
            mXmlEventType = xrp.next();
            while (XmlPullParser.END_DOCUMENT != mXmlEventType) {
//                MYLOG.LOGI("in while mXmlEventType : " + mXmlEventType
//                        + "mXmlEventType in while name : " + xrp.getName());
                mNextEventFetched = false;
                if (XmlPullParser.START_TAG == mXmlEventType) {
                    String tag = xrp.getName();
                    if (XML_TAG_KEYBOARD.equals(tag)) {
                        //下面之所以不用getInt是因为获取的是个指向的@xml/skb_template1的在R里的id 而不是具体属性　
                        int templateId = xrp.getAttributeResourceValue(null, XML_ATTR_SKB_TEMPLATE, 0);
                        mSkbTemplate = skbPool.getTemplate(mContext, templateId);//如果没有现成的会调用上面的loadTemplate
                        if (null == mSkbTemplate || !attrSkb.getAttributes(attrDef)) {
                            return null;
                        }
                        softKeyboard = new SoftKeyboard(xmlId, mSkbTemplate, mSkbWidth, mSkbHeight);

                        boolean cacheFlag = getBoolean(xrp, XML_ATTR_SKB_CACHE_FLAG, DEFAULT_SKB_CACHE_FLAG);
                        boolean stickyFlag = getBoolean(xrp, XML_ATTR_SKB_STICKY_FLAG, DEFAULT_SKB_STICKY_FLAG);
                        boolean isQwerty = getBoolean(xrp, XML_ATTR_QWERTY, false);
                        boolean isQwertyUpperCase = getBoolean(xrp, XML_ATTR_QWERTY_UPPERCASE, false);
                        softKeyboard.setFlags(cacheFlag, stickyFlag, isQwerty, isQwertyUpperCase);
                        //margin
                        mKeyXMargin = getFloat(xrp, XML_ATTR_KEY_XMARGIN, mSkbTemplate.getXMargin());
                        mKeyYMargin = getFloat(xrp, XML_ATTR_KEY_YMARGIN, mSkbTemplate.getYMargin());
                        softKeyboard.setKeyMargins(mKeyXMargin, mKeyYMargin);
                        //bg 如果某个键盘有特殊定制的话就覆盖template
                        skbBg = getDrawable(xrp, XML_ATTR_SKB_BG, null);
                        popupBg = getDrawable(xrp, XML_ATTR_POPUP_BG, null);
                        balloonBg = getDrawable(xrp, XML_ATTR_BALLOON_BG, null);
                        if (null != skbBg) {
                            softKeyboard.setSkbBackground(skbBg);
                        }
                        if (null != popupBg) {
                            softKeyboard.setPopupBackground(popupBg);
                        }
                        if (null != balloonBg) {
                            softKeyboard.setKeyBalloonBackground(balloonBg);
                        }
                    } else if (XML_TAG_ROW.equals(tag)) {
                        if (!attrRow.getAttributes(attrSkb)) {
                            return null;
                        }
                        // Get the starting positions for the row.
                        mKeyXPos = getFloat(xrp, XML_ATTR_START_POS_X, 0);
                        mKeyYPos = getFloat(xrp, XML_ATTR_START_POS_Y, mKeyYPos);
                        int rowId = getInteger(xrp, XML_ATTR_ROW_ID, SoftKeyboard.KeyRow.ALWAYS_SHOW_ROW_ID);
                        softKeyboard.beginNewRow(rowId, mKeyYPos);
                    } else if (XML_TAG_KEYS.equals(tag)) {
                        if (null == softKeyboard) return null;
                        if (!attrKeys.getAttributes(attrRow)) {
                            return null;
                        }
                        String splitter = xrp.getAttributeValue(null, XML_ATTR_KEY_SPLITTER);
                        splitter = Pattern.quote(splitter);
                        String labels = xrp.getAttributeValue(null, XML_ATTR_KEY_LABELS);
                        String codes = xrp.getAttributeValue(null, XML_ATTR_KEY_CODES);
                        if (null == splitter || null == labels) {
                            return null;
                        }

                        String labelArr[] = labels.split(splitter);
                        String codeArr[] = null;
                        if (null != codes) {
                            codeArr = codes.split(splitter);
                            if (labelArr.length != codeArr.length) {
                                return null;
                            }
                        }
                        //开始一一对应把label和code
                        for (int i = 0; i < labelArr.length; i++) {
                            softKey = new SoftKey();
                            int keyCode = 0;
                            if (null != codeArr) {
                                keyCode = Integer.valueOf(codeArr[i]);
                            }
                            softKey.setKeyAttribute(keyCode, labelArr[i], attrKeys.mRepeat, attrKeys.mBalloon);
                            //部分公共属直接从template里面读

                            softKey.setKeyType(mSkbTemplate.getKeyType(attrKeys.mKeyType), null, null);
                            float left, right, top, bottom;
                            left = mKeyXPos;
                            top = mKeyYPos;
                            right = left + attrKeys.mKeyWidth;
                            bottom = top + attrKeys.mKeyHeight;
                            if (right - left < 2 * mKeyXMargin) return null;
                            if (bottom - top < 2 * mKeyYMargin) return null;
                            softKey.setKeyDimensions(left, top, right, bottom);
                            softKeyboard.addSoftKey(softKey);
                            mKeyXPos = right;//下一个键的左边就是这个键的右边
                            if ((int) mKeyXPos * mSkbWidth > mSkbWidth) {
                                return null;
                            }
                        }
                    } else if (XML_TAG_KEY.equals(tag)) {
                        if (null == softKeyboard) {
                            return null;
                        }
                        if (!attrKey.getAttributes(attrRow)) {
                            return null;
                        }
                        int keyId = this.getInteger(xrp, XML_ATTR_ID, -1);
                        if (keyId >= 0) {
                            softKey = mSkbTemplate.getDefaultKey(keyId);
                        } else {
                            softKey = getSoftKey(xrp, attrKey);
                        }
                        if (null == softKey) {
                            return null;
                        }
                        mKeyXPos = softKey.mRightF;
                        if ((int) mKeyXPos * mSkbWidth > mSkbWidth) {
                            return null;
                        }
                        //
                        if (mXmlEventType == XmlResourceParser.START_TAG) { //这种情况是经过getSoftKey
                            tag = xrp.getName();
                            if (XML_TAG_ROW.equals(tag)) {
                                mKeyYPos += attrRow.mKeyHeight;
                                if ((int) mKeyYPos * mSkbHeight > mSkbHeight) {
                                    return null;
                                }
                            }
                        }
                        //
                        softKeyboard.addSoftKey(softKey);
                    }
                } else if (XmlPullParser.END_TAG == mXmlEventType) {
                    String tag = xrp.getName();
                    if (XML_TAG_ROW.equals(tag)) {
                        mKeyYPos += attrRow.mKeyHeight;
                        if ((int) mKeyYPos * mSkbHeight > mSkbHeight) {
                            return null;
                        }
                    }
                }
                if (!mNextEventFetched) mXmlEventType = xrp.next();
            }
            xrp.close();
//            MYLOG.LOGD("xml load normal and go to set skb size " + mSkbWidth + "_" + mSkbHeight);
            softKeyboard.setSkbCoreSize(mSkbWidth, mSkbHeight);
            return softKeyboard;
        } catch (XmlPullParserException e) {
            MYLOG.LOGE("XmlPullParserException : " + e.toString());
        } catch (IOException e) {
            MYLOG.LOGE("IOException : " + e.toString());
        }

        return null;
    }


    /**可以抽取一下不过就这吧*/
    private int getInteger(XmlResourceParser xrp, String name, int defValue) {
        int resId = xrp.getAttributeResourceValue(null, name, 0);//先找Ｒ
        String str;
        if (0 == resId) {
            str = xrp.getAttributeValue(null, name);
            if (null == str) return defValue;
            try {
                return Integer.valueOf(str);
            } catch (Exception e) {
                return defValue;
            }
        } else {
            return Integer.valueOf(mContext.getResources().getString(resId));
        }
    }

    private int getColor(XmlResourceParser xrp, String name, int defValue) {
        int resId = xrp.getAttributeResourceValue(null, name, 0);
        String str = null;
        if (0 == resId) {
            str = xrp.getAttributeValue(null, name);
            if (null == str) return defValue;
            try {
                return Integer.valueOf(str);
            } catch (Exception e) {
                return defValue;
            }
        } else {
            return mContext.getResources().getColor(resId);
        }
    }

    private String getString(XmlResourceParser xrp, String name, String defValue) {
        int resId = xrp.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xrp.getAttributeValue(null, name);
        } else {
            return mContext.getResources().getString(resId);
        }
    }

    private float getFloat(XmlResourceParser xrp, String name, float defValue) {
        int resId = xrp.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            String s = xrp.getAttributeValue(null, name);
            if (null == s) return defValue;
            try {
                float ret;
                if (s.endsWith("%p")) {
                    ret = Float.parseFloat(s.substring(0, s.length() - 2)) / 100;
                } else {
                    ret = Float.parseFloat(s);
                }
                return ret;
            } catch (NumberFormatException e) {
                return defValue;
            }
        } else {
            return mContext.getResources().getDimension(resId);
        }
    }

    private boolean getBoolean(XmlResourceParser xrp, String name, boolean defValue) {
        String s = xrp.getAttributeValue(null, name);
        if (null == s) return defValue;
        try {
            return Boolean.parseBoolean(s);
        } catch (Exception e) {
            return defValue;
        }
    }

    private Drawable getDrawable(XmlResourceParser xrp, String name, Drawable defValue) {
        int resId = xrp.getAttributeResourceValue(null, name, 0);
        if (0 == resId) return defValue;
        return mResources.getDrawable(resId);
    }

}
