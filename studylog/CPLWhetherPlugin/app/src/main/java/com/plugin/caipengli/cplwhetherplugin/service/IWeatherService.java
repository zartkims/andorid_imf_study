package com.plugin.caipengli.cplwhetherplugin.service;

import com.plugin.caipengli.cplwhetherplugin.bean.WeatherInfo;

/**
 * Created by caipengli on 16年4月1日.
 */
public interface IWeatherService {
    public void loadTheWeather(String city);
    public void setFinishListener(ILoadFinishListener listener);
    public interface ILoadFinishListener {
        public void loadFinish(WeatherInfo info);
    }
}
