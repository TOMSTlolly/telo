package com.tomst.lolly.core;


import java.time.Instant;

public class TInfo {
    public int idx;
    public TDevState stat;
    public TMeteo meteo;

    public TDeviceType devType;
    public RFirmware fw;

    public double humAd;
    public double t1;
    public double t2;
    public double t3;

    public String msg;
    public Instant currDay;
    public long remainDays;

}
