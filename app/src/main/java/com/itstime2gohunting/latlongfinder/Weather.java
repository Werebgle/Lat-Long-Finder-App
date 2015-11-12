package com.itstime2gohunting.latlongfinder;

import android.util.Log;

public class Weather {
    private static final String TAG = Weather.class.getSimpleName();
    private String mSummary;
    private double mTemp;

    public String getSummary() {
        return mSummary;
    }

    public void setSummary(String summary) {
        mSummary = summary;
        Log.i(TAG, mSummary);
    }

    public double getTemp() {
        return mTemp;
    }

    public void setTemp(double temp) {
        mTemp = temp;
    }
}
