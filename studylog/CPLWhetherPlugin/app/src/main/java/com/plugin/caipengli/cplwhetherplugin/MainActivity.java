package com.plugin.caipengli.cplwhetherplugin;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.plugin.caipengli.cplwhetherplugin.interactor.IInteractor;
import com.plugin.caipengli.cplwhetherplugin.interactor.InteracotrImpl;
import com.plugin.caipengli.cplwhetherplugin.service.WeahterService;

public class MainActivity extends Activity implements InvaildateUI {
    private IInteractor mInteractor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mInteractor = new InteracotrImpl();
        mInteractor.init(this, new WeahterService());
        mInteractor.loadTheWeather();
    }

    @Override
    public void invalidateUI() {
        Toast.makeText(this, "update the ui", Toast.LENGTH_SHORT).show();
    }

    @Override
    public Context getContext() {
        return this;
    }
}
