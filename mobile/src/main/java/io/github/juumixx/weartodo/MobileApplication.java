package io.github.juumixx.weartodo;

import android.app.Application;

import net.danlew.android.joda.JodaTimeAndroid;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EApplication;

@EApplication
public class MobileApplication extends Application {
    @AfterInject
    void init() {
        JodaTimeAndroid.init(this);
    }
}