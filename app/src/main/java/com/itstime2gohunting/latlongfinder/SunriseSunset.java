package com.itstime2gohunting.latlongfinder;

public class SunriseSunset {
    private String mSunrise;
    private String mSunset;
    private double mDayLength;

    public String getSunrise() {
        return mSunrise;
    }

    public void setSunrise(String sunrise) {
        mSunrise = sunrise;
    }

    public String getSunset() {
        return mSunset;
    }

    public void setSunset(String sunset) {
        mSunset = sunset;
    }

    public double getDayLength() {
        return mDayLength;
    }

    public void setDayLength(double dayLength) {
        mDayLength = dayLength;
    }
}
