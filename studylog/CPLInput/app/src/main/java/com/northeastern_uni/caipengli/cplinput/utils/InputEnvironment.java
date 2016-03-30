package com.northeastern_uni.caipengli.cplinput.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Created by caipengli on 16年2月23日.
 */
public class InputEnvironment {

    public static final int LEFT_PADDING = 5;
    public static final int RIGHT_PADDING = 5;
    public static final int TOP_PADDING = 5;
    public static final int BOTTOM_PADDING = 5;
    private static final int IGNORE_DP = 5;

    public static final int LOAD_SEQUENT_SIZE = 32;

    //竖屏时按键的高度比例
    private static final float KEY_HEIGHT_RATIO_PORTRAIT = 0.095f;
    //横屏时按键的高度比例
    private static final float KEY_HEIGHT_RATIO_LANDSCAPE = 0.127f;

    //候选词同理
    private static final float CANDIDATES_AREA_HEIGHT_RATIO_PORTRAIT = 0.084F;
    private static final float CANDIDATES_AREA_HEIGHT_RATIO_LANDSCAPE = 0.125F;

    /**popup稍微比候选词宽一点点 看起来好些*/
    private static final float KEY_BALLOON_HEIGHT_PLUS_RATIO = 0.07F;
    private static final float KEY_BALLOON_WIDTH_PLUS_RATIO = 0.06F;

    private static final float NORMAL_KEY_TEXT_SIZE_RATIO = 0.075f;
    private static final float FUNCTION_KEY_TEXT_SIZE_RATIO = 0.055f;

    private static final float NORMAL_BALLOON_TEXT_SIZE_RATIO = 0.14f;
    private static final float FUNCTION_BALLOON_TEXT_SIZE_RATIO = 0.085f;

    private static final float FLOAT_MODE_MIN_X_RATIO = 0.4f;
    private static final float FLOAT_MODE_MIN_Y_RATIO = 0.45f;
    private static final float FLOAT_MODE_DEFUALT_X_RATIO = 0.9f;
    private static final float FLOAT_MODE_DEFUALT_Y_RATIO = 0.8f;

    /**
     * 如果是悬浮模式稍微小一点看起来有点区别
     */
    private float mFloatModeXRatio = FLOAT_MODE_DEFUALT_X_RATIO;
    private float mFloatModeYRatio = FLOAT_MODE_DEFUALT_Y_RATIO;

    private static InputEnvironment sInstance = new InputEnvironment();


    private int mScreenWidth;
    private int mScreenHeight;
    private int mKeyHeight;
    private int mCandidatesAreaHeight;
    private int mKeyBalloonWidthPlus;
    private int mKeyBalloonHeightPlus;
    private int mNormalKeyTextSize;
    private int mFunctionKeyTextSize;
    private int mNormalBalloonTextSize;
    private int mFunctionBalloonTextSize;
    private boolean mIsFloat = false;
    private DisplayMetrics mDisplayMetrics;

    private Configuration mConfig = new Configuration();

    //=====下面是浮动窗口属性
    private static final String FLOAT_LAN_LOC_X = "floatLanLocX";
    private static final String FLOAT_LAN_LOC_Y = "floatLanLocY";
    private static final String FLOAT_PRO_LOC_X = "floatProLocX";
    private static final String FLOAT_PRO_LOC_Y = "floatProLocY";
    private static final String FLOAT_MODEL = "isFloat";
    private static final String FLOAT_MODE_X_RATIO = "floatModeXRatio";
    private static final String FLOAT_MODE_Y_RATIO = "floatModeYRatio";
//    private static final String FLOAT_MODEL = "isFloat";

    private int mFloatLanLocationX = Integer.MIN_VALUE;
    private int mFloatLanLocationY = Integer.MIN_VALUE;
    private int mFloatPortLocationX = Integer.MIN_VALUE;
    private int mFloatPortLocationY = Integer.MIN_VALUE;

    private InputEnvironment() {}
    public static InputEnvironment getInstance() {return sInstance;}

