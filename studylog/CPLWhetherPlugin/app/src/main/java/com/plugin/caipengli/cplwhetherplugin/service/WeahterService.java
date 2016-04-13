package com.plugin.caipengli.cplwhetherplugin.service;

import android.os.AsyncTask;
import android.util.Log;

import com.plugin.caipengli.cplwhetherplugin.Utils;
import com.plugin.caipengli.cplwhetherplugin.bean.WeatherInfo;
import com.plugin.caipengli.cplwhetherplugin.request.WeatherRequest;

/**
 * Created by caipengli on 16年4月1日.
 */
public class WeahterService implements IWeatherService {
    ILoadFinishListener mListener = null;
//    AsyncTa
    public void loadTheWeather(String city) {
        AsyncTask<String, String, String> mTask = new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                String city = params[0];
                String result = WeatherRequest.requestWeather(city);
                return result;
            }

            @Override
            protected void onPostExecute(String json) {
                Log.i("weather", json);
                if (null != mListener) {
                    mListener.loadFinish(parasJson2Obj(json));
                }
            }
        };
        if (null == city || "".equals(city.trim())) city = Utils.DEFAULT_CITY;
        mTask.execute(city);
    }

    @Override
    public void setFinishListener(ILoadFinishListener listener) {
        this.mListener = listener;
    }

    public WeatherInfo parasJson2Obj(String json) {
        WeatherInfo info = new WeatherInfo();
        return info;
    }


}
