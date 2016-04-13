package com.plugin.caipengli.cplwhetherplugin.interactor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.plugin.caipengli.cplwhetherplugin.InvaildateUI;
import com.plugin.caipengli.cplwhetherplugin.Utils;
import com.plugin.caipengli.cplwhetherplugin.bean.WeatherInfo;
import com.plugin.caipengli.cplwhetherplugin.service.IWeatherService;
import com.plugin.caipengli.cplwhetherplugin.service.WeahterService;

/**
 * Created by caipengli on 16年4月1日.
 */
public class InteracotrImpl implements IInteractor {
    InvaildateUI mView;
    IWeatherService mService;
    WeahterService.ILoadFinishListener mListener = new LoadFinishListenerImpl();

    @Override
    public void init(InvaildateUI view, IWeatherService service) {
        this.mView = view;
        this.mService = service;
    }

    @Override
    public void loadTheWeather() {
        Context context = mView.getContext();
        //默认的包名和private mode
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String city = Utils.getCityName(sp);
        mService.setFinishListener(mListener);
        mService.loadTheWeather(city);
    }


    private class LoadFinishListenerImpl implements IWeatherService.ILoadFinishListener {
        @Override
        public void loadFinish(WeatherInfo info) {
            if (null != mView) {
                mView.invalidateUI();
            }
        }
    }

}
