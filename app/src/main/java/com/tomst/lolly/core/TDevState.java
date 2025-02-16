package com.tomst.lolly.core;

// signalizace z threadu do hlavni formy
public enum TDevState
{
    tCapacity,
    tCheckTMSFirmware,
    tCompareTime,
    tError,
    tFinal,
    tFinishedData,
    tFirmware,
    tGetTime,
    tGetTimeError,
    tHead,
    tInfo,
    tInit,
    tLollyService,
    tMeasure,
    tNoHardware,
    tTMDCycling,
    tProgress,
    tReadData,
    tReadType,
    tReadFromBookmark,
    tReadMeteo,
    tRemainDays,
    tSerial,
    tSerialDuplicity,
    tSmallCommand,
    tSetMeteo,
    tSetTime,
    tStart,
    tWaitForAdapter,
    tWaitForMeasure,
    tWaitInLimbo,

    tSimulateSerial
}