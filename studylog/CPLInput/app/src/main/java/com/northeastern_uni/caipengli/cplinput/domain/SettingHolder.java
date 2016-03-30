package com.northeastern_uni.caipengli.cplinput.domain;

import android.content.SharedPreferences;

/**
 * Created by caipengli on 16年2月29日.
 */
public class SettingHolder {

    private static final String PREDICTION_CONFIG = "prediction";
    private static final String VIBRATE_CONFIG = "vibrate";
    private static final String KEYSOUND_CONFIG = "sound";

    private static boolean sKeySound;
    private static boolean sVibrate;
    private static boolean sPrediction;
    private static int sRefCount = 0;
    private static SharedPreferences sSharedPreferences = null;

    private static SettingHolder sInstance = null;

    private SettingHolder(SharedPreferences pref) {
        sSharedPreferences = pref;
        initConfigs();
    }

    public static SettingHolder getInstance(SharedPreferences pref) {
        if (sInstance == null) {
            sInstance = new SettingHolder(pref);
        }
        sRefCount++;
        return sInstance;
    }
    private void initConfigs() {
        sKeySound = sSharedPreferences.getBoolean(KEYSOUND_CONFIG, true);
        sVibrate = sSharedPreferences.getBoolean(VIBRATE_CONFIG, false);
        sPrediction = sSharedPreferences.getBoolean(PREDICTION_CONFIG, true);
    }

    public void writeBack() {
        SharedPreferences.Editor editor = sSharedPreferences.edit();
        editor.putBoolean(VIBRATE_CONFIG, sVibrate);
        editor.putBoolean(KEYSOUND_CONFIG, sKeySound);
        editor.putBoolean(PREDICTION_CONFIG, sPrediction);
        editor.commit();
    }

    public static void releaseInstance() {
        sRefCount--;
        if (0 == sRefCount) {
            sInstance = null;
        }
    }

    public static boolean getKeySound() {
        return sKeySound;
    }

    public static void setKeySound(boolean keySound) {
        sKeySound = keySound;
    }

    public static boolean getVibrate() {
        return sVibrate;
    }

    public static void setVibrate(boolean vibrate) {
        sVibrate = vibrate;
    }

    public static boolean getPrediction() {
        return sPrediction;
    }

    public static void setPrediction(boolean prediction) {
        sPrediction = prediction;
    }
}
