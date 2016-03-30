package com.northeastern_uni.caipengli.cplinput.domain;

import android.content.Context;

import com.northeastern_uni.caipengli.cplinput.logic.SoftKeyboard;
import com.northeastern_uni.caipengli.cplinput.utils.XMLKeyboardLoader;

import java.util.Vector;

/**
 * Created by caipengli on 16年3月1日.
 * 这个看名字就只做个优化缓存
 */
public class SoftKeyboardPool {
    private static SoftKeyboardPool sInstance = new SoftKeyboardPool();
    private Vector<SoftKeyboardTemplate> mSkbTemplates = new Vector<SoftKeyboardTemplate>();
    private Vector<SoftKeyboard> mKeyboards = new Vector<SoftKeyboard>();
    private SoftKeyboardPool() {}
    public static SoftKeyboardPool getInstance() {
        return sInstance;
    }

    public SoftKeyboardTemplate getTemplate(Context context, int templateId) {
        for (int i = 0; i < mSkbTemplates.size(); i++) {
            SoftKeyboardTemplate template = mSkbTemplates.get(i);
            if (template.getTemplateId() == templateId) {
                return template;
            }
        }
        //强行加载一波
        if (null != context) {
            XMLKeyboardLoader loader = new XMLKeyboardLoader(context);
            SoftKeyboardTemplate template = loader.loadSkbTemplate(templateId);
            if (null != template) {
                mSkbTemplates.add(template);
                return template;
            }
        }
        return null;
    }

    public SoftKeyboard getSoftKeyboard(Context context, int skbCacheId,
                int xmlId, int skbWidth, int skbHeight) {
        for (int i = 0; i < mKeyboards.size(); i++) {
            SoftKeyboard keyboard = mKeyboards.get(i);
            if (keyboard.getCacheId() == skbCacheId && keyboard.getSkbXmlId() == xmlId) {
                keyboard.setSkbCoreSize(skbWidth, skbHeight);
                keyboard.setNewlyLoadedFlag(false);
                return keyboard;
            }
        }
        if (null != context) {
            XMLKeyboardLoader loader = new XMLKeyboardLoader(context);
            SoftKeyboard softKeyboard = loader.loadKeyboard(xmlId, skbWidth, skbHeight);
            if (null != softKeyboard) {
                if (softKeyboard.getCacheFlag()) { //如果要缓存就缓存
                    softKeyboard.setCacheId(skbCacheId);
                    mKeyboards.add(softKeyboard);
                }
            }
            return  softKeyboard;
        }
        return null;
    }

    public void setSoftKeyboardCoreSize(int skbWidth, int skbHeight) {
        for (int i = 0; i < mKeyboards.size(); i++) {
            SoftKeyboard keyboard = mKeyboards.get(i);
            keyboard.setSkbCoreSize(skbWidth, skbHeight);
            keyboard.setNewlyLoadedFlag(false);
        }
    }

    public int getSkbCoreWidth() {
        return mKeyboards.get(0) == null ? -1 : mKeyboards.get(0).getSkbCoreWidth();
    }

    public int getSkbCoreHeight() {
        return mKeyboards.get(0) == null ? -1 : mKeyboards.get(0).getSkbCoreHeight();
    }
    public void clearCacheSkb() {
        mKeyboards.clear();
    }

    public void clearTemplate() {
        mSkbTemplates.clear();
    }

}
