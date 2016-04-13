package com.plugin.caipengli.cplwhetherplugin;

import android.content.SharedPreferences;

/**
 * Created by caipengli on 16年4月1日.
 */
public class Utils {
    public static final String CITY_NAME = "cityName";
    public static final String DEFAULT_CITY = "北京";

    public static String getCityName(SharedPreferences sp) {
        return  sp.getString(CITY_NAME, DEFAULT_CITY);
    }

    public static void saveCity(SharedPreferences sp, String city) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(CITY_NAME, city);
        editor.apply();
    }
}
