package com.tomst.lolly.core;

public enum TPhysValue {
    vT1(1),vT2(2),vT3(3),vHum(4),vMvs(5),vAD(6),vMicro(7);

    private int value;
    private TPhysValue(int value) {
        this.value = value;
    }

    static TPhysValue fromValue(int value) {
        for (TPhysValue my : TPhysValue.values()) {
            if (my.value == value) {
                return my;
            }
        }
        return null;
    }

    public String valToString(TPhysValue val){
        String s = "";
        switch (val){
            case vT1: s = "Temp 1"; break;
            case vT2: s = "Temp 2"; break;
            case vT3: s = "Temp 3"; break;

            case vHum:
            case vAD:
            case vMicro:
                //if dendrometer csv
                s = "Growth";     //humidity
                //if soil mesurement csv
                //s = "Humidity; break;
                break;
        }
        return (s);
    }

    int value() {
        return value;
    }
}

