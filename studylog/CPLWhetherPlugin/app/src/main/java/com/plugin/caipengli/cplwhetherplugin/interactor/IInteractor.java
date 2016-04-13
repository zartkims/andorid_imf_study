package com.plugin.caipengli.cplwhetherplugin.interactor;

import com.plugin.caipengli.cplwhetherplugin.InvaildateUI;
import com.plugin.caipengli.cplwhetherplugin.service.IWeatherService;

/**
 * Created by caipengli on 16年4月1日.
 */
public interface IInteractor {
    public void init(InvaildateUI view, IWeatherService service);
    public void loadTheWeather();
}
