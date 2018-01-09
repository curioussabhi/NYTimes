package com.abhishek.nytimes.app;

import android.app.Application;

import com.abhishek.nytimes.di.AppComponent;
import com.abhishek.nytimes.di.DaggerAppComponent;


public class NYTApplication extends Application {
    public static AppComponent component;
    public static AppComponent getComponent() {
        return component;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        component = DaggerAppComponent.create();
    }
}