    private static float sDensity = 0;

    /**
     * 看看原来的屏幕有没有变(横竖屏)
     * @param newConfig
     * @param context
     */
    public void onConfigurationChanged(Configuration newConfig, Context context) {
        readFloatModeInfoFromSharePre(context);
        if (mConfig.orientation != newConfig.orientation) {
            resetWindowSize(context, newConfig);
        }
        mConfig.updateFrom(newConfig);
    }

    public Configuration getConfiguration() {
        return mConfig;
    }


    public int getSkbWidth() {
        return mScreenWidth;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public int getHeightForCandidates() {
        return mCandidatesAreaHeight;
    }

    public float getKeyXMarginFactor() {
        return 1.0f;
    }

    public float getKeyYMarginFactor() {
        if (Configuration.ORIENTATION_LANDSCAPE == mConfig.orientation) {
            return 0.7f;
        }
        return 1.0f;
    }

    public static float getDensity(Context context) {
        if (sDensity == 0) {
            sDensity = context.getResources().getDisplayMetrics().density;
        }
        return sDensity;
    }

    public int getKeyHeight() {
        return mKeyHeight;
    }

    public float getFloatModeXRatio() {
        return mFloatModeXRatio;
    }

    public float getFloatModeYRatio() {
        return mFloatModeYRatio;
    }

    public void setFloatModeXRatio(float XRatio, Context context) {
        if (XRatio <= FLOAT_MODE_MIN_X_RATIO) {
            mFloatModeXRatio = FLOAT_MODE_MIN_X_RATIO;
        } else if (XRatio >= 1) {
            mFloatModeXRatio = 1;
        } else {
            mFloatModeXRatio = XRatio;
        }
        saveFloatRatio(context);
    }

    public void setFloatModeYRatio(float YRatio, Context context) {
        if (YRatio <= FLOAT_MODE_MIN_Y_RATIO){
            mFloatModeYRatio = FLOAT_MODE_MIN_Y_RATIO;
        } else if (YRatio >= 1.5) { //原来的1.5倍为最大
            mFloatModeYRatio = 1.5f;
        } else {
            mFloatModeYRatio = YRatio;
        }
        logd("mFloatModeYRatio " + mFloatModeYRatio);
        saveFloatRatio(context);
    }

    public float getMinFloatModeWidth(Context context) {
        return getDisplayMetrics(context).widthPixels * FLOAT_MODE_MIN_X_RATIO;
    }

    public float getMinFloatModeHeight(Context context) {
        //键盘 + candidate
        return (getDisplayMetrics(context).heightPixels * KEY_HEIGHT_RATIO_PORTRAIT * 4
                + getDisplayMetrics(context).heightPixels * CANDIDATES_AREA_HEIGHT_RATIO_PORTRAIT)
                * FLOAT_MODE_MIN_Y_RATIO;
    }

    public float getMaxFloatModeWidth(Context context) {
        return getDisplayMetrics(context).widthPixels;
    }

    public float getMaxFloatModeHeight(Context context) {
        return ((getDisplayMetrics(context).heightPixels * KEY_HEIGHT_RATIO_PORTRAIT * 4
                + getDisplayMetrics(context).heightPixels * CANDIDATES_AREA_HEIGHT_RATIO_PORTRAIT))
                * 1.5f;//原来的1.5倍为最大
    }

    public int getKeyBalloonWidthPlus() {
        return mKeyBalloonWidthPlus;
    }

    public int getKeyBalloonHeightPlus() {
        return mKeyBalloonHeightPlus;
    }

    public int getSkbHeight() {
        if (Configuration.ORIENTATION_PORTRAIT == mConfig.orientation) {
            return mKeyHeight * 4;
        } else if (Configuration.ORIENTATION_LANDSCAPE == mConfig.orientation) {
            return mKeyHeight * 4;
        }
        return 0;
    }

    public int getKeyTextSize(boolean isFunctionKey) {
        if (isFunctionKey) {
            return mFunctionKeyTextSize;
        } else {
            return mNormalKeyTextSize;
        }
    }
    public int getBalloonTextSize(boolean isFunctionKey) {
        if (isFunctionKey) {
            return mFunctionBalloonTextSize;
        } else {
            return mNormalBalloonTextSize;
        }
    }

    public float getIngnorMoveDis(Context context) {
        return IGNORE_DP * getDensity(context);
    }

    public boolean hasHardKeyboard() {
        if (mConfig.keyboard == Configuration.KEYBOARD_NOKEYS
                || mConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            return false;
        }
        return true;
    }

    public boolean needDebug() {
        return true;
    }


    public boolean isFloatMode() {
        return mIsFloat;
    }

    public void setFloatMode(boolean isFloat) {
        this.mIsFloat = isFloat;
    }

    public DisplayMetrics getDisplayMetrics(Context context) {
        if (null == context) return mDisplayMetrics;
        if(mDisplayMetrics == null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            mDisplayMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(mDisplayMetrics);
        }
        return mDisplayMetrics;
    }

    public void resetWindowSize(Context context, Configuration config) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(mDisplayMetrics);
        mScreenWidth = !mIsFloat ? mDisplayMetrics.widthPixels
                : (int) (mDisplayMetrics.widthPixels * mFloatModeXRatio);
        mScreenHeight = !mIsFloat ? mDisplayMetrics.heightPixels
                : (int) (mDisplayMetrics.heightPixels * mFloatModeYRatio);

        int scale;
        if (Configuration.ORIENTATION_PORTRAIT   == config.orientation) {//竖屏
            mKeyHeight = (int) (mScreenHeight * KEY_HEIGHT_RATIO_PORTRAIT);
            mCandidatesAreaHeight = (int) (mScreenHeight * CANDIDATES_AREA_HEIGHT_RATIO_PORTRAIT);
            scale = mScreenWidth;
        } else {
            mKeyHeight = (int) (mScreenHeight * KEY_HEIGHT_RATIO_LANDSCAPE);
            mCandidatesAreaHeight = (int) (mScreenHeight * CANDIDATES_AREA_HEIGHT_RATIO_LANDSCAPE);
            scale = mScreenHeight;
        }
        //根据取得的较小一方设定其他
        mNormalKeyTextSize = (int) (scale * NORMAL_KEY_TEXT_SIZE_RATIO);
        mFunctionKeyTextSize = (int) (scale * FUNCTION_KEY_TEXT_SIZE_RATIO);
        mNormalBalloonTextSize = (int) (scale * NORMAL_BALLOON_TEXT_SIZE_RATIO);
        mFunctionBalloonTextSize = (int) (scale * FUNCTION_BALLOON_TEXT_SIZE_RATIO);
        mKeyBalloonWidthPlus = (int) (scale * KEY_BALLOON_WIDTH_PLUS_RATIO);
        mKeyBalloonHeightPlus = (int) (scale * KEY_BALLOON_HEIGHT_PLUS_RATIO);
    }

