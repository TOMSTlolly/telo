package com.tomst.lolly.core;

public enum TDeviceType {
        dUnknown(0), dLolly3(1), dLolly4(2),dStehlik(3), dTermoChron(4), dAD(5), dAdMicro(6);
        private int value;
        private TDeviceType(int value) {
                this.value = value;
        }

        static TDeviceType fromValue(int value) {
                for (TDeviceType my : TDeviceType.values()) {
                        if (my.value == value) {
                                return my;
                        }
                }
                return null;
        }

        int value() {
                return value;
        }
}
