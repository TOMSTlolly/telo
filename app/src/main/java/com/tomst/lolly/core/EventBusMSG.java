package com.tomst.lolly.core;

public class EventBusMSG {
        public static final short APP_RESUME                       =   1;  // Sent to components on app resume
        public static final short APP_PAUSE                        =   2;  // Sent to components on app pause
        public static final short NEW_TRACK                        =   3;  // Request to create a new track
        public static final short UPDATE_FIX                       =   4;  // Notify that a new fix is available
        public static final short UPDATE_TRACK                     =   5;  // Notify that the current track stats are updated
        public static final short UPDATE_TRACKLIST                 =   6;  // Notify that the tracklist is changed
        public static final short UPDATE_SETTINGS                  =   7;  // Tell that settings are changed
        public static final short REQUEST_ADD_PLACEMARK            =   8;  // The user ask to add a placemark
        public static final short ADD_PLACEMARK                    =   9;  // The placemark is available
        public static final short APPLY_SETTINGS                   =  10;  // The new settings must be applied
        static final short TOAST_TRACK_EXPORTED             =  11;  // The exporter has finished to export the track, shows toast
        static final short UPDATE_JOB_PROGRESS              =  13;  // Update the progress of the current Job
        static final short NOTIFY_TRACKS_DELETED            =  14;  // Notify that some tracks are deleted
        static final short UPDATE_ACTIONBAR                 =  15;  // Notify that the actionbar must be updated
        static final short REFRESH_TRACKLIST                =  16;  // Refresh the tracklist, without update it from DB
        static final short REFRESH_TRACKTYPE                =  17;  // Refresh the track type on the Edit Details dialog

        static final short TRACKLIST_DESELECT               =  24;  // The user deselect (into the tracklist) the track with a given id
        static final short TRACKLIST_SELECT                 =  25;  // The user select (into the tracklist) the track with a given id
        static final short INTENT_SEND                      =  26;  // Request to share
        static final short TOAST_UNABLE_TO_WRITE_THE_FILE   =  27;  // Exporter fails to export the Track (given id)

        static final short ACTION_BULK_DELETE_TRACKS        =  40;  // Delete the selected tracks
        static final short ACTION_BULK_EXPORT_TRACKS        =  41;  // Export the selected tracks
        static final short ACTION_BULK_VIEW_TRACKS          =  42;  // View the selected tracks
        static final short ACTION_BULK_SHARE_TRACKS         =  43;  // Share the selected tracks
        static final short TRACKLIST_RANGE_SELECTION        =  44;  // Select/Deselect a range of tracks
        static final short ACTION_EDIT_TRACK                =  45;  // Edit the selected track
}
