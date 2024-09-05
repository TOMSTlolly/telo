package com.tomst.lolly.core;

import java.time.LocalDateTime;
import java.util.Date;

public class TMereni {
    public int idx;
    public String Serial;
    public int hh, mm, ss;    // hodiny, minuty
    public int year;          // rok
    public int month, day;    // rok/mesic/den
    public int gtm;           // posun gtm
    public int adc;
    public int hum;
    public double t1, t2, t3;
    public int Err;
    public int mvs;
    public TDeviceType dev;
    public LocalDateTime dtm;
    public String msg;
    public TMereni(){
        this.idx =-1;
    }

    // kopirovaci konstruktor
    public TMereni(TMereni mer){
        this.idx =mer.idx;
        this.Serial = mer.Serial;
        this.hh = mer.hh;
        this.mm = mer.mm;
        this.ss = mer.ss;
        this.year=mer.year;
        this.month = mer.month;
        this.day = mer.day;
        this.gtm = mer.gtm;
        this.adc = mer.adc;
        this.hum = mer.hum;
        this.t1 = mer.t1;
        this.t2 = mer.t2;
        this.t3 = mer.t3;
        this.Err = mer.Err;
        this.mvs = mer.mvs;
        this.dtm = mer.dtm;
        this.dev = mer.dev;
        this.msg = mer.msg;
    }
}
