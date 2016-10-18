package com.yelinaung.melosearch;

import android.app.Application;
import timber.log.Timber;

/**
 * Created by yelinaung on 17/10/16.
 */

public class MeloApp extends Application {
  @Override public void onCreate() {
    super.onCreate();
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }
  }
}