    public int getFloatingModeLocationX(Context context) {
        if (mFloatLanLocationX == Integer.MIN_VALUE || mFloatPortLocationX == Integer.MIN_VALUE) {
            readFloatLocationFromSharePre(context);
        }
        if (context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            return mFloatLanLocationX;
        } else {
            return mFloatPortLocationX;
        }
    }

    public int getFloatingModeLocationY(Context context) {
        if (mFloatLanLocationY == Integer.MIN_VALUE || mFloatPortLocationY == Integer.MIN_VALUE) {
            readFloatLocationFromSharePre(context);
        }
        if (context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            return mFloatLanLocationY;
        } else {
            return mFloatPortLocationY;
        }
    }

    /**
     * 这个方法保存的是浮动窗口的　leftMargin　和　topMargin
     * 只是保存在 内存 里如果要保存到硬盘伤请调用
     * 　{@link #saveFloatLocation}
     * @param locX
     * @param locY
     * @param context
     */
    public void setFloatingModeLocation(int locX, int locY, Context context) {
        if (context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            mFloatLanLocationX = locX;
            mFloatLanLocationY = locY;
        } else {
            mFloatPortLocationX = locX;
            mFloatPortLocationY = locY;
        }
//        saveFloatLocation(context);
    }

    /**
     * 浮动键盘大小
     * @param context
     */
    public void saveFloatRatio(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(FLOAT_MODE_X_RATIO, mFloatModeXRatio);
        editor.putFloat(FLOAT_MODE_Y_RATIO, mFloatModeYRatio);
        editor.apply();
    }

