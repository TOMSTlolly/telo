package com.tomst.lolly.core;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class TDendroInfo
{
    public String serial;
    // include picture later
    public Long longitude, latitude;
    // an array of measurements (lines of a file)
    public Integer color;
    public ArrayList<TMereni> mers;
    public ArrayList<Entry> vT1;
    public ArrayList<Entry> vT2;
    public ArrayList<Entry> vT3;
    public ArrayList<Entry> vHA;


    public TDendroInfo(String serial, Long longitude, Long latitude)
    {
        this.serial = serial;
        this.longitude = longitude;
        this.latitude = latitude;
        this.color = color;
        this.mers = new ArrayList<>();
        this.vT1 = new ArrayList<>();
        this.vT2 = new ArrayList<>();
        this.vT3 = new ArrayList<>();
        this.vHA = new ArrayList<>();
    }
}
