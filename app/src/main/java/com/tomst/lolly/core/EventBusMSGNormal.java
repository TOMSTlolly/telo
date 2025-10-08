package com.tomst.lolly.core;


public class EventBusMSGNormal {
    public short eventBusMSG;
    long trackID;

    /**
     * Creates a new EventBusMSGNormal.
     *
     * @param eventBusMSG One of the EventBusMSG Values
     * @param trackID The ID of the Track
     */
    EventBusMSGNormal (short eventBusMSG, long trackID) {
        this.eventBusMSG = eventBusMSG;
        this.trackID = trackID;
    }
}