    /**
     * 保存是否是浮动状态
     * @param context
     */
    public void saveIsFloat2SharePre(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(FLOAT_MODEL, mIsFloat);
        editor.apply();
//        logd("save flaot " + mIsFloat);
    }

    /**
     * 保存浮动键盘出现的位置
     * @param context
     */
    public void saveFloatLocation(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(FLOAT_LAN_LOC_X, mFloatLanLocationX);
        editor.putInt(FLOAT_LAN_LOC_Y, mFloatLanLocationY);
        editor.putInt(FLOAT_PRO_LOC_X, mFloatPortLocationX);
        editor.putInt(FLOAT_PRO_LOC_Y, mFloatPortLocationY);
        editor.putBoolean(FLOAT_MODEL, mIsFloat);
        /**
         * apply的话是先写到内存之后再异步同步到硬盘上
         * 而commit是立刻同步到硬盘
         */
        editor.apply();
    }

    public void readFloatModeInfoFromSharePre(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        readFloatLocationFromSharePre(context);
        mIsFloat = sp.getBoolean(FLOAT_MODEL, false);
        mFloatModeXRatio = sp.getFloat(FLOAT_MODE_X_RATIO, FLOAT_MODE_DEFUALT_X_RATIO);
        mFloatModeYRatio = sp.getFloat(FLOAT_MODE_Y_RATIO, FLOAT_MODE_DEFUALT_Y_RATIO);
//        logd("read float from sharePre : " + mIsFloat);
    }

    public void readFloatLocationFromSharePre(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);


        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (Configuration.ORIENTATION_PORTRAIT == mConfig.orientation) {//竖屏
            mFloatPortLocationX = sp.getInt(FLOAT_PRO_LOC_X, (displayMetrics.widthPixels - mScreenWidth) / 2);
            mFloatPortLocationY = sp.getInt(FLOAT_PRO_LOC_Y, (displayMetrics.heightPixels - mScreenHeight) / 2);
            mFloatLanLocationX = sp.getInt(FLOAT_LAN_LOC_X, (displayMetrics.heightPixels - mScreenHeight) / 2); //反而将横当ｙ
            mFloatLanLocationY = sp.getInt(FLOAT_LAN_LOC_Y, (displayMetrics.widthPixels - mScreenWidth) / 2);
        } else {
            mFloatLanLocationX = sp.getInt(FLOAT_LAN_LOC_X, (displayMetrics.widthPixels - mScreenWidth) / 2);
            mFloatLanLocationY = sp.getInt(FLOAT_LAN_LOC_Y, (displayMetrics.heightPixels - mScreenHeight) / 2);
            mFloatPortLocationX = sp.getInt(FLOAT_PRO_LOC_X, (displayMetrics.heightPixels - mScreenHeight) / 2);
            mFloatPortLocationY = sp.getInt(FLOAT_PRO_LOC_Y, (displayMetrics.widthPixels - mScreenWidth) / 2);
        }

//        logd(displayMetrics.widthPixels + "_" + mScreenWidth + displayMetrics.heightPixels + "_" + mScreenHeight);
//        logd(mFloatPortLocationX + "_" + mFloatLanLocationY + "_" + mFloatLanLocationX + "_" + mFloatLanLocationY + "__");
    }

    private void logd(String s) {
        MYLOG.LOGD(s);
    }
}
