package com.tomst.lolly.core;

import android.location.Location;
import static com.tomst.lolly.LollyActivity.NOT_AVAILABLE;

public class LocationExtended {

    private Location location;
    private String description              = "";
    private double altitudeEGM96Correction  = NOT_AVAILABLE;
    private int numberOfSatellites          = NOT_AVAILABLE;
    private int numberOfSatellitesUsedInFix = NOT_AVAILABLE;



    public LocationExtended(Location location) {
        this.location = location;
        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            if (egm96.isLoaded()) altitudeEGM96Correction = egm96.getEGMCorrection(this.location.getLatitude(), this.location.getLongitude());
        }
    }


    // ------------------------------------------------------------------------- Getters and Setters

    public Location getLocation() {
        return location;
    }

    public double getLatitude() { return location.getLatitude(); }

    public double getLongitude() { return location.getLongitude(); }

    public double getAltitude() { return location.hasAltitude() ? location.getAltitude() : NOT_AVAILABLE; }

    public float getSpeed() { return location.hasSpeed() ? location.getSpeed() : NOT_AVAILABLE; }

    public float getAccuracy() { return location.hasAccuracy() ? location.getAccuracy() : NOT_AVAILABLE; }

    public float getBearing() { return location.hasBearing() ? location.getBearing() : NOT_AVAILABLE; }

    public long getTime() { return location.getTime(); }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNumberOfSatellites(int numberOfSatellites) {
        this.numberOfSatellites = numberOfSatellites;
    }

    public int getNumberOfSatellites() {
        return numberOfSatellites;
    }

    public void setNumberOfSatellitesUsedInFix(int numberOfSatellites) {
        numberOfSatellitesUsedInFix = numberOfSatellites;
    }

    public int getNumberOfSatellitesUsedInFix() {
        return numberOfSatellitesUsedInFix;
    }

    /**
     * @return the altitude correction, in meters, based on EGM96
     */


    public double getAltitudeEGM96Correction(){
        if (altitudeEGM96Correction == NOT_AVAILABLE) {
            //Log.w("myApp", "[#] LocationExtended.java - _AltitudeEGM96Correction == NOT_AVAILABLE");
            EGM96 egm96 = EGM96.getInstance();
            if (egm96 != null) {
                if (egm96.isLoaded()) altitudeEGM96Correction = egm96.getEGMCorrection(location.getLatitude(), location.getLongitude());
            }
        }
        return altitudeEGM96Correction;
    }


    /**
     * @return the orthometric altitude in meters
     */
    public double getAltitudeCorrected(double AltitudeManualCorrection, boolean EGMCorrection) {
        if (location != null) {
            if (!location.hasAltitude()) return NOT_AVAILABLE;
            if ((EGMCorrection) && (getAltitudeEGM96Correction() != NOT_AVAILABLE)) return location.getAltitude() - getAltitudeEGM96Correction() + AltitudeManualCorrection;
            else return location.getAltitude() + AltitudeManualCorrection;
        }
        return NOT_AVAILABLE;
    }

}
