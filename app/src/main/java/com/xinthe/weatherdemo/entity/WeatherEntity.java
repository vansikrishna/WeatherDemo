package com.xinthe.weatherdemo.entity;

import java.io.Serializable;

/**
 * Created by BALBI on 6/29/2016.
 */
public class WeatherEntity implements Serializable{

    public String weatherTitle;
    public String weatherDescription;
    public String weatherIcon;
    public String mainTemp;
    public String minTemp;
    public String maxTemp;
    public long updateTime;
}
