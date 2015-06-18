package com.dodola.flip;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

/**
 * Created by dodola on 15/6/18.
 */
public class FlipApplcation extends Application {
    private static FlipApplcation flipApplcation;

    public static FlipApplcation getInstance() {
        return flipApplcation;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        flipApplcation = this;
        Fresco.initialize(this);
    }
}
